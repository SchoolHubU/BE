package com.shu.backend.domain.usersetting;


import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "user_setting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_setting_user",
                        columnNames = {"user_id"}
                )
        }
)
public class UserSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 전체 푸시 알람
    @Column(name = "allow_push", nullable = false)
    private Boolean allowPush = true;

    // 댓글 알림
    @Column(name = "allow_comment_notification", nullable = false)
    private Boolean allowCommentNotification = true;

    // 대댓글 알림
    @Column(name = "allow_reply_notification", nullable = false)
    private Boolean allowReplyNotification = true;

    // 좋아요 알ㄹ림
    @Column(name = "allow_like_notification", nullable = false)
    private Boolean allowLikeNotification = true;

    // 실시간 채팅 알림
    @Column(name = "allow_chat_notification", nullable = false)
    private Boolean allowChatNotification = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //=== 생성 메서드 ===//
    public static UserSetting create(User user) {
        UserSetting setting = new UserSetting();
        setting.user = user;
        return setting;
    }
}
