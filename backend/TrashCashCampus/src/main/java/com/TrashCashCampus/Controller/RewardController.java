package com.TrashCashCampus.Controller;

import com.TrashCashCampus.Entity.Reward;
import com.TrashCashCampus.Service.RewardService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public Map<String, Object> getRewards() {
        List<Reward> rewards = rewardService.getAllRewards();
        Map<String, Object> response = new HashMap<>();
        response.put("rewards", rewards);
        return response;
    }

    @PostMapping("/redeem")
    public Map<String, Object> redeem(@RequestBody Map<String, String> request) {
        String rewardId = request.get("rewardId");
        int userPoints = 250; // Later: fetch actual points from User table

        return rewardService.redeemReward(rewardId, userPoints);
    }
}
