package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.Product;
import com.kakaotechbootcamp.community.entity.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Product JPA Repository
 * - 의도: 커서 기반 목록 조회 시 판매자 정보까지 함께 로딩하는 쿼리 제공
 * - @Query 사용 이유: join fetch로 N+1 문제 방지, 커서 기반 페이지네이션 지원
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {

    /**
     * 커서 이후 페이지 조회 (id 내림차순, 판매자 정보 함께 로딩)
     */
    @Query("select p from Product p join fetch p.user u where p.id < :cursor order by p.id desc")
    List<Product> findPageByCursorWithUser(@Param("cursor") Integer cursor, Pageable pageable);

    /**
     * 첫 페이지 조회 (id 내림차순, 판매자 정보 함께 로딩)
     */
    @Query("select p from Product p join fetch p.user u order by p.id desc")
    List<Product> findFirstPageWithUser(Pageable pageable);

    /**
     * 카테고리별 커서 기반 조회
     */
    @Query("select p from Product p join fetch p.user u where p.category = :category and p.id < :cursor order by p.id desc")
    List<Product> findByCategoryAndCursor(@Param("category") String category, @Param("cursor") Integer cursor, Pageable pageable);

    /**
     * 카테고리별 첫 페이지 조회
     */
    @Query("select p from Product p join fetch p.user u where p.category = :category order by p.id desc")
    List<Product> findFirstPageByCategory(@Param("category") String category, Pageable pageable);

    /**
     * 상태별 커서 기반 조회
     */
    @Query("select p from Product p join fetch p.user u where p.status = :status and p.id < :cursor order by p.id desc")
    List<Product> findByStatusAndCursor(@Param("status") ProductStatus status, @Param("cursor") Integer cursor, Pageable pageable);

    /**
     * 상태별 첫 페이지 조회
     */
    @Query("select p from Product p join fetch p.user u where p.status = :status order by p.id desc")
    List<Product> findFirstPageByStatus(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * 검색어 기반 조회 (제목 포함)
     */
    @Query("select p from Product p join fetch p.user u where p.title like %:keyword% order by p.id desc")
    List<Product> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);
}


