package com.visa.service;
import com.fasterxml.jackson.databind.*;import com.visa.entity.VisaApplication;import org.springframework.beans.factory.annotation.Value;import org.springframework.http.MediaType;import org.springframework.stereotype.Service;import org.springframework.web.client.RestClient;import java.nio.file.*;import java.util.*;
@Service public class GeminiVerificationService {
 @Value("${app.ai.api-key}") private String apiKey;@Value("${app.ai.model}") private String model;@Value("${app.upload-dir}") private String uploadDir;
 private final RestClient http=RestClient.create();private final ObjectMapper mapper=new ObjectMapper();
 private JsonNode call(String prompt,Path image,double temperature,Integer maxOutputTokens)throws Exception{if(apiKey==null||apiKey.isBlank())throw new IllegalStateException("AI_SERVICE_API_KEY is not configured on the backend");String mime=Files.probeContentType(image);String data=Base64.getEncoder().encodeToString(Files.readAllBytes(image));Map<String,Object> inline=Map.of("mimeType",mime==null?"image/jpeg":mime,"data",data);Map<String,Object> generation=new LinkedHashMap<>();generation.put("temperature",temperature);generation.put("topP",0.95);if(maxOutputTokens!=null)generation.put("maxOutputTokens",maxOutputTokens);Map<String,Object> payload=Map.of("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt),Map.of("inlineData",inline)))),"generationConfig",generation);String url="https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+apiKey;String response=http.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(String.class);JsonNode root=mapper.readTree(response);return root.path("candidates").path(0).path("content").path("parts").path(0).path("text");}
 private Path file(Object path){if(path==null)return null;String s=path.toString();if(s.startsWith("/uploads/"))s=s.substring("/uploads/".length());else if(s.startsWith("/api/applications/")){String[] parts=s.split("/");if(parts.length<6)return null;s="application-"+parts[3]+"/"+parts[5];}Path root=Paths.get(uploadDir).toAbsolutePath().normalize();Path p=root.resolve(s).normalize();return p.startsWith(root)&&Files.exists(p)?p:null;}
 private Map<String,Object> detect(Path image,String label){if(image==null)return Map.of("detected",false,"error",label+" image not found");try{String result=call("Analyze the provided image. Does it clearly show a single, unobstructed, forward-facing human face suitable for identification? Respond with only YES or NO.",image,0.1,5).asText().trim();return Map.of("detected",result.equalsIgnoreCase("YES"),"error","");}catch(Exception e){return Map.of("detected",false,"error",String.valueOf(e.getMessage()));}}
 private Boolean combine(Boolean a,Boolean b){if(Boolean.FALSE.equals(a)||Boolean.FALSE.equals(b))return false;if(a==null||b==null)return null;return true;}
 private Boolean compare(Object extracted,Object entered){if(extracted==null||entered==null)return null;String x=extracted.toString().trim(),y=entered.toString().trim();if(x.isBlank()||y.isBlank()||x.equalsIgnoreCase("Not Visible"))return null;return x.equalsIgnoreCase(y);}
 public Map<String,Object> analyze(VisaApplication app)throws Exception{
  Path user=file(app.getPersonalDetails().get("photo_path"));Path passport=file(app.getPassportDetails().get("passport_image_path"));Map<String,Object> userFace=detect(user,"User Photo"),passportFace=detect(passport,"Passport Photo");Map<String,Object> images=new LinkedHashMap<>();images.put("face_detected_user_photo",userFace.get("detected"));images.put("user_photo_face_error",userFace.get("error"));images.put("face_detected_passport_photo",passportFace.get("detected"));images.put("passport_photo_face_error",passportFace.get("error"));images.put("image_quality_user_photo",Boolean.TRUE.equals(userFace.get("detected"))?"Acceptable (Face Detected)":"Poor or unavailable");images.put("image_quality_passport_photo",Boolean.TRUE.equals(passportFace.get("detected"))?"Acceptable (Face Detected)":"Poor or unavailable");images.put("liveness_check","N/A (Gemini API does not provide liveness in this workflow)");images.put("tampering_detected",null);images.put("photo_match_score",null);images.put("photo_match_note","The existing Gemini workflow checks face presence; it does not perform biometric face matching. No match score is fabricated.");
  Map<String,Object> ocr=new LinkedHashMap<>();if(passport==null){ocr.put("error","Passport image not found");}else{String prompt="""
Extract the following details from the provided passport image.
Format the output as a valid JSON object with the following keys:
- \"givenNames\": The given name(s) of the passport holder.
- \"surname\": The surname(s) of the passport holder.
- \"passportNumber\": The passport number.
- \"nationality\": The nationality code (e.g., IND).
- \"dateOfBirth\": The date of birth in YYYY-MM-DD format.
- \"gender\": The gender (e.g., M, F, X).
- \"dateOfIssue\": The date of passport issue in YYYY-MM-DD format.
- \"dateOfExpiry\": The date of passport expiry in YYYY-MM-DD format.
- \"issuingAuthority\": The authority that issued the passport.
If a field is not clearly visible or cannot be determined, use \"Not Visible\" as its value. Return only JSON.""";String raw=call(prompt,passport,0.2,null).asText();ocr.put("raw_text",raw);try{String clean=raw.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$","").trim();ocr.put("ocr_data",mapper.readValue(clean,Map.class));}catch(Exception e){ocr.put("error","Could not parse structured OCR response");}}
  Map<String,Object> consistency=new LinkedHashMap<>();Map<String,Object> extracted=ocr.get("ocr_data") instanceof Map<?,?>?(Map<String,Object>)ocr.get("ocr_data"):Map.of();
  consistency.put("nameMatch",combine(compare(extracted.get("givenNames"),app.getPersonalDetails().get("first_name")),compare(extracted.get("surname"),app.getPersonalDetails().get("last_name"))));
  consistency.put("dateOfBirthMatch",compare(extracted.get("dateOfBirth"),app.getPersonalDetails().get("date_of_birth")));
  consistency.put("passportNumberMatch",compare(extracted.get("passportNumber"),app.getPassportDetails().get("passport_number")));
  Map<String,Object> findings=new LinkedHashMap<>();findings.put("image_verification",images);findings.put("passport_ocr",ocr);findings.put("data_consistency",consistency);
  boolean face=Boolean.TRUE.equals(userFace.get("detected"));boolean pface=Boolean.TRUE.equals(passportFace.get("detected"));
  // Preserve the legacy weighted risk thresholds. The old photo match score was random/mock data;
  // it is deliberately not fabricated, so an unavailable score contributes zero.
  int totalScore=(face?30:-50)+(pface?20:0);
  String risk=totalScore>=50?"Low":totalScore>=25?"Medium":"High";
  String summary="AI Verification Summary:\n- User photo face detected: "+face+"\n- Passport photo face detected: "+pface+"\n- Passport OCR: "+(ocr.containsKey("ocr_data")?"Data extracted.":ocr.getOrDefault("error","Not available"))+"\n- Data consistency checks: "+consistency+"\n- Liveness: Not supported by the configured Gemini workflow.\n- Biometric face match: Not performed by the existing API workflow.\n- Photo match score: Not available (legacy score was a mock value).\nAI findings are advisory; an administrator must make the final decision.";
  Map<String,Object> result=new LinkedHashMap<>();result.put("findings",findings);result.put("riskScore",risk);result.put("summary",summary);return result;
 }
}
