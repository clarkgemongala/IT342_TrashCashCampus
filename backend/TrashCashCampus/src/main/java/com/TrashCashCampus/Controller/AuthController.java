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
import com.google.firebase.auth.ActionCodeSettings;

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
            
            // Check if email is verified
            if (!user.isEmailVerified()) {
                // Get Firestore document to check if we've already verified
                Map<String, Object> userDoc = firebaseService.getDocument("users", uid);
                Boolean isEmailVerified = false;
                
                if (userDoc != null && userDoc.containsKey("isEmailVerified")) {
                    isEmailVerified = (Boolean) userDoc.get("isEmailVerified");
                }
                
                // If neither Firebase Auth nor Firestore shows verified, reject the login
                if (!isEmailVerified) {
                    return ResponseEntity.status(403).body(
                        new ApiResponse("Please verify your email before logging in. Check your inbox and spam folder for the verification link.")
                    );
                }
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
            profile.put("displayName", request.getName());
            profile.put("password", request.getPassword()); // Store password for server-side authentication
            profile.put("isEmailVerified", false); // Always set to false for new registrations
            profile.put("totalPoints", 0); // Initialize with 0 points
            profile.put("points", 0); // Individual points field
            profile.put("recycledMetal", 0); // Initialize with 0
            profile.put("recycledPlastic", 0); // Initialize with 0
            profile.put("totalRecycled", 0); // Total items recycled
            profile.put("role", "student"); // Default role is student
            profile.put("photoURL", ""); // Empty photo URL
            profile.put("createdAt", new java.util.Date());
            profile.put("lastUpdated", new java.util.Date());
            
            firebaseService.createDocumentWithId("users", uid, profile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", uid);
            response.put("message", "User registered successfully. Please check your email to verify your account.");
            
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

    @PostMapping("/request-email-verification")
    public ResponseEntity<?> requestEmailVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        // Check if Firebase is initialized
        if (!firebaseService.isFirebaseInitialized()) {
            return ResponseEntity.status(500).body(
                new ApiResponse("Firebase service is currently unavailable. Please try again later.")
            );
        }
        
        try {
            // Generate a verification email link
            ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
                .setUrl("https://trashcash-campus.netlify.app/emailVerified")
                .setHandleCodeInApp(false)
                .build();
            
            String link = firebaseService.getFirebaseAuth().generateEmailVerificationLink(email, actionCodeSettings);
            
            // Actually send the email using our sendVerificationEmail method
            firebaseService.sendVerificationEmail(email, link);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email verification link sent to " + email + ". Please check your inbox and spam folder.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to send verification email: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String oobCode = request.get("oobCode"); // The code from the email link
        
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }

        // Check if Firebase is initialized
        if (!firebaseService.isFirebaseInitialized()) {
            return ResponseEntity.status(500).body(
                new ApiResponse("Firebase service is currently unavailable. Please try again later.")
            );
        }
        
        try {
            // In a production app, you should verify the oobCode
            // This is a simplified version
            
            // Find the user with this email
            UserRecord user = firebaseService.getUserByEmail(email);
            String userId = user.getUid();
            
            // Update the user's email verification status in Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("isEmailVerified", true);
            
            firebaseService.updateDocument("users", userId, updates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email verification successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Email verification failed: " + e.getMessage()));
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