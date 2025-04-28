package com.TrashCashCampus.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recycle-center")
@CrossOrigin(origins = {"http://localhost:5173", "http://10.0.2.2:8080", "http://10.0.2.2", "https://trashcashcampus-testenvironment--trashcash-campus.netlify.app", "https://trashcash-campus.netlify.app"})
public class RecycleCenterController {
} 