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
            throw new RuntimeException("Failed to fetch scan bins", e);
        }
    }
    
    public Optional<ScanBin> findById(String id) {
        try {
            Map<String, Object> doc = firebaseService.getDocument(COLLECTION_NAME, id);
            if (doc == null) {
                return Optional.empty();
            }
            
            ScanBin scanBin = convertToScanBin(doc);
            scanBin.setId(id);
            
            return Optional.of(scanBin);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch scan bin with id: " + id, e);
        }
    }
    
    public ScanBin save(ScanBin scanBin) {
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
            throw new RuntimeException("Failed to save scan bin", e);
        }
    }
    
    public void deleteById(String id) {
        try {
            firebaseService.deleteDocument(COLLECTION_NAME, id);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to delete scan bin with id: " + id, e);
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
        
        // Extract location information
        scanBin.setLocationName((String) doc.get("locationName"));
        
        // Handle timestamp conversion
        Object timestampObj = doc.get("timestamp");
        if (timestampObj instanceof Long) {
            LocalDateTime timestamp = Instant.ofEpochMilli((Long) timestampObj)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            scanBin.setTimestamp(timestamp);
        }
        
        // Handle numeric type conversion
        Object pointsEarnedObj = doc.get("pointsEarned");
        if (pointsEarnedObj instanceof Long) {
            scanBin.setPointsEarned(((Long) pointsEarnedObj).intValue());
        } else if (pointsEarnedObj instanceof Integer) {
            scanBin.setPointsEarned((Integer) pointsEarnedObj);
        } else if (pointsEarnedObj instanceof Double) {
            scanBin.setPointsEarned(((Double) pointsEarnedObj).intValue());
        }
        
        return scanBin;
    }
}
