package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.repository.PostStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostStatAsyncService {

    private final PostStatRepository postStatRepository;

    @Async
    @Transactional
    public void incrementViewCount(Integer postId) {
        postStatRepository.incrementViewCount(postId);
    }

    @Async
    @Transactional
    public void incrementLikeCount(Integer postId) {
        postStatRepository.incrementLikeCount(postId);
    }

    @Async
    @Transactional
    public void decrementLikeCount(Integer postId) {
        postStatRepository.decrementLikeCount(postId);
    }

    @Async
    @Transactional
    public void incrementCommentCount(Integer postId) {
        postStatRepository.incrementCommentCount(postId);
    }

    @Async
    @Transactional
    public void decrementCommentCount(Integer postId) {
        postStatRepository.decrementCommentCount(postId);
    }
}
