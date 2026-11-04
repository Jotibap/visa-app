package com.visa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Uploads are intentionally not exposed as public static resources. Use the
 * authenticated file endpoint.
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {
}
