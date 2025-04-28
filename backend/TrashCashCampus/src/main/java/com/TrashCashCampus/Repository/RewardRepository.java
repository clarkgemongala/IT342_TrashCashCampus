package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.Reward;
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
public class RewardRepository {
    
    private static final String COLLECTION_NAME = "rewards";
    
    @Autowired
    private FirebaseService firebaseService;
    
    public List<Reward> findAll() {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty rewards list");
            return new ArrayList<>();
        }
        
        try {
            List<Map<String, Object>> documents = firebaseService.getAllDocuments(COLLECTION_NAME);
            List<Reward> rewards = new ArrayList<>();
            
            for (Map<String, Object> doc : documents) {
                Reward reward = new Reward();
                reward.setId((String) doc.get("id"));
                reward.setName((String) doc.get("name"));
                
                // Handle potential type conversion issues with numeric types from Firestore
                Object pointsCostObj = doc.get("pointsCost");
                if (pointsCostObj instanceof Long) {
                    reward.setPointsCost(((Long) pointsCostObj).intValue());
                } else if (pointsCostObj instanceof Integer) {
                    reward.setPointsCost((Integer) pointsCostObj);
                } else if (pointsCostObj instanceof Double) {
                    reward.setPointsCost(((Double) pointsCostObj).intValue());
                }
                
                rewards.add(reward);
            }
            
            return rewards;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to fetch rewards: " + e.getMessage());
            return new ArrayList<>(); // Return empty list instead of throwing exception
        }
    }
    
    public Optional<Reward> findById(String id) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty result");
            return Optional.empty();
        }
        
        try {
            Map<String, Object> doc = firebaseService.getDocument(COLLECTION_NAME, id);
            if (doc == null) {
                return Optional.empty();
            }
            
            Reward reward = new Reward();
            reward.setId(id);
            reward.setName((String) doc.get("name"));
            
            // Handle potential type conversion issues with numeric types from Firestore
            Object pointsCostObj = doc.get("pointsCost");
            if (pointsCostObj instanceof Long) {
                reward.setPointsCost(((Long) pointsCostObj).intValue());
            } else if (pointsCostObj instanceof Integer) {
                reward.setPointsCost((Integer) pointsCostObj);
            } else if (pointsCostObj instanceof Double) {
                reward.setPointsCost(((Double) pointsCostObj).intValue());
            }
            
            return Optional.of(reward);
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to fetch reward with id: " + id + " - " + e.getMessage());
            return Optional.empty(); // Return empty Optional instead of throwing exception
        }
    }
    
    public Reward save(Reward reward) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning entity as-is without saving");
            return reward;
        }
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", reward.getName());
            data.put("pointsCost", reward.getPointsCost());
            
            if (reward.getId() == null || reward.getId().isEmpty()) {
                // Create new document
                String newId = firebaseService.createDocument(COLLECTION_NAME, data);
                reward.setId(newId);
            } else {
                // Update existing document
                firebaseService.updateDocument(COLLECTION_NAME, reward.getId(), data);
            }
            
            return reward;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to save reward: " + e.getMessage());
            return reward; // Return the reward object as-is instead of throwing exception
        }
    }
    
    public void saveAll(List<Reward> rewards) {
        for (Reward reward : rewards) {
            save(reward);
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
            System.err.println("Failed to delete reward with id: " + id + " - " + e.getMessage());
            // Don't throw exception
        }
    }
    
    public long count() {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning 0 for count");
            return 0;
        }
        
        try {
            return firebaseService.getAllDocuments(COLLECTION_NAME).size();
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to count rewards: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Seeds the database with some initial rewards if the collection is empty
     */
    public void seedDefaultRewards() {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, skipping reward seeding");
            return;
        }
        
        try {
            List<Reward> existingRewards = findAll();
            if (!existingRewards.isEmpty()) {
                System.out.println("Rewards already exist, skipping seeding");
                return;
            }
            
            // Create default rewards using proper constructors
            Reward[] defaultRewards = {
                createReward("CIT Recycling Tote Bag", 150),
                createReward("Eco-friendly Water Bottle", 200),
                createReward("CIT Sustainability T-Shirt", 250),
                createReward("Campus Canteen Discount (10%)", 100),
                createReward("Book Store Discount (15%)", 300)
            };
            
            for (Reward reward : defaultRewards) {
                save(reward);
            }
            
            System.out.println("Sample rewards seeded.");
        } catch (Exception e) {
            System.err.println("Failed to seed rewards: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to create a reward with name and points cost
     */
    private Reward createReward(String name, int pointsCost) {
        Reward reward = new Reward();
        reward.setName(name);
        reward.setPointsCost(pointsCost);
        return reward;
    }
}
