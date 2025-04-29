package com.TrashCashCampus.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            profile.put("fullName", request.getName()); // Include fullName field
            profile.put("password", request.getPassword()); // Store password for server-side authentication
            profile.put("totalPoints", 0); // Initialize with 0 points
            profile.put("recentPoints", 0); // Initialize daily points to 0
            profile.put("lastPointsUpdate", System.currentTimeMillis());
            profile.put("isEmailVerified", false); // Use isEmailVerified instead of isVerified
            profile.put("userId", uid); // Include userId field to match test@cit.edu structure
            profile.put("createdAt", System.currentTimeMillis()); // Add creation timestamp
            
            // Use the UID as the document ID instead of letting Firestore generate a random ID
            firebaseService.createDocumentWithId("users", uid, profile);
            
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
            // Get user by email from Firebase Auth
            UserRecord user = firebaseService.getUserByEmail(email);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse("User not found"));
            }
            
            if (isVerification) {
                // Send actual verification email using Firebase
                String link = firebaseService.generateEmailVerificationLink(email);
                System.out.println("Generated verification link: " + link);
                
                // Send the verification email
                firebaseService.sendEmail(
                    email,
                    "TrashCash Campus - Verify Your Email",
                    "Please verify your email address by clicking this link: " + link
                );
                
                // Note: We no longer need to update isEmailVerified here since the user
                // will click the verification link which will update Firebase Auth's emailVerified flag
                
                String uid = user.getUid();
                System.out.println("Email verification link sent to user with UID: " + uid);
                
                // Ensure we have a properly structured user document
                try {
                    // Try to ensure user has a document with the user's UID as the document ID
                    Map<String, Object> userDoc = firebaseService.findUserByEmail(email);
                    
                    if (userDoc != null) {
                        String docId = (String) userDoc.get("docId");
                        System.out.println("Found user document with ID: " + docId);
                        
                        // If docId doesn't match UID, create a proper document
                        if (!docId.equals(uid)) {
                            try {
                                System.out.println("Document ID doesn't match UID, creating a proper document");
                                
                                // Add userId field if missing
                                if (!userDoc.containsKey("userId")) {
                                    userDoc.put("userId", uid);
                                }
                                
                                // Ensure other consistent fields
                                if (!userDoc.containsKey("fullName") && userDoc.containsKey("name")) {
                                    userDoc.put("fullName", userDoc.get("name"));
                                }
                                
                                // Remove the docId field as it's not a real field
                                userDoc.remove("docId");
                                
                                // Create a new document with the proper UID
                                firebaseService.createDocumentWithId("users", uid, userDoc);
                                System.out.println("Created proper user document with UID as document ID");
                            } catch (Exception migrationError) {
                                System.out.println("Failed to migrate user document: " + migrationError.getMessage());
                            }
                        }
                    } else {
                        System.out.println("No existing user document found, creating one");
                        
                        // Create a basic user document if none exists
                        Map<String, Object> newUserDoc = new HashMap<>();
                        newUserDoc.put("email", email);
                        newUserDoc.put("userId", uid);
                        newUserDoc.put("isEmailVerified", false);
                        newUserDoc.put("createdAt", System.currentTimeMillis());
                        
                        firebaseService.createDocumentWithId("users", uid, newUserDoc);
                    }
                } catch (Exception e) {
                    System.out.println("Error ensuring proper user document: " + e.getMessage());
                    // Continue - this is not fatal
                }
            } else {
                // This is a password reset request
                // Generate password reset link
                String link = firebaseService.generatePasswordResetLink(email);
                System.out.println("Generated password reset link: " + link);
                
                // Send the reset email
                firebaseService.sendEmail(
                    email,
                    "TrashCash Campus - Reset Your Password",
                    "Please reset your password by clicking this link: " + link
                );
            }
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", isVerification ? 
                "Verification email sent to " + email : "Password reset email sent to " + email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error in requestPasswordReset: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * Endpoint to check email verification status and update Firestore
     * This is mainly for testing but can also be used to synchronize status
     */
    @GetMapping("/check-verification/{email}")
    public ResponseEntity<?> checkVerification(@PathVariable String email) {
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(new ApiResponse("Only @cit.edu email addresses are allowed"));
        }
        
        try {
            boolean updated = firebaseService.checkAndUpdateVerificationStatus(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("status", updated ? "updated" : "no_change_needed");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse("Error checking verification: " + e.getMessage()));
        }
    }
    
    /**
     * Endpoint to handle verification action code
     * This can be used as a callback URL for verification emails
     */
    @GetMapping("/action")
    public ResponseEntity<?> handleAction(@RequestParam("mode") String mode, 
                                        @RequestParam("oobCode") String oobCode,
                                        @RequestParam(value = "continueUrl", required = false) String continueUrl) {
        try {
            if ("verifyEmail".equals(mode)) {
                String email = firebaseService.checkActionCode(oobCode);
                
                if (email != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("email", email);
                    response.put("verified", true);
                    
                    // If continueUrl is provided, the client should redirect there
                    if (continueUrl != null && !continueUrl.isEmpty()) {
                        response.put("continueUrl", continueUrl);
                    }
                    
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.badRequest().body(new ApiResponse("Invalid verification code"));
                }
            } else if ("resetPassword".equals(mode)) {
                // Could implement password reset handling here
                return ResponseEntity.ok(new ApiResponse("Password reset link is valid"));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("Unsupported action mode: " + mode));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse("Error handling action: " + e.getMessage()));
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@cit.edu");
    }
}