package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.TrashEntity;
import com.TrashCashCampus.Service.FirebaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class TrashRepository {
    
    private static final String COLLECTION_NAME = "users";
    
    @Autowired
    private FirebaseService firebaseService;
    
    public List<TrashEntity> findAll() {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty trash list");
            return new ArrayList<>();
        }
        
        try {
            List<Map<String, Object>> documents = firebaseService.getAllDocuments(COLLECTION_NAME);
            List<TrashEntity> users = new ArrayList<>();
            
            for (Map<String, Object> doc : documents) {
                TrashEntity user = new TrashEntity();
                user.setId((String) doc.get("id"));
                user.setUsername((String) doc.get("username"));
                user.setEmail((String) doc.get("email"));
                user.setPassword((String) doc.get("password"));
                users.add(user);
            }
            
            return users;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to fetch users: " + e.getMessage());
            return new ArrayList<>(); // Return empty list instead of throwing exception
        }
    }
    
    public Optional<TrashEntity> findById(String id) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty result");
            return Optional.empty();
        }
        
        try {
            Map<String, Object> doc = firebaseService.getDocument(COLLECTION_NAME, id);
            if (doc == null) {
                return Optional.empty();
            }
            
            TrashEntity user = new TrashEntity();
            user.setId(id);
            user.setUsername((String) doc.get("username"));
            user.setEmail((String) doc.get("email"));
            user.setPassword((String) doc.get("password"));
            
            return Optional.of(user);
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to fetch user with id: " + id + " - " + e.getMessage());
            return Optional.empty(); // Return empty Optional instead of throwing exception
        }
    }
    
    public Optional<TrashEntity> findByEmail(String email) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty result");
            return Optional.empty();
        }
        
        try {
            // Since Firestore doesn't have direct query by field in our FirebaseService,
            // we'll get all documents and filter in memory (not ideal for production)
            List<Map<String, Object>> documents = firebaseService.getAllDocuments(COLLECTION_NAME);
            
            for (Map<String, Object> doc : documents) {
                String docEmail = (String) doc.get("email");
                if (email.equals(docEmail)) {
                    TrashEntity user = new TrashEntity();
                    user.setId((String) doc.get("id"));
                    user.setUsername((String) doc.get("username"));
                    user.setEmail(docEmail);
                    user.setPassword((String) doc.get("password"));
                    return Optional.of(user);
                }
            }
            
            return Optional.empty();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to find user by email: " + email + " - " + e.getMessage());
            return Optional.empty(); // Return empty Optional instead of throwing exception
        }
    }
    
    public TrashEntity save(TrashEntity user) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning entity as-is without saving");
            return user;
        }
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("password", user.getPassword());
            
            if (user.getId() == null || user.getId().isEmpty()) {
                // Create new document
                String newId = firebaseService.createDocument(COLLECTION_NAME, data);
                user.setId(newId);
            } else {
                // Update existing document
                firebaseService.updateDocument(COLLECTION_NAME, user.getId(), data);
            }
            
            return user;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to save user: " + e.getMessage());
            return user; // Return the user object as-is instead of throwing exception
        }
    }
    
    public void deleteById(String id) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, skipping delete operation");
            return;
        }
        
        try {
            firebaseService.deleteDocument(COLLECTION_NAME, id);
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to delete user with id: " + id + " - " + e.getMessage());
            // Don't throw exception
        }
    }
}
