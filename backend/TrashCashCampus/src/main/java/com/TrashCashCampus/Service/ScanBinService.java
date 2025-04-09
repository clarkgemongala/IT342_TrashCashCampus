package com.TrashCashCampus.Service;

import com.TrashCashCampus.Entity.ScanBin;
import com.TrashCashCampus.Repository.ScanBinRepository;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ScanBinService {

    private final ScanBinRepository scanBinRepository;

    public ScanBinService(ScanBinRepository scanBinRepository) {
        this.scanBinRepository = scanBinRepository;
    }

    public ScanBin handleScan(String qrCode, String wasteType, String imageBase64) {
        int pointsEarned = new Random().nextInt(6) + 5; // Points between 5-10
        String message = "Correct waste disposal!";
        String fact = "Recycling this plastic saves energy equivalent to powering a laptop for 2 hours.";

        ScanBin log = new ScanBin();
        log.setQrCode(qrCode);
        log.setWasteType(wasteType);
        log.setImageBase64(imageBase64);
        log.setPointsEarned(pointsEarned);
        log.setMessage(message);
        log.setFact(fact);

        return scanBinRepository.save(log);
    }
}
