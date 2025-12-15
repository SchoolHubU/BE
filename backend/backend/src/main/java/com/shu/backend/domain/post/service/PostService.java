package com.shu.backend.domain.post.service;

import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public Slice<Post> getPostsByBoardId(Long boardId, Pageable pageable) {
        return postRepository.findByBoardId(boardId, pageable);
    }
}
