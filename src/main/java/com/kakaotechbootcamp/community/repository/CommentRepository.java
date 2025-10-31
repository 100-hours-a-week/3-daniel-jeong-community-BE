package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Comment JPA Repository
 * - 의도: 게시글 기준 댓글 정렬 조회 제공
 */
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Integer postId);

    @Query("select c from Comment c join fetch c.user u where c.post.id = :postId order by c.createdAt asc")
    List<Comment> findByPostIdOrderByCreatedAtAscWithUser(Integer postId);

    int countByPostId(Integer postId);
}
