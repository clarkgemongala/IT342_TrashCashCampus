package com.TrashCashCampus.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class TrashSecurity {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Disable CSRF if not needed
            .authorizeRequests()
                .requestMatchers("/api/trash/register", "/api/trash/login").permitAll() // Allow access to register/login
                .anyRequest().authenticated(); // Secure other endpoints

        return http.build();
    }
}
