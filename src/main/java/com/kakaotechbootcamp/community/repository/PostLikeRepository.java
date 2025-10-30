package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.PostLike;
import com.kakaotechbootcamp.community.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PostLike JPA Repository
 * - 의도: 좋아요 토글/통계 계산에 필요한 최소 메서드 제공
 */
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    /**
     * 통계 동기화용: 특정 게시글의 좋아요 수 집계
     */
    int countByPostId(Integer postId);

    /**
     * 멱등 토글용: 사용자가 해당 게시글을 이미 좋아요했는지 여부
     */
    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    /**
     * 편의 메서드: postId, userId로 삭제
     */
    void deleteByPostIdAndUserId(Integer postId, Integer userId);
}
