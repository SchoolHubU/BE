package com.shu.backend.domain.pushtoken.repository;

import com.shu.backend.domain.pushtoken.entity.PushToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findByUserIdAndIsActiveTrue(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PushToken t set t.isActive = false where t.userId = :userId")
    int deactivateAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PushToken t set t.isActive = false where t.token = :token")
    int deactivateByToken(@Param("token") String token);
}
