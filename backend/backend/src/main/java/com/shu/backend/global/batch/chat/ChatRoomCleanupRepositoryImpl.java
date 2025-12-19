package com.shu.backend.global.batch.chat;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatRoomCleanupRepositoryImpl {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 삭제 대상이 되는 "빈 DM 채팅방" ID 조회
     *
     * 조건:
     *  - 메시지가 한 번도 없는 방 (last_message_id IS NULL)
     *  - 생성 후 cutoff(예: 7일) 경과
     *  - 참여자 2명(DM)
     *  - 두 사용자 모두 hidden 상태
     *  - batch 단위(limit)로 조회
     */
    public List<Long> findDeletableEmptyDmRoomIds(LocalDateTime cutoff, int limit) {
        String sql = """
            SELECT cru.chat_room_id
            FROM chat_room_user cru
            JOIN chat_room r ON r.id = cru.chat_room_id
            WHERE r.last_message_id IS NULL
              AND r.created_at < ?
            GROUP BY cru.chat_room_id
            HAVING COUNT(*) = 2
               AND SUM(CASE WHEN cru.hidden = 1 THEN 1 ELSE 0 END) = 2
            LIMIT ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, i) -> rs.getLong(1),
                Timestamp.valueOf(cutoff),
                limit
        );
    }

    /**
     * 채팅방에 속한 사용자 관계(chat_room_user) 먼저 삭제
     * (FK 제약 때문에 부모(chat_room)보다 먼저 제거)
     */
    public int deleteChatRoomUsersByRoomIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) return 0;

        String inSql = String.join(",", roomIds.stream().map(x -> "?").toList());
        String sql = "DELETE FROM chat_room_user WHERE chat_room_id IN (" + inSql + ")";

        return jdbcTemplate.update(sql, roomIds.toArray());
    }

    /**
     * 채팅방(chat_room) 삭제
     */
    public int deleteChatRoomsByRoomIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) return 0;

        String inSql = String.join(",", roomIds.stream().map(x -> "?").toList());
        String sql = "DELETE FROM chat_room WHERE id IN (" + inSql + ")";

        return jdbcTemplate.update(sql, roomIds.toArray());
    }
}