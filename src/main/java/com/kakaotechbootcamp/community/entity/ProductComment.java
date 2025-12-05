package com.kakaotechbootcamp.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.kakaotechbootcamp.community.common.SoftDeletable;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "product_comment", indexes = {
        // 특정 상품의 댓글을 생성일시 기준으로 조회
        @Index(name = "idx_product_comment_product_created_at", columnList = "product_id, created_at"),
        // 특정 상품의 부모 댓글별 대댓글을 생성일시 기준으로 조회
        @Index(name = "idx_product_comment_product_parent_created_at", columnList = "product_id, parent_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public class ProductComment implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_comment_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ProductComment(Product product, User user, Integer parentId, String content, Integer depth) {
        this.product = product;
        this.user = user;
        this.parentId = parentId;
        this.content = content;
        this.depth = depth;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    @Override
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}


