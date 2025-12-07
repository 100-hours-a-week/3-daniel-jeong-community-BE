package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.Product;
import com.kakaotechbootcamp.community.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ProductImage JPA Repository
 * - 의도: 상품 이미지 정렬 조회 및 일괄 처리 지원
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    /**
     * 엔티티 레퍼런스가 있는 경우(트랜잭션 내) 정렬 포함 이미지 조회
     */
    List<ProductImage> findByProductOrderByDisplayOrderAsc(Product product);

    /**
     * 아이디만 알고 있는 경우(트랜잭션 경계 밖 또는 캐시/단순 조회) 정렬 포함 이미지 조회
     */
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Integer productId);

    /**
     * 상품 ID로 모든 이미지 삭제
     */
    @Modifying
    @Query("delete from ProductImage pi where pi.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    /**
     * 여러 상품 ID로 첫 번째 이미지 조회 (썸네일용, 배치 조회)
     * - 각 상품별로 displayOrder가 가장 작은 이미지만 반환
     */
    @Query("select pi from ProductImage pi " +
           "where pi.product.id in :productIds " +
           "and pi.displayOrder = (select min(pi2.displayOrder) from ProductImage pi2 where pi2.product.id = pi.product.id)")
    List<ProductImage> findFirstByProductIds(@Param("productIds") List<Integer> productIds);

    /**
     * 상품의 모든 이미지 삭제
     */
    void deleteByProduct(Product product);
}
