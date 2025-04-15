package com.TrashCashCampus.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.TrashCashCampus.dto.ApiResponse;
import com.TrashCashCampus.dto.CredentialRequest;
import com.TrashCashCampus.dto.LoginRequest;



@RequestMapping("/api")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        // Dummy password check (replace with real auth logic)
        if ("admin@cit.edu".equalsIgnoreCase(request.getEmail()) && "admin123".equals(request.getPassword())) {
            return ResponseEntity.ok(new ApiResponse("Login successful"));
        }

        return ResponseEntity.status(401).body(new ApiResponse("Invalid credentials"));
    }

    @PostMapping("/request-credentials")
    public ResponseEntity<ApiResponse> requestCredentials(@RequestBody CredentialRequest request) {
        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        // Logic to handle saving or notifying admin can be added here
        System.out.println("Credential request from: " + request.getEmail());

        return ResponseEntity.ok(new ApiResponse("Credential request submitted"));
    }

    private boolean isValidEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@cit.edu");
    }
}