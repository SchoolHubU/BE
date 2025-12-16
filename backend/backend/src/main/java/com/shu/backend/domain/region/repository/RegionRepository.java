package com.shu.backend.domain.region.repository;

import com.shu.backend.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    boolean existsByName(String name);
}
