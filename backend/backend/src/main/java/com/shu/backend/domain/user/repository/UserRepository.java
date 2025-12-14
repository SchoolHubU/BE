package com.shu.backend.domain.user.repository;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);


    boolean existsByRole(UserRole role);
}
