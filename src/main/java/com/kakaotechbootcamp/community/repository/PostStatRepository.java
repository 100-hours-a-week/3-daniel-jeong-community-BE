package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.PostStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * PostStat JPA Repository
 * - 의도: 통계 조회, 조회수, 좋아요, 댓글 수 증가, 감소 메서드 제공
 */
public interface PostStatRepository extends JpaRepository<PostStat, Integer> {
    @Modifying
    @Query("update PostStat ps set ps.viewCount = ps.viewCount + 1 where ps.id = :postId")
    int incrementViewCount(@Param("postId") Integer postId);

    @Modifying
    @Query("update PostStat ps set ps.likeCount = ps.likeCount + 1 where ps.id = :postId")
    int incrementLikeCount(@Param("postId") Integer postId);

    @Modifying
    @Query("update PostStat ps set ps.likeCount = case when ps.likeCount > 0 then ps.likeCount - 1 else 0 end where ps.id = :postId")
    int decrementLikeCount(@Param("postId") Integer postId);

    @Modifying
    @Query("update PostStat ps set ps.commentCount = ps.commentCount + 1 where ps.id = :postId")
    int incrementCommentCount(@Param("postId") Integer postId);

    @Modifying
    @Query("update PostStat ps set ps.commentCount = case when ps.commentCount > 0 then ps.commentCount - 1 else 0 end where ps.id = :postId")
    int decrementCommentCount(@Param("postId") Integer postId);
}
