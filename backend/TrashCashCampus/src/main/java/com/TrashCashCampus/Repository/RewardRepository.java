package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.Reward;
import com.TrashCashCampus.Service.FirebaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
            throw new RuntimeException("Failed to fetch rewards", e);
        }
    }
    
    public Optional<Reward> findById(String id) {
        try {
            Map<String, Object> doc = firebaseService.getDocument(COLLECTION_NAME, id);
            if (doc == null) {
                return Optional.empty();
            }
            
            Reward reward = new Reward();
            reward.setId(id);
            reward.setName((String) doc.get("name"));
            
            // Handle potential type conversion issues
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
            throw new RuntimeException("Failed to fetch reward with id: " + id, e);
        }
    }
    
    public Reward save(Reward reward) {
        try {
            Map<String, Object> data = Map.of(
                "name", reward.getName(),
                "pointsCost", reward.getPointsCost()
            );
            
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
            throw new RuntimeException("Failed to save reward", e);
        }
    }
    
    public void saveAll(List<Reward> rewards) {
        for (Reward reward : rewards) {
            save(reward);
        }
    }
    
    public void deleteById(String id) {
        try {
            firebaseService.deleteDocument(COLLECTION_NAME, id);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to delete reward with id: " + id, e);
        }
    }
    
    public long count() {
        try {
            return firebaseService.getAllDocuments(COLLECTION_NAME).size();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to count rewards", e);
        }
    }
}
