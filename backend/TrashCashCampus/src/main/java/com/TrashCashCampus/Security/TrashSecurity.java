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
	            .requestMatchers("/**").permitAll() // Allow access to all endpoints
	            .anyRequest().authenticated(); // Secure other endpoints

	    return http.build();
	}
}
