package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.PostLike;
import com.kakaotechbootcamp.community.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * PostLike JPA Repository
 * - 의도: 좋아요 토글/통계 계산에 필요한 최소 메서드 제공
 * - 주의: 복합키(EmbeddedId) 기반 파생 쿼리는 id.postId, id.userId 경로를 사용
 */
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    /**
     * 통계 동기화용: 특정 게시글의 좋아요 수 집계
     */
    int countByIdPostId(Integer postId);

    /**
     * 토글 조회용: 특정 게시글에 대한 사용자가 이미 좋아요했는지 여부
     * - EmbeddedId(postId, userId) 기준 경로 사용
     */
    boolean existsByIdPostIdAndIdUserId(Integer postId, Integer userId);

    /**
     * 편의 메서드: postId, userId로 삭제
     * - EmbeddedId(postId, userId) 기준 경로 사용
     */
    void deleteByIdPostIdAndIdUserId(Integer postId, Integer userId);

    /**
     * 목록 조회용: 특정 사용자가 여러 게시물에 좋아요를 눌렀는지 일괄 조회
     */
    List<PostLike> findByIdPostIdInAndIdUserId(List<Integer> postIds, Integer userId);
}
