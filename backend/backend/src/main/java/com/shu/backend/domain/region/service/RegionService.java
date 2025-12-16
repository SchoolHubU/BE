package com.shu.backend.domain.region.service;

import com.shu.backend.domain.region.dto.RegionCreateRequest;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.exception.RegionException;
import com.shu.backend.domain.region.exception.status.RegionErrorStatus;
import com.shu.backend.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    @Transactional
    public Long createRegion(RegionCreateRequest request){

        String name = request.getName();

        if(name == null || name.isEmpty()){
            throw new RegionException(RegionErrorStatus.INVALID_REGION_NAME);
        }

        if(regionRepository.existsByName(name.trim())){
            throw new RegionException(RegionErrorStatus.REGION_ALREADY_EXISTS);
        }

        Region region = regionRepository.save(Region.builder().name(name.trim()).build());

        return region.getId();
    }
}
