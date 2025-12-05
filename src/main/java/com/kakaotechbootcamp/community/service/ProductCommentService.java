package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.product.ProductCommentRequestDto;
import com.kakaotechbootcamp.community.dto.product.ProductCommentResponseDto;
import com.kakaotechbootcamp.community.entity.Product;
import com.kakaotechbootcamp.community.entity.ProductComment;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.ProductCommentRepository;
import com.kakaotechbootcamp.community.repository.ProductRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중고거래 상품 댓글 도메인 서비스
 * - 목록/생성/수정/삭제 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCommentService {

    private final ProductCommentRepository productCommentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * 상품 댓글 목록 조회 (생성일 오름차순, 페이지네이션)
     */
    public ApiResponse<Page<ProductCommentResponseDto>> listByProduct(Integer productId, Integer page, Integer size) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("유효한 상품 ID가 필요합니다");
        }

        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("상품을 찾을 수 없습니다");
        }

        int p = (page == null) ? 0 : Math.max(0, page);
        int requested = (size == null || size <= 0) ? 10 : size;
        int pageSize = Math.min(requested, 20);

        var sorts = new java.util.ArrayList<Sort.Order>();
        sorts.add(Sort.Order.asc("createdAt"));
        Pageable pageable = PageRequest.of(p, pageSize, Sort.by(sorts));

        Page<ProductCommentResponseDto> result = productCommentRepository
                .findAllByProductId(productId, pageable)
                .map(ProductCommentResponseDto::from);

        return ApiResponse.success(result);
    }

    /**
     * 상품 댓글 생성 (대댓글 포함)
     */
    @Transactional
    public ApiResponse<ProductCommentResponseDto> create(Integer productId, Integer userId, ProductCommentRequestDto request) {
        if (productId == null || productId <= 0 || userId == null || userId <= 0) {
            throw new BadRequestException("유효한 ID가 필요합니다");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("댓글 내용을 입력해주세요");
        }

        Integer parentId = null;
        int depth = 0;

        if (request.getParentId() != null) {
            ProductComment parent = productCommentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException("부모 댓글을 찾을 수 없습니다"));

            if (!parent.getProduct().getId().equals(productId)) {
                throw new BadRequestException("부모 댓글이 해당 상품에 속하지 않습니다");
            }
            if (parent.getDeletedAt() != null) {
                throw new BadRequestException("삭제된 댓글에는 답글을 달 수 없습니다");
            }
            if (parent.getDepth() >= 1) {
                throw new BadRequestException("대댓글의 하위에는 더 이상 답글을 달 수 없습니다");
            }

            parentId = parent.getId();
            depth = parent.getDepth() + 1;
        }

        ProductComment saved = productCommentRepository.save(
                new ProductComment(product, user, parentId, request.getContent().trim(), depth)
        );

        return ApiResponse.created(ProductCommentResponseDto.from(saved));
    }

    /**
     * 상품 댓글 수정
     */
    @Transactional
    public ApiResponse<ProductCommentResponseDto> update(Integer commentId, String content) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("유효한 댓글 ID가 필요합니다");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("댓글 내용을 입력해주세요");
        }

        ProductComment comment = productCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다"));

        comment.updateContent(content.trim());
        ProductComment saved = productCommentRepository.save(comment);

        return ApiResponse.modified(ProductCommentResponseDto.from(saved));
    }

    /**
     * 상품 댓글 삭제
     */
    @Transactional
    public ApiResponse<Void> delete(Integer commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("유효한 댓글 ID가 필요합니다");
        }

        ProductComment comment = productCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다"));

        comment.updateContent("삭제된 댓글입니다");
        comment.softDelete();

        return ApiResponse.deleted(null);
    }
}


