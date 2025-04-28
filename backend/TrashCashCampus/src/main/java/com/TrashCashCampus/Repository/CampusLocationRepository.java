package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.CampusLocation;
import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class CampusLocationRepository {

    private static final String COLLECTION_NAME = "campus_locations";

    public List<CampusLocation> findAll() {
        List<CampusLocation> locations = new ArrayList<>();
        
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
            
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot document : documents) {
                CampusLocation location = document.toObject(CampusLocation.class);
                location.setId(document.getId());
                locations.add(location);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error getting campus locations: " + e.getMessage());
        }
        
        return locations;
    }

    public CampusLocation save(CampusLocation location) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            
            if (location.getId() == null || location.getId().isEmpty()) {
                // Create new document
                ApiFuture<com.google.cloud.firestore.DocumentReference> future = 
                    firestore.collection(COLLECTION_NAME).add(location);
                String id = future.get().getId();
                location.setId(id);
            } else {
                // Update existing document
                firestore.collection(COLLECTION_NAME)
                    .document(location.getId())
                    .set(location);
            }
            
            return location;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error saving campus location: " + e.getMessage());
            return null;
        }
    }
} 