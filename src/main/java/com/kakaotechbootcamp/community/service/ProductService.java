package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.common.ImageProperties;
import com.kakaotechbootcamp.community.common.ImageType;
import com.kakaotechbootcamp.community.dto.product.*;
import com.kakaotechbootcamp.community.entity.*;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.exception.BadRequestException;
import com.kakaotechbootcamp.community.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 중고거래 상품(Product) 도메인 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;
    private final ImageProperties imageProperties;

    /**
     * 상품 목록 조회(커서 기반)
     */
    public ApiResponse<ProductResponseDto> list(Integer cursor, Integer size, String category, 
                                                 String status, String search) {
        int requested = (size == null) ? 12 : size;
        int pageSize = requested <= 0 ? 12 : Math.min(requested, 50);
        Pageable pageable = PageRequest.of(0, pageSize);

        List<Product> products;
        
        // 카테고리 필터
        if (category != null && !category.equals("all")) {
            if (cursor == null || cursor <= 0) {
                products = productRepository.findFirstPageByCategory(category, pageable);
            } else {
                products = productRepository.findByCategoryAndCursor(category, cursor, pageable);
            }
        }
        // 상태 필터
        else if (status != null && !status.equals("all")) {
            ProductStatus productStatus = ProductStatus.valueOf(status);
            if (cursor == null || cursor <= 0) {
                products = productRepository.findFirstPageByStatus(productStatus, pageable);
            } else {
                products = productRepository.findByStatusAndCursor(productStatus, cursor, pageable);
            }
        }
        // 검색
        else if (search != null && !search.trim().isEmpty()) {
            products = productRepository.findByTitleContaining(search.trim(), pageable);
        }
        // 기본 조회
        else {
            if (cursor == null || cursor <= 0) {
                products = productRepository.findFirstPageWithUser(pageable);
            } else {
                products = productRepository.findPageByCursorWithUser(cursor, pageable);
            }
        }

        // 썸네일 이미지 조회
        Map<Integer, String> productIdToThumbnail = new HashMap<>();
        List<Integer> productIds = products.stream().map(Product::getId).toList();
        
        if (!productIds.isEmpty()) {
            List<ProductImage> thumbnails = productImageRepository.findFirstByProductIds(productIds);
            for (ProductImage thumbnail : thumbnails) {
                productIdToThumbnail.put(thumbnail.getProduct().getId(), thumbnail.getObjectKey());
            }
        }

        List<ProductListItemDto> items = products.stream()
                .map(p -> ProductListItemDto.from(p, productIdToThumbnail.get(p.getId())))
                .toList();
        
        Integer nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).productId();
        boolean hasNext = items.size() == pageSize;

        return ApiResponse.success(ProductResponseDto.of(items, nextCursor, hasNext));
    }

    /**
     * 상품 상세 조회
     */
    @Transactional
    public ApiResponse<ProductDetailDto> getDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다"));

        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        
        // 조회수 증가
        product.incrementViewCount();

        ProductDetailDto dto = ProductDetailDto.from(product, images);
        return ApiResponse.success(dto);
    }

    /**
     * 상품 생성
     */
    @Transactional
    public ApiResponse<ProductDetailDto> create(ProductCreateRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        ProductStatus status = ProductStatus.SELLING;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            status = ProductStatus.valueOf(request.getStatus());
        }

        Product product = new Product(
                user,
                request.getTitle(),
                request.getContent(),
                request.getPrice(),
                request.getCategory(),
                request.getLocation(),
                status
        );

        Product saved = productRepository.save(product);

        // 이미지 저장
        if (request.getImageObjectKeys() != null && !request.getImageObjectKeys().isEmpty()) {
            if (request.getImageObjectKeys().size() > imageProperties.getMaxPerPost()) {
                throw new BadRequestException("이미지 최대 업로드 개수는 " + imageProperties.getMaxPerPost() + "개 입니다");
            }
            // 이미지 objectKey 검증
            List<String> keys = request.getImageObjectKeys();
            for (String objectKey : keys) {
                imageUploadService.validateObjectKey(ImageType.PRODUCT, objectKey, saved.getId());
            }
            saveProductImages(saved, keys);
        }

        ProductDetailDto dto = buildProductDetailDto(saved);
        return ApiResponse.created(dto);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public ApiResponse<ProductDetailDto> update(Integer productId, ProductUpdateRequestDto request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다"));

        // 기본 정보 업데이트
        product.updateTitle(request.getTitle());
        product.updateContent(request.getContent());
        product.updatePrice(request.getPrice());
        product.updateCategory(request.getCategory());
        product.updateLocation(request.getLocation());
        
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            product.updateStatus(ProductStatus.valueOf(request.getStatus()));
        }

        // 이미지 전체 교체 정책
        if (request.getImageObjectKeys() != null) {
            List<ProductImage> existing = productImageRepository.findByProductOrderByDisplayOrderAsc(product);
            productImageRepository.deleteAll(existing);
            
            if (!request.getImageObjectKeys().isEmpty()) {
                if (request.getImageObjectKeys().size() > imageProperties.getMaxPerPost()) {
                    throw new BadRequestException("이미지 최대 업로드 개수는 " + imageProperties.getMaxPerPost() + "개 입니다");
                }
                // 이미지 objectKey 검증
                List<String> keys = request.getImageObjectKeys();
                for (String objectKey : keys) {
                    imageUploadService.validateObjectKey(ImageType.PRODUCT, objectKey, productId);
                }
                saveProductImages(product, keys);
            }
        }

        ProductDetailDto dto = buildProductDetailDto(product);
        return ApiResponse.modified(dto);
    }

    /**
     * 상품 상태 변경
     */
    @Transactional
    public ApiResponse<ProductDetailDto> updateStatus(Integer productId, String status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다"));

        ProductStatus newStatus;
        try {
            newStatus = ProductStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("유효하지 않은 상태입니다");
        }

        product.updateStatus(newStatus);

        ProductDetailDto dto = buildProductDetailDto(product);
        return ApiResponse.modified(dto);
    }

    /**
     * 상품 삭제 (Soft Delete)
     */
    @Transactional
    public ApiResponse<Void> delete(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다"));

        product.setDeletedAt(LocalDateTime.now());
        
        return ApiResponse.deleted(null);
    }

    /**
     * 상품 이미지 저장 헬퍼 메서드
     * - 의도: 이미지 순서대로 저장 (displayOrder = index)
     */
    private void saveProductImages(Product product, List<String> imageObjectKeys) {
        if (imageObjectKeys == null || imageObjectKeys.isEmpty()) {
            return;
        }

        List<ProductImage> images = new java.util.ArrayList<>();
        for (int i = 0; i < imageObjectKeys.size(); i++) {
            images.add(new ProductImage(product, imageObjectKeys.get(i), i));
        }
        productImageRepository.saveAll(images);
    }

    /**
     * 상품 상세 DTO 생성 헬퍼 메서드
     */
    private ProductDetailDto buildProductDetailDto(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        return ProductDetailDto.from(product, images);
    }
}

