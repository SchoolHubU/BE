package com.shu.backend.domain.post.repository;

import com.shu.backend.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Slice<Post> findByBoardId(Long boardId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);
}
