package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Post JPA Repository
 * - 의도: 커서 기반 목록 조회 시 작성자 정보까지 함께 로딩하는 쿼리 제공
 */
public interface PostRepository extends JpaRepository<Post, Integer> {

    /**
     * 커서 이후 페이지 조회 (id 내림차순)
     * - 파라미터: cursor 미포함, size(Pageable)
     */
    @Query("select p from Post p join fetch p.user u where p.id < :cursor order by p.id desc")
    List<Post> findPageByCursorWithUser(@Param("cursor") Integer cursor, Pageable pageable);

    /**
     * 첫 페이지 조회 (id 내림차순)
     */
    @Query("select p from Post p join fetch p.user u order by p.id desc")
    List<Post> findFirstPageWithUser(Pageable pageable);
}
