package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Comment JPA Repository
 * - 의도: 게시글 기준 댓글 정렬 조회 제공
 */
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // 게시글 상세/수정 응답용: 작성일 오름차순 정렬 (작성자 미포함)
    List<Comment> findByPostIdOrderByCreatedAtAsc(Integer postId);

    // 게시글 상세 조회용: 댓글 + 작성자 fetch join (N+1 방지)
    @Query("select c from Comment c join fetch c.user u where c.post.id = :postId order by c.createdAt asc")
    List<Comment> findByPostIdOrderByCreatedAtAscWithUser(Integer postId);

    // 통계 동기화용 카운트
    int countByPostId(Integer postId);

    // 페이징: 게시글 기준 생성일 오름차순
    @EntityGraph(attributePaths = "user") // 작성자 로딩(N+1 방지)
    Page<Comment> findAllByPostId(Integer postId, Pageable pageable);
}
