package com.TrashCashCampus.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.TrashCashCampus.Service.FirebaseService;
import com.TrashCashCampus.dto.ApiResponse;
import com.TrashCashCampus.dto.EmailRequest;
import com.TrashCashCampus.dto.EmailResponse;
import com.TrashCashCampus.dto.PasswordUpdateRequest;
import com.TrashCashCampus.dto.ProfileRequest;
import com.TrashCashCampus.dto.ProfileResponse;
import com.TrashCashCampus.dto.ProfileUpdateRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://10.0.2.2:8080", "http://10.0.2.2", "https://trashcashcampus-testenvironment--trashcash-campus.netlify.app", "https://trashcash-campus.netlify.app"})
public class UserController {

    @Autowired
    private FirebaseService firebaseService;

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authToken) {
        try {
            Map<String, Object> userData = firebaseService.getDocument("users", userId);
            if (userData == null) {
                return ResponseEntity.notFound().build();
            }
            
            ProfileResponse response = new ProfileResponse();
            response.setUserId(userId);
            response.setName((String) userData.get("name"));
            response.setEmail((String) userData.get("email"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{userId}/profile/email")
    public ResponseEntity<EmailResponse> getUserEmail(
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authToken) {
        try {
            Map<String, Object> userData = firebaseService.getDocument("users", userId);
            if (userData == null) {
                return ResponseEntity.notFound().build();
            }
            
            EmailResponse response = new EmailResponse();
            response.setEmail((String) userData.get("email"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<ProfileResponse> updateUserProfile(
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authToken,
            @RequestBody ProfileRequest profileRequest) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", profileRequest.getName());
            
            firebaseService.updateDocument("users", userId, updates);
            
            Map<String, Object> userData = firebaseService.getDocument("users", userId);
            
            ProfileResponse response = new ProfileResponse();
            response.setUserId(userId);
            response.setName((String) userData.get("name"));
            response.setEmail((String) userData.get("email"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{userId}/profile/email")
    public ResponseEntity<EmailResponse> updateUserEmail(
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authToken,
            @RequestBody EmailRequest emailRequest) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("email", emailRequest.getEmail());
            
            firebaseService.updateDocument("users", userId, updates);
            
            EmailResponse response = new EmailResponse();
            response.setEmail(emailRequest.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @PathVariable("userId") String userId,
            @RequestHeader("Authorization") String authToken,
            @RequestBody PasswordUpdateRequest passwordUpdateRequest) {
        // Firebase password update logic would go here
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password updated successfully");
        
        return ResponseEntity.ok(response);
    }
} 