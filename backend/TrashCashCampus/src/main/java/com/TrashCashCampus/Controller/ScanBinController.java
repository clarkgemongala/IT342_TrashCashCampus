package com.TrashCashCampus.Controller;

import com.TrashCashCampus.Entity.ScanBin;
import com.TrashCashCampus.Service.ScanBinService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bins")
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

        ScanBin saved = scanBinService.handleScan(qrCode, wasteType, imageBase64);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("pointsEarned", saved.getPointsEarned());
        response.put("message", saved.getMessage());
        response.put("totalPoints", 250); // You can dynamically get this from a User table later
        response.put("fact", saved.getFact());

        return response;
    }
}
