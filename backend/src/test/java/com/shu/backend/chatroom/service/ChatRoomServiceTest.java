package com.shu.backend.chatroom.service;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatmessage.service.ChatActionRateLimiter;
import com.shu.backend.domain.chatmessage.service.ChatMessageService;
import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.exception.ChatRoomException;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroom.service.ChatRoomService;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaType;
import com.shu.backend.domain.media.repository.MediaRepository;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.report.service.ReportService;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.usersetting.repository.UserSettingRepository;
import com.shu.backend.domain.boardprofile.service.BoardDisplayProfileService;
import com.shu.backend.global.file.FileStorageService;
import com.shu.backend.global.moderation.ContentModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatFeatureServiceTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomUserRepository chatRoomUserRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock UserRepository userRepository;
    @Mock MediaRepository mediaRepository;
    @Mock NotificationService notificationService;
    @Mock PushService pushService;
    @Mock UserSettingRepository userSettingRepository;
    @Mock BoardAccessPolicy boardAccessPolicy;
    @Mock ChatActionRateLimiter chatActionRateLimiter;
    @Mock FileStorageService fileStorageService;
    @Mock ContentModerationService contentModerationService;
    @Mock ReportService reportService;
    @Mock PostRepository postRepository;
    @Mock BoardDisplayProfileService boardDisplayProfileService;

    @InjectMocks ChatRoomService chatRoomService;
    @InjectMocks ChatMessageService chatMessageService;

    private static final Long SOURCE_POST_ID = 100L;
    private static final String ROOM_TITLE = "테스트 게시글";

    @BeforeEach
    void stubNotification() {
        // notificationService.create() → null 반환 (push 로직 skip)
        lenient().when(notificationService.create(any(), any(), anyLong(), anyString(), anyLong(), anyLong()))
                .thenReturn(null);
        lenient().when(boardAccessPolicy.requireVerifiedActiveUserWithSchool(anyLong()))
                .thenAnswer(invocation -> mockUser(invocation.getArgument(0)));
        lenient().when(userRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.of(mockUser(invocation.getArgument(0))));
        lenient().when(postRepository.findDetailById(anyLong()))
                .thenAnswer(invocation -> Optional.of(mockPost(invocation.getArgument(0))));
        lenient().when(postRepository.findByIdIn(any()))
                .thenReturn(List.of());
    }

    // =========================
    // ChatRoomService
    // =========================

    @Test
    void DM_채팅방이_없으면_생성되고_참여자2개가_생성된다() {
        Long myId = 1L;
        Long otherId = 2L;

        ChatRoom created = ChatRoom.ofDm(myId, otherId, SOURCE_POST_ID, ROOM_TITLE);
        setId(created, 10L);

        when(chatRoomRepository.findByUser1IdAndUser2IdAndSourcePostId(1L, 2L, SOURCE_POST_ID))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(created);

        ChatRoomDTO.CreateDmResponse res = chatRoomService.findOrCreateDm(myId, otherId, SOURCE_POST_ID, ROOM_TITLE);

        assertThat(res.getRoomId()).isEqualTo(10L);
        assertThat(res.getOtherUserId()).isEqualTo(otherId);
        assertThat(res.getDisplayName()).isEqualTo(ROOM_TITLE);

        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomUserRepository, times(2)).save(any(ChatRoomUser.class));
    }

    @Test
    void DM_채팅방이_이미_있으면_재사용하고_새로_생성하지_않는다() {
        Long myId = 2L;
        Long otherId = 1L;

        ChatRoom existed = ChatRoom.ofDm(myId, otherId, SOURCE_POST_ID, ROOM_TITLE);
        setId(existed, 99L);

        when(chatRoomRepository.findByUser1IdAndUser2IdAndSourcePostId(1L, 2L, SOURCE_POST_ID))
                .thenReturn(Optional.of(existed));

        ChatRoomDTO.CreateDmResponse res = chatRoomService.findOrCreateDm(myId, otherId, SOURCE_POST_ID, ROOM_TITLE);

        assertThat(res.getRoomId()).isEqualTo(99L);
        assertThat(res.getOtherUserId()).isEqualTo(otherId);

        verify(chatRoomRepository, never()).save(any());
        verify(chatRoomUserRepository, never()).save(any());
        verify(userRepository, never()).getReferenceById(any());
    }

    @Test
    void DM_요청은_같은_학교_학생끼리만_가능하다() {
        Long myId = 1L;
        Long otherId = 2L;

        when(boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId)).thenReturn(mockUser(myId, 20L));
        when(boardAccessPolicy.requireVerifiedActiveUserWithSchool(otherId)).thenReturn(mockUser(otherId, 21L));

        assertThatThrownBy(() -> chatRoomService.findOrCreateDm(myId, otherId, SOURCE_POST_ID, ROOM_TITLE))
                .isInstanceOf(ChatRoomException.class);

        verify(chatRoomRepository, never()).save(any());
        verify(chatRoomUserRepository, never()).save(any());
    }

    @Test
    void 같은_두_사용자가_다른_글에서_만나면_별도_채팅방이_생성된다() {
        Long myId = 1L;
        Long otherId = 2L;
        Long anotherPostId = 200L;

        ChatRoom room1 = ChatRoom.ofDm(myId, otherId, SOURCE_POST_ID, "글 A");
        setId(room1, 10L);
        ChatRoom room2 = ChatRoom.ofDm(myId, otherId, anotherPostId, "글 B");
        setId(room2, 11L);

        when(chatRoomRepository.findByUser1IdAndUser2IdAndSourcePostId(1L, 2L, SOURCE_POST_ID))
                .thenReturn(Optional.of(room1));
        when(chatRoomRepository.findByUser1IdAndUser2IdAndSourcePostId(1L, 2L, anotherPostId))
                .thenReturn(Optional.of(room2));

        ChatRoomDTO.CreateDmResponse res1 = chatRoomService.findOrCreateDm(myId, otherId, SOURCE_POST_ID, "글 A");
        ChatRoomDTO.CreateDmResponse res2 = chatRoomService.findOrCreateDm(myId, otherId, anotherPostId, "글 B");

        assertThat(res1.getRoomId()).isEqualTo(10L);
        assertThat(res2.getRoomId()).isEqualTo(11L);
        assertThat(res1.getRoomId()).isNotEqualTo(res2.getRoomId());
    }

    @Test
    void 내_채팅방_목록은_lastMessageAt_최신순으로_정렬된다_null은_맨아래() {
        Long myId = 1L;

        ChatRoom r1 = ChatRoom.ofDm(1L, 2L, 1L, "글1");
        setId(r1, 1L);
        setField(r1, "lastMessageAt", LocalDateTime.of(2025, 1, 1, 10, 0));

        ChatRoom r2 = ChatRoom.ofDm(1L, 3L, 2L, "글2");
        setId(r2, 2L);
        setField(r2, "lastMessageAt", LocalDateTime.of(2025, 1, 1, 12, 0));

        ChatRoom r3 = ChatRoom.ofDm(1L, 4L, 3L, "글3");
        setId(r3, 3L);
        setField(r3, "lastMessageAt", null);

        ChatRoomUser cru1 = mockCruForList(r1);
        ChatRoomUser cru2 = mockCruForList(r2);
        ChatRoomUser cru3 = mockCruForList(r3);

        when(chatRoomUserRepository.findByUserIdAndHiddenFalse(myId))
                .thenReturn(new ArrayList<>(List.of(cru1, cru3, cru2)));
        when(chatRoomUserRepository.findByChatRoomIdIn(List.of(2L, 1L, 3L)))
                .thenReturn(List.of());
        when(chatMessageRepository.findAllById(List.of()))
                .thenReturn(List.of());
        when(chatMessageRepository.countUnreadByRoomIds(List.of(2L, 1L, 3L), myId))
                .thenReturn(List.of());

        ChatRoomDTO.RoomListResponse res = chatRoomService.getMyRooms(myId);

        assertThat(res.getRooms()).hasSize(3);
        assertThat(res.getRooms().get(0).getRoomId()).isEqualTo(2L);
        assertThat(res.getRooms().get(1).getRoomId()).isEqualTo(1L);
        assertThat(res.getRooms().get(2).getRoomId()).isEqualTo(3L);
    }

    @Test
    void 나가기_성공시_leave가_호출된다() {
        Long myId = 1L;
        Long roomId = 10L;

        ChatRoomUser cru = mock(ChatRoomUser.class);
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)).thenReturn(Optional.of(cru));

        chatRoomService.leave(myId, roomId);

        verify(cru).leave();
    }

    @Test
    void 나가기_멤버아니면_예외() {
        Long myId = 1L;
        Long roomId = 10L;

        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.leave(myId, roomId))
                .isInstanceOf(ChatRoomException.class);
    }

    @Test
    void 차단_성공시_block이_호출된다() {
        Long myId = 1L;
        Long roomId = 10L;

        ChatRoom room = ChatRoom.ofDm(1L, 2L, SOURCE_POST_ID, ROOM_TITLE);
        ChatRoomUser cru = mock(ChatRoomUser.class);
        when(cru.getChatRoom()).thenReturn(room);
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)).thenReturn(Optional.of(cru));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser(2L)));

        chatRoomService.block(myId, roomId);

        verify(cru).block();
    }

    // =========================
    // ChatMessageService
    // =========================

    @Test
    void TEXT_메시지_전송시_저장되고_수신자_자동복귀가_호출된다() {
        Long senderId = 1L;
        Long receiverId = 2L;

        ChatRoom room = ChatRoom.ofDm(senderId, receiverId, SOURCE_POST_ID, ROOM_TITLE);
        setId(room, 10L);
        setField(room, "user1Id", 1L);
        setField(room, "user2Id", 2L);

        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        ChatRoomUser senderCru = mock(ChatRoomUser.class);
        when(senderCru.isBlocked()).thenReturn(false);

        ChatRoomUser receiverCru = mock(ChatRoomUser.class);
        when(receiverCru.isBlocked()).thenReturn(false);

        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, senderId)).thenReturn(Optional.of(senderCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, receiverId)).thenReturn(Optional.of(receiverCru));

        User sender = mockUser();
        when(userRepository.getReferenceById(senderId)).thenReturn(sender);

        ChatMessage saved = ChatMessage.builder()
                .type(ChatMessage.MessageType.TEXT)
                .content("hi")
                .chatRoom(room)
                .sender(sender)
                .build();
        setId(saved, 100L);
        setCreatedAt(saved, LocalDateTime.of(2025, 1, 1, 12, 30));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO.SendRequest req = new ChatMessageDTO.SendRequest();
        req.setRoomId(10L);
        req.setType(ChatMessageDTO.MessageType.TEXT);
        req.setContent("hi");

        ChatMessageDTO.MessageResponse res = chatMessageService.send(senderId, req);

        assertThat(res.getMessageId()).isEqualTo(100L);
        assertThat(res.getType()).isEqualTo(ChatMessageDTO.MessageType.TEXT);
        assertThat(res.getContent()).isEqualTo("hi");
        assertThat(res.getMedias()).isEmpty();

        verify(receiverCru).rejoinIfLeftOrHidden();
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void IMAGE_메시지_전송시_Media가_저장된다() {
        Long senderId = 1L;
        Long receiverId = 2L;

        ChatRoom room = ChatRoom.ofDm(senderId, receiverId, SOURCE_POST_ID, ROOM_TITLE);
        setId(room, 10L);
        setField(room, "user1Id", 1L);
        setField(room, "user2Id", 2L);

        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        ChatRoomUser senderCru = mock(ChatRoomUser.class);
        when(senderCru.isBlocked()).thenReturn(false);

        ChatRoomUser receiverCru = mock(ChatRoomUser.class);
        when(receiverCru.isBlocked()).thenReturn(false);

        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, senderId)).thenReturn(Optional.of(senderCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, receiverId)).thenReturn(Optional.of(receiverCru));

        User sender = mockUser();
        when(userRepository.getReferenceById(senderId)).thenReturn(sender);

        ChatMessage savedMsg = ChatMessage.builder()
                .type(ChatMessage.MessageType.IMAGE)
                .content(null)
                .chatRoom(room)
                .sender(sender)
                .build();
        setId(savedMsg, 200L);
        setCreatedAt(savedMsg, LocalDateTime.of(2025, 1, 1, 12, 31));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        Media savedMedia = mock(Media.class);
        when(savedMedia.getId()).thenReturn(300L);
        when(savedMedia.getUrl()).thenReturn("https://s3/.../img.png");
        when(savedMedia.getMediaType()).thenReturn(MediaType.IMAGE);
        when(savedMedia.isApprovedChatUploadBy(senderId)).thenReturn(true);
        when(mediaRepository.findByIdAndUploaderId(300L, senderId)).thenReturn(Optional.of(savedMedia));
        when(fileStorageService.toPresignedReadUrl("https://s3/.../img.png")).thenReturn("https://s3/.../img.png");

        ChatMessageDTO.SendRequest req = new ChatMessageDTO.SendRequest();
        req.setRoomId(10L);
        req.setType(ChatMessageDTO.MessageType.IMAGE);
        req.setMediaId(300L);

        ChatMessageDTO.MessageResponse res = chatMessageService.send(senderId, req);

        assertThat(res.getMessageId()).isEqualTo(200L);
        assertThat(res.getType()).isEqualTo(ChatMessageDTO.MessageType.IMAGE);
        assertThat(res.getContent()).isNull();
        assertThat(res.getMedias()).hasSize(1);
        assertThat(res.getMedias().get(0).getId()).isEqualTo(300L);

        verify(receiverCru).rejoinIfLeftOrHidden();
    }

    @Test
    void IMAGE_메시지인데_imageUrl이_없으면_예외() {
        Long senderId = 1L;
        Long receiverId = 2L;

        ChatRoom room = ChatRoom.ofDm(senderId, receiverId, SOURCE_POST_ID, ROOM_TITLE);
        setId(room, 10L);
        setField(room, "user1Id", 1L);
        setField(room, "user2Id", 2L);

        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        ChatRoomUser senderCru = mock(ChatRoomUser.class);
        when(senderCru.isBlocked()).thenReturn(false);

        ChatRoomUser receiverCru = mock(ChatRoomUser.class);
        when(receiverCru.isBlocked()).thenReturn(false);

        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, senderId)).thenReturn(Optional.of(senderCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, receiverId)).thenReturn(Optional.of(receiverCru));

        when(userRepository.getReferenceById(senderId)).thenReturn(mock(User.class));

        ChatMessageDTO.SendRequest req = new ChatMessageDTO.SendRequest();
        req.setRoomId(10L);
        req.setType(ChatMessageDTO.MessageType.IMAGE);

        assertThatThrownBy(() -> chatMessageService.send(senderId, req))
                .isInstanceOf(ChatMessageException.class);

        verify(chatMessageRepository, never()).save(any());
        verify(mediaRepository, never()).findByIdAndUploaderId(anyLong(), anyLong());
    }

    @Test
    void 차단_상태면_메시지_전송_예외() {
        Long senderId = 1L;
        Long receiverId = 2L;

        ChatRoom room = ChatRoom.ofDm(senderId, receiverId, SOURCE_POST_ID, ROOM_TITLE);
        setId(room, 10L);
        setField(room, "user1Id", 1L);
        setField(room, "user2Id", 2L);

        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        ChatRoomUser senderCru = mock(ChatRoomUser.class);
        when(senderCru.isBlocked()).thenReturn(true);

        ChatRoomUser receiverCru = mock(ChatRoomUser.class);

        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, senderId)).thenReturn(Optional.of(senderCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, receiverId)).thenReturn(Optional.of(receiverCru));

        ChatMessageDTO.SendRequest req = new ChatMessageDTO.SendRequest();
        req.setRoomId(10L);
        req.setType(ChatMessageDTO.MessageType.TEXT);
        req.setContent("hi");

        assertThatThrownBy(() -> chatMessageService.send(senderId, req))
                .isInstanceOf(ChatMessageException.class);

        verify(chatMessageRepository, never()).save(any());
        verify(receiverCru, never()).rejoinIfLeftOrHidden();
    }

    @Test
    void 상대가_나갔거나_숨김이면_전송시_rejoinIfLeftOrHidden가_호출된다() {
        Long senderId = 1L;
        Long receiverId = 2L;

        ChatRoom room = ChatRoom.ofDm(senderId, receiverId, SOURCE_POST_ID, ROOM_TITLE);
        setId(room, 10L);
        setField(room, "user1Id", 1L);
        setField(room, "user2Id", 2L);

        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        ChatRoomUser senderCru = mock(ChatRoomUser.class);
        when(senderCru.isBlocked()).thenReturn(false);

        ChatRoomUser receiverCru = mock(ChatRoomUser.class);
        when(receiverCru.isBlocked()).thenReturn(false);

        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, senderId)).thenReturn(Optional.of(senderCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(10L, receiverId)).thenReturn(Optional.of(receiverCru));

        User sender = mockUser();
        when(userRepository.getReferenceById(senderId)).thenReturn(sender);

        ChatMessage saved = ChatMessage.builder()
                .type(ChatMessage.MessageType.TEXT)
                .content("ping")
                .chatRoom(room)
                .sender(sender)
                .build();
        setId(saved, 101L);
        setCreatedAt(saved, LocalDateTime.now());

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        ChatMessageDTO.SendRequest req = new ChatMessageDTO.SendRequest();
        req.setRoomId(10L);
        req.setType(ChatMessageDTO.MessageType.TEXT);
        req.setContent("ping");

        chatMessageService.send(senderId, req);

        verify(receiverCru).rejoinIfLeftOrHidden();
    }

    @Test
    void 채팅방_입장시_최근50개를_조회한다() {
        Long myId = 1L;
        Long roomId = 10L;

        ChatRoom room = ChatRoom.ofDm(myId, 2L, SOURCE_POST_ID, ROOM_TITLE);
        ChatRoomUser myCru = mock(ChatRoomUser.class);
        when(myCru.getChatRoom()).thenReturn(room);
        when(myCru.isBlocked()).thenReturn(false);
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId))
                .thenReturn(Optional.of(myCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, 2L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser(2L)));

        User sender = mockUser();

        ChatMessage m1 = ChatMessage.builder()
                .type(ChatMessage.MessageType.TEXT)
                .content("A")
                .chatRoom(null)
                .sender(sender)
                .build();
        setId(m1, 3L);
        setCreatedAt(m1, LocalDateTime.now());

        ChatMessage m2 = ChatMessage.builder()
                .type(ChatMessage.MessageType.TEXT)
                .content("B")
                .chatRoom(null)
                .sender(sender)
                .build();
        setId(m2, 2L);
        setCreatedAt(m2, LocalDateTime.now());

        when(chatMessageRepository.findTop50ByChatRoomIdOrderByIdDesc(roomId))
                .thenReturn(List.of(m1, m2));

        ChatMessageDTO.MessageListResponse res = chatMessageService.getMessages(myId, roomId, null);

        assertThat(res.getRoomId()).isEqualTo(roomId);
        assertThat(res.getMessages()).hasSize(2);
        verify(chatMessageRepository).findTop50ByChatRoomIdOrderByIdDesc(roomId);
    }

    @Test
    void 커서기반_조회_lastId가_있으면_idLessThan으로_조회한다() {
        Long myId = 1L;
        Long roomId = 10L;
        Long lastId = 50L;

        ChatRoom room = ChatRoom.ofDm(myId, 2L, SOURCE_POST_ID, ROOM_TITLE);
        ChatRoomUser myCru = mock(ChatRoomUser.class);
        when(myCru.getChatRoom()).thenReturn(room);
        when(myCru.isBlocked()).thenReturn(false);
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId))
                .thenReturn(Optional.of(myCru));
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, 2L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser(2L)));

        User sender = mockUser();

        ChatMessage m1 = ChatMessage.builder()
                .type(ChatMessage.MessageType.TEXT)
                .content("older")
                .chatRoom(null)
                .sender(sender)
                .build();
        setId(m1, 49L);
        setCreatedAt(m1, LocalDateTime.now());

        when(chatMessageRepository.findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, lastId))
                .thenReturn(List.of(m1));

        ChatMessageDTO.MessageListResponse res = chatMessageService.getMessages(myId, roomId, lastId);

        assertThat(res.getMessages()).hasSize(1);
        assertThat(res.getMessages().get(0).getMessageId()).isEqualTo(49L);

        verify(chatMessageRepository).findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, lastId);
        verify(chatMessageRepository, never()).findTop50ByChatRoomIdOrderByIdDesc(any());
    }

    @Test
    void 읽음처리시_lastReadMessageId가_갱신된다() {
        Long myId = 1L;
        Long roomId = 10L;
        Long messageId = 123L;

        ChatRoomUser cru = mock(ChatRoomUser.class);
        when(chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)).thenReturn(Optional.of(cru));
        when(chatMessageRepository.existsByIdAndChatRoomId(messageId, roomId)).thenReturn(true);

        chatMessageService.read(myId, roomId, messageId);

        verify(cru).read(messageId);
    }

    // =========================
    // helpers
    // =========================

    private static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private static void setCreatedAt(Object entity, LocalDateTime t) {
        ReflectionTestUtils.setField(entity, "createdAt", t);
    }

    private static void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    private static User mockUser() {
        return mockUser(1L);
    }

    private static User mockUser(Long id) {
        return mockUser(id, 20L);
    }

    private static User mockUser(Long id, Long schoolId) {
        Region region = Region.builder().name("Seoul").build();
        setId(region, 10L);

        School school = School.builder()
                .name("Teenple High " + schoolId)
                .region(region)
                .build();
        setId(school, schoolId);

        User user = User.builder()
                .username("student" + id)
                .email("student" + id + "@example.com")
                .password("password")
                .nickname(ROOM_TITLE)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .school(school)
                .verified(true)
                .gender(Gender.MALE)
                .grade(Grade.FIRST)
                .phoneNumber("0101234567" + id)
                .phoneVerified(true)
                .build();
        setId(user, id);
        return user;
    }

    private static Post mockPost(Long id) {
        Post post = Post.builder()
                .title(ROOM_TITLE)
                .content("content")
                .user(mockUser(1L))
                .build();
        setId(post, id);
        return post;
    }

    private static ChatRoomUser mockCruForList(ChatRoom room) {
        ChatRoomUser cru = mock(ChatRoomUser.class);
        when(cru.getChatRoom()).thenReturn(room);
        when(cru.isBlocked()).thenReturn(false);
        return cru;
    }
}
