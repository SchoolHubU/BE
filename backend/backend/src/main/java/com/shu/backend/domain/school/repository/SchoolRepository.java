package com.shu.backend.domain.school.repository;

import com.shu.backend.domain.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByName(@Param("name") String name);

    public List<School> findByRegionId(Long regionId);

    List<School> findByRegionIdOrderByNameAsc(Long regionId);

    // 지역 ID와 학교명(keyword)을 기준으로 학교 검색
    @Query("SELECT s FROM School s WHERE s.region.id = :regionId " +
            "AND (:keyword IS NULL OR :keyword = '' OR s.name LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.name ASC")
    List<School> findSchoolsByRegionAndName(@Param("regionId") Long regionId, @Param("keyword") String keyword);

    boolean existsByRegionIdAndName(Long id, String name);
}
