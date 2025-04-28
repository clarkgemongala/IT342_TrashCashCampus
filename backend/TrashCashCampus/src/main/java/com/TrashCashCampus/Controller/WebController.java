package com.TrashCashCampus.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Redirect all frontend routes to the deployed frontend application
     */
    @GetMapping(value = {"", "/", "/login", "/dashboard", "/bins", "/rewards", "/users", "/admin-management"})
    public String redirectToFrontend() {
        return "redirect:https://it342-trashcashcampus.onrender.com";
    }
} 