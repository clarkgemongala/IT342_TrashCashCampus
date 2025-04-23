package com.TrashCashCampus.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.TrashCashCampus.Service.FirebaseService;
import com.TrashCashCampus.dto.PickupLocationResponse;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/pickup-locations")
public class PickupLocationController {

    @Autowired
    private FirebaseService firebaseService;

    @GetMapping
    public ResponseEntity<PickupLocationResponse> getPickupLocations() {
        try {
            List<Map<String, Object>> locations = firebaseService.getAllDocuments("pickup-locations");
            
            PickupLocationResponse response = new PickupLocationResponse();
            response.setLocations(locations);
            
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickupLocationResponse> getPickupLocationById(@PathVariable("id") String id) {
        try {
            Map<String, Object> location = firebaseService.getDocument("pickup-locations", id);
            
            if (location == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Map<String, Object>> locations = new ArrayList<>();
            locations.add(location);
            
            PickupLocationResponse response = new PickupLocationResponse();
            response.setLocations(locations);
            
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 