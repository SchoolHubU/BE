package com.shu.backend.domain.user.service;

import com.shu.backend.domain.block.repository.UserBlockRepository;
import com.shu.backend.domain.bookmark.repository.BookmarkRepository;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.notification.repository.NotificationRepository;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import com.shu.backend.domain.reaction.repository.ReactionRepository;
import com.shu.backend.domain.user.dto.UserDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.RefreshTokenRepository;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRepository;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.global.exception.GeneralException;
import com.shu.backend.global.file.FileStorageService;
import com.shu.backend.global.util.HtmlText;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PushTokenRepository pushTokenRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserSchoolVerificationRequestRepository verificationRequestRepository;
    private final UserSchoolVerificationRepository verificationRepository;

    @Transactional(readOnly = true)
    public UserDTO.ProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdWithSchoolAndRegion(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        long myPostCount = postRepository.countActiveByUserId(userId);
        long myCommentCount = commentRepository.countActiveByUserId(userId);

        return UserDTO.ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(fileStorageService.toPresignedReadUrl(user.getProfileImageUrl()))
                .schoolName(user.getSchool().getName())
                .grade(user.getGrade())
                .gender(user.getGender())
                .verified(user.isVerified())
                .phoneVerified(user.isPhoneVerified())
                .myPostCount(myPostCount)
                .myCommentCount(myCommentCount)
                .nicknameChangedAt(user.getNicknameChangedAt())
                .build();
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        if (user.getNickname().equals(nickname)) {
            throw new GeneralException(UserErrorStatus.SAME_NICKNAME);
        }
        if (user.getNicknameChangedAt() != null &&
                user.getNicknameChangedAt().isAfter(LocalDateTime.now().minusDays(30))) {
            throw new GeneralException(UserErrorStatus.NICKNAME_CHANGE_COOLDOWN);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new GeneralException(UserErrorStatus.EXIST_NICKNAME);
        }

        user.updateNickname(nickname);
        log.info("Nickname updated: userId={}", userId);
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new GeneralException(UserErrorStatus.INVALID_PASSWORD);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new GeneralException(UserErrorStatus.SAME_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteByUser(user);
        log.info("User password updated: userId={}", userId);
    }

    @Transactional(readOnly = true)
    public Slice<UserDTO.MyPostResponse> getMyPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequestUtils.of(page, size, 50);
        Pageable slicePageable = PageRequestUtils.slice(pageable);
        List<Object[]> rows = postRepository.findMyPostRows(userId, slicePageable);
        boolean hasNext = rows.size() > pageable.getPageSize();
        List<UserDTO.MyPostResponse> content = rows.stream()
                .limit(pageable.getPageSize())
                .map(r -> UserDTO.MyPostResponse.builder()
                        .postId((Long) r[0])
                        .title(HtmlText.unescape((String) r[1]))
                        .content(HtmlText.unescape((String) r[2]))
                        .postStatus(r[3] instanceof Enum<?> e ? e.name() : String.valueOf(r[3]))
                        .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                        .createdAt((LocalDateTime) r[5])
                        .commentCount(r[6] == null ? 0 : ((Number) r[6]).intValue())
                        .boardTitle((String) r[7])
                        .build()
                ).toList();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Transactional(readOnly = true)
    public Slice<UserDTO.MyCommentResponse> getMyComments(Long userId, int page, int size) {
        Pageable pageable = PageRequestUtils.of(page, size, 50);
        Pageable slicePageable = PageRequestUtils.slice(pageable);
        List<Object[]> rows = commentRepository.findMyCommentRows(userId, slicePageable);
        boolean hasNext = rows.size() > pageable.getPageSize();
        List<UserDTO.MyCommentResponse> content = rows.stream()
                .limit(pageable.getPageSize())
                .map(r -> UserDTO.MyCommentResponse.builder()
                        .commentId((Long) r[0])
                        .content(HtmlText.unescape((String) r[1]))
                        .postId((Long) r[2])
                        .postTitle(HtmlText.unescape((String) r[3]))
                        .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                        .createdAt((LocalDateTime) r[5])
                        .boardTitle((String) r[6])
                        .build()
                ).toList();
        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));
        String oldUrl = user.getProfileImageUrl();
        String imageUrl = fileStorageService.uploadProfileImage(file);
        user.updateProfileImageUrl(imageUrl);
        if (oldUrl != null && oldUrl.startsWith("http")) {
            fileStorageService.deletePublicFile(oldUrl);
        }
        log.info("Profile image updated: userId={}", userId);
        return fileStorageService.toPresignedReadUrl(imageUrl);
    }

    @Transactional(readOnly = true)
    public List<UserDTO.MyPostResponse> getLikedPosts(Long userId, int page, int size) {
        List<Long> postIds = reactionRepository.findLikedPostIds(userId, PageRequestUtils.of(page, size));
        if (postIds.isEmpty()) return Collections.emptyList();

        List<Object[]> rows = postRepository.findLikedPostRows(postIds);
        return rows.stream().map(r -> UserDTO.MyPostResponse.builder()
                .postId((Long) r[0])
                .title(HtmlText.unescape((String) r[1]))
                .content(HtmlText.unescape((String) r[2]))
                .postStatus(r[3] instanceof Enum<?> e ? e.name() : String.valueOf(r[3]))
                .likeCount(r[4] == null ? 0 : ((Number) r[4]).intValue())
                .createdAt((LocalDateTime) r[5])
                .commentCount(r[6] == null ? 0 : ((Number) r[6]).intValue())
                .build()
        ).toList();
    }

    /**
     * 탈퇴 요청: 즉시 삭제하지 않고 7일 유예 기간을 부여한다.
     * 실제 PII 파기는 UserWithdrawalScheduler가 만료 시 처리한다.
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        // refresh 토큰·푸시 토큰만 즉시 무효화해 추가 API 접근을 차단한다.
        refreshTokenRepository.deleteByUser(user);
        pushTokenRepository.deleteAllByUserId(userId);

        user.requestDeletion();
        userRepository.save(user);
        log.info("Account deletion requested: userId={}", userId);
    }

    /**
     * 유예 기간 만료 유저의 PII를 실제로 파기한다.
     * UserWithdrawalScheduler에서만 호출되어야 한다.
     */
    @Transactional
    public void purgeExpiredAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.USER_NOT_FOUND));

        // 1. 학생증 이미지(프라이빗 S3) 삭제
        List<UserSchoolVerificationRequest> verifications = verificationRequestRepository.findByUser(user);
        for (UserSchoolVerificationRequest v : verifications) {
            try {
                fileStorageService.deleteStudentCardImage(v.getRequestImageUrl());
            } catch (Exception e) {
                log.warn("[Purge] 학생증 S3 삭제 실패 (userId={}, url={}): {}", userId, v.getRequestImageUrl(), e.getMessage());
            }
        }
        verificationRequestRepository.deleteByUser(user);
        verificationRepository.deleteByUserId(userId);

        // 2. 프로필 이미지(퍼블릭 S3) 삭제
        String profileUrl = user.getProfileImageUrl();
        if (profileUrl != null && profileUrl.startsWith("http")) {
            try {
                fileStorageService.deletePublicFile(profileUrl);
            } catch (Exception e) {
                log.warn("[Purge] 프로필 이미지 S3 삭제 실패 (userId={}): {}", userId, e.getMessage());
            }
        }

        // 3. 행동 기록 삭제
        bookmarkRepository.deleteAllByUserId(userId);
        reactionRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        userBlockRepository.deleteByBlockerId(userId);
        userBlockRepository.deleteByBlockedId(userId);

        // 4. PII 비식별 처리 (게시글/댓글 FK 보존을 위해 행은 유지)
        user.anonymize();
        userRepository.save(user);
        log.info("Account purged: userId={}", userId);
    }
}
