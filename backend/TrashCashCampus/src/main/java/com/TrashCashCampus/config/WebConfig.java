package com.TrashCashCampus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;

import java.util.Collections;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure how Spring MVC should match request paths to controllers
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        // Keep semicolon content (for matrix variables)
        urlPathHelper.setRemoveSemicolonContent(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    /**
     * Forward root requests to the health endpoints
     */
    @Bean
    public SimpleUrlHandlerMapping customUrlHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Integer.MAX_VALUE - 2);
        
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        
        mapping.setUrlMap(Collections.singletonMap("/", handler));
        mapping.setInterceptors(Collections.singletonList((request, response, handler1) -> {
            response.sendRedirect("/api/health");
            return false;
        }));
        
        return mapping;
    }
} 