package com.TrashCashCampus.Controller;

import com.TrashCashCampus.Entity.TrashEntity;
import com.TrashCashCampus.Service.TrashService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trash")
public class TrashController {

    @Autowired
    private TrashService trashService;

    @PostMapping("/register")
    public String register(@RequestBody TrashEntity user) {
        System.out.println("Register endpoint hit with user: " + user.getEmail());
        return trashService.registerUser(user);
    }

    @PostMapping("/login")
    public String login(@RequestBody TrashEntity user) {
        return trashService.loginUser(user.getEmail(), user.getPassword());
    }

    // Endpoint to get all users
    @GetMapping("/users")
    public List<TrashEntity> getAllUsers() {
        return trashService.getAllUsers();
    }

    @GetMapping("/test-connection")
    public String testConnection() {
        return trashService.testDatabaseConnection();
    }
    
}