package com.TrashCashCampus.Service;

import com.TrashCashCampus.Entity.Reward;
import com.TrashCashCampus.Repository.RewardRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RewardService {

    private final RewardRepository rewardRepository;

    public RewardService(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    public List<Reward> getAllRewards() {
        return rewardRepository.findAll();
    }

    public Map<String, Object> redeemReward(String rewardId, int userPoints) {
        Optional<Reward> optionalReward = rewardRepository.findById(rewardId);
        if (optionalReward.isEmpty()) {
            throw new RuntimeException("Reward not found.");
        }

        Reward reward = optionalReward.get();

        if (userPoints < reward.getPointsCost()) {
            throw new RuntimeException("Insufficient points.");
        }

        String redemptionCode = reward.getName().toUpperCase().replace(" ", "_") + "_2025_" + UUID.randomUUID().toString().substring(0, 4);

        Map<String, Object> rewardInfo = new HashMap<>();
        rewardInfo.put("id", reward.getId());
        rewardInfo.put("name", reward.getName());
        rewardInfo.put("redemptionCode", redemptionCode);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("reward", rewardInfo);
        response.put("remainingPoints", userPoints - reward.getPointsCost()); // Dummy static logic

        return response;
    }
}
