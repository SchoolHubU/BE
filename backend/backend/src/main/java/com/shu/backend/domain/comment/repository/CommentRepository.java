package com.shu.backend.domain.comment.repository;

import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);

    int countByPostId(Long postId);

    @Query("""
        select c
        from Comment c
        join fetch c.user u
        where c.post.id = :postId
          and c.parent is null
          and c.commentStatus in :statuses
        order by c.createdAt asc
    """)
    List<Comment> findParentsForPostDetail(
            @Param("postId") Long postId,
            @Param("statuses") List<CommentStatus> statuses
    );

    default List<Comment> findParentsForPostDetail(Long postId) {
        return findParentsForPostDetail(postId, List.of(CommentStatus.ACTIVE, CommentStatus.HIDDEN));
    }

    @Query("""
    select c
    from Comment c
    join fetch c.user u
    where c.parent.id = :parentId
      and c.commentStatus in :statuses
    order by c.createdAt asc
""")
    List<Comment> findReplies(Long parentId, List<CommentStatus> statuses);

}
