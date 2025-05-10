package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.ScanBin;
import com.TrashCashCampus.Service.FirebaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class ScanBinRepository {
    
    private static final String COLLECTION_NAME = "scanBins";
    
    @Autowired
    private FirebaseService firebaseService;
    
    public List<ScanBin> findAll() {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty scan bin list");
            return new ArrayList<>();
        }
        
        try {
            List<Map<String, Object>> documents = firebaseService.getAllDocuments(COLLECTION_NAME);
            List<ScanBin> scanBins = new ArrayList<>();
            
            for (Map<String, Object> doc : documents) {
                ScanBin scanBin = convertToScanBin(doc);
                scanBin.setId((String) doc.get("id"));
                scanBins.add(scanBin);
            }
            
            return scanBins;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to fetch scan bins: " + e.getMessage());
            return new ArrayList<>(); // Return empty list instead of throwing exception
        }
    }
    
    public Optional<ScanBin> findById(String id) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning empty result");
            return Optional.empty();
        }
        
        try {
            Map<String, Object> doc = firebaseService.getDocument(COLLECTION_NAME, id);
            if (doc == null) {
                return Optional.empty();
            }
            
            ScanBin scanBin = convertToScanBin(doc);
            scanBin.setId(id);
            
            return Optional.of(scanBin);
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to fetch scan bin with id: " + id + " - " + e.getMessage());
            return Optional.empty(); // Return empty Optional instead of throwing exception
        }
    }
    
    public ScanBin save(ScanBin scanBin) {
        if (!firebaseService.isFirebaseInitialized()) {
            System.out.println("Firebase is in degraded mode, returning entity as-is without saving");
            return scanBin;
        }
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("qrCode", scanBin.getQrCode());
            data.put("wasteType", scanBin.getWasteType());
            data.put("imageBase64", scanBin.getImageBase64());
            data.put("pointsEarned", scanBin.getPointsEarned());
            data.put("message", scanBin.getMessage());
            data.put("fact", scanBin.getFact());
            data.put("timestamp", Instant.now().toEpochMilli()); // Store as timestamp for Firestore
            data.put("locationName", scanBin.getLocationName()); // Save location name properly
            data.put("binLocation", scanBin.getLocationName()); // Add binLocation field for backward compatibility
            
            if (scanBin.getId() == null || scanBin.getId().isEmpty()) {
                // Create new document
                String newId = firebaseService.createDocument(COLLECTION_NAME, data);
                scanBin.setId(newId);
            } else {
                // Update existing document
                firebaseService.updateDocument(COLLECTION_NAME, scanBin.getId(), data);
            }
            
            return scanBin;
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Failed to save scan bin: " + e.getMessage());
            return scanBin; // Return the scanBin object as-is instead of throwing exception
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
            System.err.println("Failed to delete scan bin with id: " + id + " - " + e.getMessage());
            // Don't throw exception
        }
    }
    
    // Helper method to convert Firestore document to ScanBin entity
    private ScanBin convertToScanBin(Map<String, Object> doc) {
        ScanBin scanBin = new ScanBin();
        
        scanBin.setQrCode((String) doc.get("qrCode"));
        scanBin.setWasteType((String) doc.get("wasteType"));
        scanBin.setImageBase64((String) doc.get("imageBase64"));
        scanBin.setMessage((String) doc.get("message"));
        scanBin.setFact((String) doc.get("fact"));
        
        // Try to get locationName, falling back to binLocation for backward compatibility
        String locationName = (String) doc.get("locationName");
        if (locationName == null) {
            locationName = (String) doc.get("binLocation");
        }
        scanBin.setLocationName(locationName);
        
        // Handle potential type conversion issues with numeric types from Firestore
        Object pointsEarnedObj = doc.get("pointsEarned");
        if (pointsEarnedObj instanceof Long) {
            scanBin.setPointsEarned(((Long) pointsEarnedObj).intValue());
        } else if (pointsEarnedObj instanceof Integer) {
            scanBin.setPointsEarned((Integer) pointsEarnedObj);
        } else if (pointsEarnedObj instanceof Double) {
            scanBin.setPointsEarned(((Double) pointsEarnedObj).intValue());
        }
        
        // Convert timestamp to LocalDateTime
        Object timestampObj = doc.get("timestamp");
        if (timestampObj instanceof Long) {
            long timestamp = (Long) timestampObj;
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), 
                ZoneId.systemDefault()
            );
            scanBin.setTimestamp(dateTime);
        }
        
        return scanBin;
    }
}
