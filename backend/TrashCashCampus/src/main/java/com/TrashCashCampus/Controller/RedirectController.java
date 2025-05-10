package com.TrashCashCampus.Controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/**")
public class RedirectController {

    /**
     * Fallback handler for all unmatched routes - redirects to frontend
     */
    @GetMapping
    public ResponseEntity<Void> redirectToFrontend() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "https://it342-trashcashcampus.onrender.com");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
} 