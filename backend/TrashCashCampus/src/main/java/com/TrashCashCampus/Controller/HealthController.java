package com.TrashCashCampus.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.TrashCashCampus.Service.FirebaseService;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private FirebaseService firebaseService;
    
    @Autowired
    private Environment environment;
    
    @Value("${spring.application.name:TrashCashCampus}")
    private String applicationName;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        
        // Basic application info
        status.put("status", "UP");
        status.put("application", applicationName);
        status.put("timestamp", System.currentTimeMillis());
        
        // Environment info
        String[] activeProfiles = environment.getActiveProfiles();
        status.put("profiles", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"});
        
        // Check if the application has proper Firebase connectivity
        boolean firebaseAvailable = firebaseService.isAvailable();
        Map<String, Object> firebase = new HashMap<>();
        firebase.put("status", firebaseAvailable ? "UP" : "DOWN");
        firebase.put("mode", firebaseAvailable ? "NORMAL" : "DEGRADED");
        
        status.put("firebase", firebase);
        
        // Add component statuses
        Map<String, Object> components = new HashMap<>();
        components.put("firebase", firebase);
        status.put("components", components);
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
} 