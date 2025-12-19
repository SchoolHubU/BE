package com.shu.backend.domain.chatroom.repository;

import com.shu.backend.domain.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);
}