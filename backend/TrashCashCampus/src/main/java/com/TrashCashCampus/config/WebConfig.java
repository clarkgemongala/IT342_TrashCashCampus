package com.TrashCashCampus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map paths to serve the index.html
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/login").setViewName("index");
        registry.addViewController("/dashboard").setViewName("index");
        registry.addViewController("/bins").setViewName("index");
        registry.addViewController("/rewards").setViewName("index");
        registry.addViewController("/users").setViewName("index");
        registry.addViewController("/admin-management").setViewName("index");
    }
} 