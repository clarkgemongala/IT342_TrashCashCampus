package com.TrashCashCampus.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class TrashSecurity {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    http
	        .csrf().disable() // Disable CSRF if not needed
	        .authorizeRequests()
	            .antMatchers("/**").permitAll() // Allow access to all endpoints
	            .anyRequest().authenticated() // Secure other endpoints
	        .and()
	        .formLogin().disable()
	        .httpBasic().disable();

	    return http.build();
	}
}
