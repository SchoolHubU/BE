package com.shu.backend.global.batch.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomCleanupScheduler {

    private final ChatRoomCleanupRepositoryImpl cleanupRepository;

    // 매일 새벽 4시 (서울)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupEmptyDmRooms() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int batchSize = 1000;

        while (true) {
            List<Long> roomIds = cleanupRepository.findDeletableEmptyDmRoomIds(cutoff, batchSize);
            if (roomIds.isEmpty()) break;

            // FK 때문에 자식 먼저
            cleanupRepository.deleteChatRoomUsersByRoomIds(roomIds);
            cleanupRepository.deleteChatRoomsByRoomIds(roomIds);
        }
    }
}