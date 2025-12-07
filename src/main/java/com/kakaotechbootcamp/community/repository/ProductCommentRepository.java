package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.ProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCommentRepository extends JpaRepository<ProductComment, Integer> {

    Page<ProductComment> findAllByProductId(Integer productId, Pageable pageable);
}
