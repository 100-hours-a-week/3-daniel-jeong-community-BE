package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.post.PostStatResponseDto;
import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.PostStat;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.CommentRepository;
import com.kakaotechbootcamp.community.repository.PostLikeRepository;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.PostStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostStatService {

    private final PostRepository postRepository;
    private final PostStatRepository postStatRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    public ApiResponse<PostStatResponseDto> getByPostId(Integer postId) {
        PostStat stat = postStatRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("통계를 찾을 수 없습니다"));
        return ApiResponse.modified(PostStatResponseDto.from(stat));
    }

    @Transactional
    public ApiResponse<PostStatResponseDto> syncAll(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다"));

        PostStat stat = postStatRepository.findById(postId).orElseGet(() -> new PostStat(post));

        long likeCount = postLikeRepository.countByPostId(postId);
        long commentCount = commentRepository.countByPostId(postId);

        stat.syncLikeCount((int) likeCount);
        stat.syncCommentCount((int) commentCount);

        PostStat saved = postStatRepository.save(stat);
        return ApiResponse.modified(PostStatResponseDto.from(saved));
    }
}
