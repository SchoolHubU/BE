package com.shu.backend.domain.chatmessage.repository;

import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findTop50ByChatRoomIdOrderByIdDesc(Long roomId);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long lastId);
}