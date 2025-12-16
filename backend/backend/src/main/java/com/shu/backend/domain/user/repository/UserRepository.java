package com.shu.backend.domain.user.repository;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);


    boolean existsByRole(UserRole role);

    @Query("""
    select u from User u
    join fetch u.school s
    join fetch s.region r
    where u.id = :userId
    """)
    Optional<User> findByIdWithSchoolAndRegion(@Param("userId") Long userId);
}
