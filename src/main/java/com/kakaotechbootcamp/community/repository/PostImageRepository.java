package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * PostImage JPA Repository
 * - 의도: 게시글 이미지 정렬 조회 및 일괄 처리 지원
 */
public interface PostImageRepository extends JpaRepository<PostImage, Integer> {
    // 엔티티 레퍼런스가 있는 경우(트랜잭션 내) 정렬 포함 이미지 조회
    List<PostImage> findByPostOrderByDisplayOrderAsc(Post post);

    // 아이디만 알고 있는 경우(트랜잭션 경계 밖 또는 캐시/단순 조회) 정렬 포함 이미지 조회
    List<PostImage> findByPostIdOrderByDisplayOrderAsc(Integer postId);

    // 교체/삭제 편의 메서드
    void deleteByPost(Post post);
    void deleteByPostId(Integer postId);
}
