package com.TrashCashCampus.Repository;

import com.TrashCashCampus.Entity.TrashEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrashRepository extends JpaRepository<TrashEntity, Long> {
    Optional<TrashEntity> findByEmail(String email);
}
