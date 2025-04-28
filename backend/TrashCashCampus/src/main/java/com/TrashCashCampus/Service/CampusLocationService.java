package com.TrashCashCampus.Service;

import com.TrashCashCampus.Entity.CampusLocation;
import com.TrashCashCampus.Repository.CampusLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampusLocationService {

    private final CampusLocationRepository campusLocationRepository;

    public CampusLocationService(CampusLocationRepository campusLocationRepository) {
        this.campusLocationRepository = campusLocationRepository;
        
        // On service initialization, make sure we have the default campus locations
        ensureDefaultLocationsExist();
    }

    public List<CampusLocation> getAllLocations() {
        return campusLocationRepository.findAll();
    }

    public CampusLocation saveLocation(CampusLocation location) {
        return campusLocationRepository.save(location);
    }

    /**
     * Initialize the database with the default campus locations if not already present
     */
    private void ensureDefaultLocationsExist() {
        List<CampusLocation> existingLocations = campusLocationRepository.findAll();
        
        // Skip if we already have locations in the database
        if (!existingLocations.isEmpty()) {
            return;
        }
        
        // Initialize with the 8 campus locations specified
        CampusLocation[] defaultLocations = {
            new CampusLocation("NGE Building", 10.294460890889612, 123.881064193439, 
                "NGE Building - Recyclable waste collection", "recyclable"),
            new CampusLocation("RTL Building", 10.294840425650719, 123.88049020072194, 
                "RTL Building - General waste collection", "general"),
            new CampusLocation("Engineering Department", 10.294758615762326, 123.87985451718372, 
                "Engineering Department - Recyclable waste collection", "recyclable"),
            new CampusLocation("Junior High Building", 10.295550323525, 123.87961580058581, 
                "Junior High Building - Paper waste collection", "paper"),
            new CampusLocation("Gymnasium", 10.296260219787618, 123.87957020303742, 
                "Gymnasium - Plastic waste collection", "plastic"),
            new CampusLocation("Canteen", 10.296128268936334, 123.88038693566146, 
                "Canteen - Food waste collection", "food"),
            new CampusLocation("ACAD Building", 10.295721859947419, 123.88122914928363, 
                "ACAD Building - Paper waste collection", "paper"),
            new CampusLocation("GLE Building", 10.295378787021845, 123.88130693334064, 
                "GLE Building - General waste collection", "general")
        };
        
        // Save each default location
        for (CampusLocation location : defaultLocations) {
            campusLocationRepository.save(location);
        }
    }
} 