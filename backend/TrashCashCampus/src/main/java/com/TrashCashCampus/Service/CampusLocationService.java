package com.TrashCashCampus.Service;

import com.TrashCashCampus.Entity.CampusLocation;
import com.TrashCashCampus.Repository.CampusLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

@Service
public class CampusLocationService {

    private final CampusLocationRepository campusLocationRepository;

    public CampusLocationService(CampusLocationRepository campusLocationRepository) {
        this.campusLocationRepository = campusLocationRepository;
        // We'll initialize default locations after the context is fully refreshed
    }
    
    // Initialize locations after the entire context is refreshed
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("Application context refreshed, initializing default locations if needed");
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
        try {
            List<CampusLocation> existingLocations = campusLocationRepository.findAll();
            
            // Skip if we already have locations in the database
            if (!existingLocations.isEmpty()) {
                System.out.println("Default locations already exist. Found " + existingLocations.size() + " locations.");
                return;
            }
            
            System.out.println("No existing locations found. Creating default locations...");
            
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
                try {
                    campusLocationRepository.save(location);
                    System.out.println("Created default location: " + location.getName());
                } catch (Exception e) {
                    System.err.println("Error saving default location: " + location.getName() + " - " + e.getMessage());
                }
            }
            
            System.out.println("Default locations created successfully");
        } catch (Exception e) {
            System.err.println("Error initializing default locations: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 