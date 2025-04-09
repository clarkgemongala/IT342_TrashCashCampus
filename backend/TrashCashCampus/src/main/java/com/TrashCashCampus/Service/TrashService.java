package com.TrashCashCampus.Service;

import com.TrashCashCampus.Entity.TrashEntity;
import com.TrashCashCampus.Repository.TrashRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

@Service
public class TrashService {

    @Autowired
    private TrashRepository trashRepository;
    
    @Autowired
    private DataSource dataSource; // Injecting the DataSource

    // Method to get all users
    public List<TrashEntity> getAllUsers() {
        return trashRepository.findAll();
    }

    public String registerUser(TrashEntity user) {
        Optional<TrashEntity> existing = trashRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            return "Email already registered!";
        }
        trashRepository.save(user);
        return "Registration successful!";
    }

    public String loginUser(String email, String password) {
        Optional<TrashEntity> user = trashRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return "Login successful!";
        } else {
            return "Invalid email or password.";
        }
    }

    public String testDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                return "Database connection is successful!";
            } else {
                return "Database connection failed!";
            }
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
    
    
}