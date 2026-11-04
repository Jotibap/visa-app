package com.visa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender sender;
    @Value("${app.mail.from:no-reply@visa-portal.local}") private String from;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;
    @Value("${app.mail.enabled:false}") private boolean enabled;
    public EmailService(JavaMailSender sender){this.sender=sender;}
    public boolean isEnabled(){return enabled;}
    public void sendVerification(String email,String name,String token){send(email,"Verify your Visa Portal email","Hello "+name+",\n\nVerify your email using this link:\n"+frontendUrl+"/?verifyToken="+token+"\n\nIf you did not create this account, ignore this message.");}
    public void sendPasswordReset(String email,String name,String token){send(email,"Reset your Visa Portal password","Hello "+name+",\n\nReset your password using this link:\n"+frontendUrl+"/?resetToken="+token+"\n\nThis link expires in 30 minutes. If you did not request this, ignore this message.");}
    private void send(String to,String subject,String body){if(!enabled)return;SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(to);message.setSubject(subject);message.setText(body);sender.send(message);}
}
