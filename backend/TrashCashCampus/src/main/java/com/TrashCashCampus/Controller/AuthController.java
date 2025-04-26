package com.TrashCashCampus.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.TrashCashCampus.Service.FirebaseService;
import com.TrashCashCampus.dto.ApiResponse;
import com.TrashCashCampus.dto.CredentialRequest;
import com.TrashCashCampus.dto.LoginRequest;
import com.TrashCashCampus.dto.LoginResponse;
import com.TrashCashCampus.dto.RegistrationRequest;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private FirebaseService firebaseService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        try {
            String uid = firebaseService.signIn(request.getEmail(), request.getPassword());
            UserRecord user = firebaseService.getUserById(uid);
            
            LoginResponse response = new LoginResponse();
            response.setUserId(uid);
            response.setEmail(user.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ApiResponse("Invalid credentials: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        try {
            String uid = firebaseService.createUser(request.getEmail(), request.getPassword());
            
            // Create user profile in Firestore
            Map<String, Object> profile = new HashMap<>();
            profile.put("email", request.getEmail());
            profile.put("name", request.getName());
            profile.put("password", request.getPassword()); // Store password for server-side authentication
            profile.put("totalPoints", 0); // Initialize with 0 points
            profile.put("recentPoints", 0); // Initialize daily points to 0
            profile.put("lastPointsUpdate", System.currentTimeMillis());
            firebaseService.createDocument("users", profile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", uid);
            response.put("message", "User registered successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        // Here you would implement password reset logic using Firebase
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset email sent");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String token) {
        // Here you would verify the Firebase token
        // This is a placeholder - implement actual token verification
        return ResponseEntity.ok(true);
    }

    @PostMapping("/update-password/{userId}")
    public ResponseEntity<?> updatePassword(@PathVariable String userId, @RequestBody CredentialRequest request) {
        try {
            // Get user from Firebase
            UserRecord userRecord = firebaseService.getUserById(userId);
            
            // Update password in Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("password", request.getPassword());
            
            firebaseService.updateDocument("users", userId, updates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password updated successfully for user: " + userRecord.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Password update failed: " + e.getMessage()));
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@cit.edu");
    }
}