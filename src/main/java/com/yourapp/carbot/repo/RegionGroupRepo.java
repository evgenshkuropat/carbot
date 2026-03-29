package com.yourapp.carbot.repo;

import com.yourapp.carbot.domain.RegionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionGroupRepo extends JpaRepository<RegionGroup, Long> {

    Optional<RegionGroup> findByCode(String code);

    List<RegionGroup> findByRegionId(Long regionId);
}