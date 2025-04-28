package com.TrashCashCampus.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Forward requests that should be handled by the frontend router
     * to index.html so the client-side routing can handle them
     */
    @GetMapping(value = {"", "/", "/login", "/dashboard", "/bins", "/rewards", "/users", "/admin-management"})
    public String forwardToIndex() {
        return "index";
    }
} 