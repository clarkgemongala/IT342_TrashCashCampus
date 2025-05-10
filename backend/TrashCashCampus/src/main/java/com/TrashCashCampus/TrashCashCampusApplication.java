package com.TrashCashCampus;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.TrashCashCampus.Entity.Reward;
import com.TrashCashCampus.Repository.RewardRepository;

@SpringBootApplication
public class TrashCashCampusApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrashCashCampusApplication.class, args);
	}
	
	
	 @Bean
	    CommandLineRunner seedRewards(RewardRepository repo) {
	        return args -> {
	            if (repo.count() == 0) {
	                repo.saveAll(List.of(
	                    new Reward("REWARD_001", "Campus Coffee Voucher", 50),
	                    new Reward("REWARD_002", "Eco Merchandise", 100)
	                ));
	                System.out.println("Sample rewards seeded.");
	            }
	        };
	    }

}
