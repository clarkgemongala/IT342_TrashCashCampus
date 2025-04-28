package com.TrashCashCampus.Controller;

import com.TrashCashCampus.Entity.CampusLocation;
import com.TrashCashCampus.Service.CampusLocationService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/locations")
@CrossOrigin(origins = {"http://localhost:5173", "http://10.0.2.2:8080", "http://10.0.2.2", "https://trashcashcampus-testenvironment--trashcash-campus.netlify.app", "https://trashcash-campus.netlify.app"})
public class CampusLocationController {

    private final CampusLocationService campusLocationService;

    public CampusLocationController(CampusLocationService campusLocationService) {
        this.campusLocationService = campusLocationService;
    }

    /**
     * Get all campus trash bin locations
     * @return List of campus locations
     */
    @GetMapping
    public List<CampusLocation> getAllLocations() {
        return campusLocationService.getAllLocations();
    }

    /**
     * Add a new campus location
     * @param location The location to add
     * @return The saved location with ID
     */
    @PostMapping
    public CampusLocation addLocation(@RequestBody CampusLocation location) {
        return campusLocationService.saveLocation(location);
    }

    /**
     * Update an existing campus location
     * @param id The location ID to update
     * @param location The updated location data
     * @return The updated location
     */
    @PutMapping("/{id}")
    public CampusLocation updateLocation(@PathVariable String id, @RequestBody CampusLocation location) {
        location.setId(id);
        return campusLocationService.saveLocation(location);
    }
} 