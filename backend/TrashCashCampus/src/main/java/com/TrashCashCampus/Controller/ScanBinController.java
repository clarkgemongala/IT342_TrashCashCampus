package com.TrashCashCampus.Controller;

import com.TrashCashCampus.Entity.ScanBin;
import com.TrashCashCampus.Service.ScanBinService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bins")
@CrossOrigin(origins = {"http://localhost:5173", "http://10.0.2.2:8080", "http://10.0.2.2", "https://trashcashcampus-web.onrender.com"})
public class ScanBinController {

    private final ScanBinService scanBinService;

    public ScanBinController(ScanBinService scanBinService) {
        this.scanBinService = scanBinService;
    }

    @PostMapping("/scan")
    public Map<String, Object> scanBin(@RequestBody Map<String, String> request) {
        String qrCode = request.get("qrCode");
        String wasteType = request.get("wasteType");
        String imageBase64 = request.get("imageBase64");
        String locationName = request.get("locationName");

        ScanBin saved = scanBinService.handleScan(qrCode, wasteType, imageBase64, locationName);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("pointsEarned", saved.getPointsEarned());
        response.put("message", saved.getMessage());
        response.put("totalPoints", 250); // You can dynamically get this from a User table later
        response.put("fact", saved.getFact());

        return response;
    }
    
    /**
     * Get QR code info for a specific campus location
     * @param locationName The name of the campus location
     * @return QR code information for that location
     */
    @GetMapping("/location/{locationName}")
    public Map<String, Object> getLocationQrInfo(@PathVariable String locationName) {
        Map<String, Object> locationInfo = scanBinService.getLocationQrInfo(locationName);
        
        return locationInfo;
    }
}
