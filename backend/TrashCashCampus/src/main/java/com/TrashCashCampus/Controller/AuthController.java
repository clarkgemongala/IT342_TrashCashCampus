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
import org.springframework.web.bind.annotation.CrossOrigin;

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
@CrossOrigin(origins = {"http://localhost:5173", "http://10.0.2.2:8080", "http://10.0.2.2", "https://trashcashcampus-testenvironment--trashcash-campus.netlify.app", "https://trashcash-campus.netlify.app"})
public class AuthController {

    @Autowired
    private FirebaseService firebaseService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (!isValidEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        // Check if Firebase is initialized
        if (!firebaseService.isFirebaseInitialized()) {
            // For testing/development, allow a test login when Firebase is down
            if (request.getEmail().equals("test@cit.edu") && request.getPassword().equals("Test123!")) {
                LoginResponse response = new LoginResponse();
                response.setUserId("test-user-id");
                response.setEmail("test@cit.edu");
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.status(500).body(
                new ApiResponse("Firebase service is currently unavailable. Please try again later.")
            );
        }

        try {
            String uid = firebaseService.signIn(request.getEmail(), request.getPassword());
            
            // Check if UID is null (which would happen if sign in failed)
            if (uid == null) {
                return ResponseEntity.status(401).body(
                    new ApiResponse("Invalid credentials or user not found")
                );
            }
            
            UserRecord user = firebaseService.getUserById(uid);
            
            // Check if user record is null
            if (user == null) {
                return ResponseEntity.status(401).body(
                    new ApiResponse("User record not found after successful authentication")
                );
            }
            
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

        // Check if Firebase is initialized
        if (!firebaseService.isFirebaseInitialized()) {
            return ResponseEntity.status(500).body(
                new ApiResponse("Firebase service is currently unavailable. Please try again later.")
            );
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
            profile.put("isEmailVerified", false); // Use isEmailVerified instead of isVerified
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

        // Check if this is a verification request or password reset
        Boolean isVerification = request.containsKey("isVerification") ? 
                               Boolean.parseBoolean(request.get("isVerification")) : false;

        try {
            // Get user by email
            UserRecord user = firebaseService.getUserByEmail(email);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse("User not found"));
            }
            
            // If this is a verification request, update the isVerified field in Firestore
            if (isVerification) {
                // Find the user document in Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("isEmailVerified", true);
                
                // Update user in Firestore - we're using the UID as the document ID
                firebaseService.updateDocument("users", user.getUid(), updates);
            }
            
            // Here you would implement actual email sending logic
            // For now, we just return success
            Map<String, Object> response = new HashMap<>();
            response.put("message", isVerification ? 
                "Verification email sent" : "Password reset email sent");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse((isVerification ? "Verification" : "Password reset") + 
                " request failed: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String token) {
        // Here you would verify the Firebase token
        // This is a placeholder - implement actual token verification
        return ResponseEntity.ok(true);
    }

    @PostMapping("/update-password/{userId}")
    public ResponseEntity<?> updatePassword(@PathVariable String userId, @RequestBody CredentialRequest request) {
        // Check if Firebase is initialized
        if (!firebaseService.isFirebaseInitialized()) {
            return ResponseEntity.status(500).body(
                new ApiResponse("Firebase service is currently unavailable. Please try again later.")
            );
        }
        
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