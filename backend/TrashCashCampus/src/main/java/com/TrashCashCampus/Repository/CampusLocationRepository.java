package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.CampusLocation;
import com.TrashCashCampus.Service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.Map;

@Repository
public class CampusLocationRepository {

    private static final String COLLECTION_NAME = "campus_locations";
    
    private final FirebaseService firebaseService;
    
    @Autowired
    public CampusLocationRepository(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public List<CampusLocation> findAll() {
        List<CampusLocation> locations = new ArrayList<>();
        
        try {
            List<Map<String, Object>> documents = firebaseService.getAllDocuments(COLLECTION_NAME);
            
            for (Map<String, Object> document : documents) {
                CampusLocation location = new CampusLocation();
                location.setId((String) document.get("id"));
                location.setName((String) document.get("name"));
                location.setLatitude(((Number) document.get("latitude")).doubleValue());
                location.setLongitude(((Number) document.get("longitude")).doubleValue());
                location.setDescription((String) document.get("description"));
                location.setBinType((String) document.get("binType"));
                locations.add(location);
            }
        } catch (Exception e) {
            System.err.println("Error getting campus locations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return locations;
    }

    public CampusLocation save(CampusLocation location) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", location.getName());
            data.put("latitude", location.getLatitude());
            data.put("longitude", location.getLongitude());
            data.put("description", location.getDescription());
            data.put("binType", location.getBinType());
            
            if (location.getId() == null || location.getId().isEmpty()) {
                // Create new document
                String id = firebaseService.createDocument(COLLECTION_NAME, data);
                location.setId(id);
            } else {
                // Update existing document
                firebaseService.updateDocument(COLLECTION_NAME, location.getId(), data);
            }
            
            return location;
        } catch (Exception e) {
            System.err.println("Error saving campus location: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 