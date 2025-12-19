package com.shu.backend.domain.chatroomuser.repository;

import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
    Optional<ChatRoomUser> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    // 내 방 목록 (숨김X, 차단X)
    List<ChatRoomUser> findByUserIdAndHiddenFalseAndBlockedAtIsNull(Long userId);
}