package com.kakaotechbootcamp.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 중고거래 상품 이미지
 */
@Entity
@Getter
@Table(name = "product_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "object_key", length = 1024, nullable = false)
    private String objectKey;

    // 이미지 순서
    @Column(name = "display_order")
    private Integer displayOrder;

    public ProductImage(Product product, String objectKey, Integer displayOrder) {
        this.product = product;
        this.objectKey = objectKey;
        this.displayOrder = displayOrder;
    }
}
