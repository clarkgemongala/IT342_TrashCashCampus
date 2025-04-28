package com.TrashCashCampus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Configure CORS for API endpoints
                registry.addMapping("/api/**")
                        .allowedOrigins(
                            "http://localhost:5173",            // Web frontend local
                            "http://10.0.2.2:8080",             // Android emulator accessing localhost
                            "https://it342-trashcashcampus.onrender.com", // Our deployed backend
                            "https://trashcashcampus-web.onrender.com"    // Web frontend on Render
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
} 