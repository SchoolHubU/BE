package com.shu.backend.domain.post.repository;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Slice<Post> findByBoardId(Long boardId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "board", "board.school", "board.region"})
    Page<Post> findByBoardIdAndPostStatusNot(Long boardId, PostStatus postStatus, Pageable pageable);

    /*@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);*/

    /*// 비관적 락 적용 테스트
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p join fetch p.user u where p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);

    // 낙관적 락 적용 테스트
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select p from Post p join fetch p.user u where p.id = :postId")
    Optional<Post> findByIdForOptimistic(@Param("postId") Long postId);*/

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.dislikeCount = p.dislikeCount + :delta where p.id = :postId")
    int updateDislikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Query("""
        select p
        from Post p
        join fetch p.user u
        join fetch p.board b
        where p.id = :postId
    """)
    Optional<Post> findDetailById(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user", "board", "board.school", "board.region"})
    Optional<Post> findWithAdminContextById(Long postId);

    @EntityGraph(attributePaths = {"board"})
    List<Post> findByIdIn(Collection<Long> postIds);

    @Query("""
        select p.id
        from Post p
        where p.board.id = :boardId
          and p.postStatus = :status
        order by p.id desc
    """)
    List<Long> findPostIdsByBoardId(
            @Param("boardId") Long boardId,
            @Param("status") PostStatus status,
            Pageable pageable
    );

    // 게시글 목록 조회시 필요한 정보만
    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            p.commentCount,
            u.profileImageUrl,
            p.createdAt,
            exists (
                select poll.id
                from com.shu.backend.domain.poll.entity.Poll poll
                where poll.post.id = p.id
            ),
            u.status,
            bdp.displayName,
            bdp.profileImageUrl
        from Post p
        join p.board b
        join p.user u
        left join BoardDisplayProfile bdp on bdp.board = b and bdp.user = u
        where b.id = :boardId
          and p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and p.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by
          case when :sortDirection = 'ASC' and :sortBy = 'createdAt' then p.createdAt end asc,
          case when :sortDirection = 'DESC' and :sortBy = 'createdAt' then p.createdAt end desc,
          case when :sortDirection = 'ASC' and :sortBy = 'likeCount' then p.likeCount end asc,
          case when :sortDirection = 'DESC' and :sortBy = 'likeCount' then p.likeCount end desc,
          case when :sortDirection = 'ASC' and :sortBy = 'viewCount' then p.viewCount end asc,
          case when :sortDirection = 'DESC' and :sortBy = 'viewCount' then p.viewCount end desc,
          case when :sortDirection = 'ASC' and :sortBy = 'commentCount' then p.commentCount end asc,
          case when :sortDirection = 'DESC' and :sortBy = 'commentCount' then p.commentCount end desc,
          p.id desc
    """)
    List<Object[]> findPostRowsByBoardId(
            @Param("boardId") Long boardId,
            @Param("currentUserId") Long currentUserId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            Pageable pageable
    );

    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            p.commentCount,
            u.profileImageUrl,
            p.createdAt,
            exists (
                select poll.id
                from com.shu.backend.domain.poll.entity.Poll poll
                where poll.post.id = p.id
            ),
            u.status,
            bdp.displayName,
            bdp.profileImageUrl
        from Post p
        join p.board b
        join p.user u
        left join BoardDisplayProfile bdp on bdp.board = b and bdp.user = u
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and (
                p.title like concat('%', :keyword, '%') escape '\\'
             or p.content like concat('%', :keyword, '%') escape '\\'
          )
          and b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL
          and b.defaultBoard = true
          and b.school.id = :schoolId
        order by p.id desc
    """)
    List<Object[]> searchAccessiblePostRowsByKeyword(
            @Param("keyword") String keyword,
            @Param("schoolId") Long schoolId,
            Pageable pageable
    );

    // 검색 조건에 맞는 postId size+1개 조회
    @Query("""
        select p.id
        from Post p
        join p.board b
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and (
                p.title like concat('%', :keyword, '%') escape '\\'
             or p.content like concat('%', :keyword, '%') escape '\\'
          )
          and b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL
          and b.defaultBoard = true
          and b.school.id = :schoolId
          and p.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by p.id desc
    """)
    List<Long> findSearchPostIds(
            @Param("keyword") String keyword,
            @Param("schoolId") Long schoolId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );


    // 21개의 postId를 통해 postRow 반환
    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            p.commentCount,
            u.profileImageUrl,
            p.createdAt,
            exists (
                select poll.id
                from com.shu.backend.domain.poll.entity.Poll poll
                where poll.post.id = p.id
            ),
            u.status,
            bdp.displayName,
            bdp.profileImageUrl
        from Post p
        join p.board b
        join p.user u
        left join BoardDisplayProfile bdp on bdp.board = b and bdp.user = u
        where p.id in :postIds
        order by p.id desc
    """)
    List<Object[]> findPostRowsByIds(
            @Param("postIds") Collection<Long> postIds
    );

    @Query("""
        select p.id
        from Post p
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and p.board.id = :boardId
          and (
                p.title like concat('%', :keyword, '%') escape '\\'
             or p.content like concat('%', :keyword, '%') escape '\\'
          )
          and p.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by p.id desc
    """)
    List<Long> findSearchPostIdsByBoardId(
            @Param("keyword") String keyword,
            @Param("boardId") Long boardId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.likeCount,
            p.createdAt,
            p.commentCount
        from Post p
        where p.id in :postIds
          and p.postStatus <> com.shu.backend.domain.post.enums.PostStatus.DELETED
        order by p.id desc
    """)
    List<Object[]> findLikedPostRows(@Param("postIds") Collection<Long> postIds);

    /**
     * 관리자 모니터링에서 삭제된 게시글은 목록에 노출하지 않으므로
     * 게시판 카드의 전체 개수에서도 같은 기준으로 제외한다.
     */
    int countByBoardAndPostStatusNot(Board board, PostStatus postStatus);

    /**
     * 관리자 게시글 목록의 상태별 집계를 계산한다.
     * HIDDEN은 전체 개수에 포함되는 부분집합이며 DELETED만 전체에서 제외된다.
     */
    long countByBoardAndPostStatus(Board board, PostStatus postStatus);

    boolean existsByBoardAndTitle(Board board, String title);

    @Query("select count(p) from Post p where p.user.id = :userId and p.postStatus <> com.shu.backend.domain.post.enums.PostStatus.DELETED")
    long countActiveByUserId(@Param("userId") Long userId);

    // 최근 N일간 특정 학교의 게시글을 좋아요 많은 순으로 조회 (이번 주 인기글)
    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            p.commentCount,
            u.profileImageUrl,
            p.createdAt,
            exists (
                select poll.id
                from com.shu.backend.domain.poll.entity.Poll poll
                where poll.post.id = p.id
            ),
            u.status,
            bdp.displayName,
            bdp.profileImageUrl
        from Post p
        join p.board b
        join p.user u
        left join BoardDisplayProfile bdp on bdp.board = b and bdp.user = u
        where b.school.id = :schoolId
          and b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL
          and b.defaultBoard = true
          and p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and p.createdAt >= :since
          and p.likeCount >= 1
          and p.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by p.likeCount desc
    """)
    List<Object[]> findHotPostRowsBySchoolId(
            @Param("schoolId") Long schoolId,
            @Param("since") LocalDateTime since,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            p.commentCount,
            u.profileImageUrl,
            p.createdAt,
            exists (
                select poll.id
                from com.shu.backend.domain.poll.entity.Poll poll
                where poll.post.id = p.id
            ),
            u.status,
            bdp.displayName,
            bdp.profileImageUrl
        from Post p
        join p.board b
        join p.user u
        left join BoardDisplayProfile bdp on bdp.board = b and bdp.user = u
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and p.createdAt >= :since
          and p.likeCount >= 1
          and b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL
          and b.defaultBoard = true
          and b.school.id = :schoolId
          and p.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by p.likeCount desc, p.commentCount desc, p.createdAt desc, p.id desc
    """)
    List<Object[]> findTopRecommendedPostRows(
            @Param("schoolId") Long schoolId,
            @Param("since") LocalDateTime since,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.likeCount,
            p.createdAt,
            p.commentCount,
            b.title
        from Post p
        join p.board b
        where p.user.id = :userId
          and p.postStatus <> com.shu.backend.domain.post.enums.PostStatus.DELETED
        order by p.id desc
    """)
    List<Object[]> findMyPostRows(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        select p.id
        from Post p
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.DELETED
          and p.deletedAt < :threshold
          and not exists (
              select r.id from com.shu.backend.domain.report.entity.Report r
              where r.targetType = com.shu.backend.domain.report.enums.TargetType.POST
                and r.targetId = p.id
                and r.status = com.shu.backend.domain.report.enums.ReportStatus.PENDING
          )
    """)
    List<Long> findPurgeablePostIds(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    @Query("""
        select
            p.id,
            p.title,
            p.content,
            p.postStatus,
            p.viewCount,
            p.anonymous,
            p.likeCount,
            p.dislikeCount,
            b.id,
            u.id,
            u.username,
            p.commentCount,
            u.profileImageUrl,
            p.createdAt,
            exists (
                select poll.id
                from com.shu.backend.domain.poll.entity.Poll poll
                where poll.post.id = p.id
            ),
            u.status,
            bdp.displayName,
            bdp.profileImageUrl
        from Post p
        join p.board b
        join p.user u
        left join BoardDisplayProfile bdp on bdp.board = b and bdp.user = u
        where p.postStatus = com.shu.backend.domain.post.enums.PostStatus.ACTIVE
          and b.scope = com.shu.backend.domain.board.enums.BoardScope.SCHOOL
          and b.defaultBoard = true
          and b.school.id = :schoolId
          and p.user.id not in (
              select ub.blocked.id from com.shu.backend.domain.block.entity.UserBlock ub
              where ub.blocker.id = :currentUserId
          )
        order by p.createdAt desc, p.id desc
    """)
    List<Object[]> findAllPostRowsBySchool(
            @Param("schoolId") Long schoolId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );
}
