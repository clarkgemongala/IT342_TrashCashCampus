package com.TrashCashCampus.Service;

import com.TrashCashCampus.Entity.CampusLocation;
import com.TrashCashCampus.Entity.ScanBin;
import com.TrashCashCampus.Repository.CampusLocationRepository;
import com.TrashCashCampus.Repository.ScanBinRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class ScanBinService {

    private final ScanBinRepository scanBinRepository;
    private final CampusLocationRepository campusLocationRepository;

    public ScanBinService(ScanBinRepository scanBinRepository, 
                          CampusLocationRepository campusLocationRepository) {
        this.scanBinRepository = scanBinRepository;
        this.campusLocationRepository = campusLocationRepository;
    }

    public ScanBin handleScan(String qrCode, String wasteType, String imageBase64, String locationName) {
        // Default values
        int pointsEarned = new Random().nextInt(6) + 5; // Points between 5-10
        String message = "Correct waste disposal!";
        String fact = "Recycling this plastic saves energy equivalent to powering a laptop for 2 hours.";
        
        // Check if location-based bonus applies
        if (locationName != null && !locationName.isEmpty()) {
            // Get the expected waste type for this location
            List<CampusLocation> locations = campusLocationRepository.findAll();
            
            for (CampusLocation location : locations) {
                if (location.getName().equals(locationName)) {
                    // If the waste type matches the expected bin type for this location, give bonus points
                    if (location.getBinType().equalsIgnoreCase(wasteType)) {
                        pointsEarned += 5; // Extra points for correct bin usage
                        message = "Perfect match! You've used the right bin at " + locationName;
                    }
                    break;
                }
            }
        }

        ScanBin log = new ScanBin();
        log.setQrCode(qrCode);
        log.setWasteType(wasteType);
        log.setImageBase64(imageBase64);
        log.setPointsEarned(pointsEarned);
        log.setMessage(message);
        log.setFact(fact);
        log.setLocationName(locationName);

        return scanBinRepository.save(log);
    }
    
    /**
     * Get QR code information for a specific location
     * @param locationName The name of the campus location
     * @return Map containing location information
     */
    public Map<String, Object> getLocationQrInfo(String locationName) {
        Map<String, Object> result = new HashMap<>();
        
        // Find the location in the database
        List<CampusLocation> locations = campusLocationRepository.findAll();
        CampusLocation targetLocation = null;
        
        for (CampusLocation location : locations) {
            if (location.getName().equals(locationName)) {
                targetLocation = location;
                break;
            }
        }
        
        if (targetLocation != null) {
            // Build the QR code info
            Map<String, String> qrInfo = new HashMap<>();
            qrInfo.put("binId", targetLocation.getBinType());
            qrInfo.put("binName", targetLocation.getName());
            qrInfo.put("description", targetLocation.getDescription());
            
            result.put("status", "success");
            result.put("qrInfo", qrInfo);
            result.put("location", targetLocation);
        } else {
            result.put("status", "error");
            result.put("message", "Location not found: " + locationName);
        }
        
        return result;
    }
}
