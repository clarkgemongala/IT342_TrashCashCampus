package com.TrashCashCampus.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Object> handleError(HttpServletRequest request) {
        // For API requests, return JSON error
        String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (path != null && path.startsWith("/api/")) {
            return new ResponseEntity<>(
                    Map.of(
                        "status", "error",
                        "message", "The requested resource was not found",
                        "path", path
                    ),
                    HttpStatus.NOT_FOUND
            );
        }
        
        // For frontend routes, redirect to the React app
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "https://it342-trashcashcampus.onrender.com");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
} 