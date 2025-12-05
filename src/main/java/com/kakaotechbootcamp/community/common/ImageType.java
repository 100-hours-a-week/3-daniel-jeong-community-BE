package com.kakaotechbootcamp.community.common;

/**
 * 이미지 타입 enum
 * - 의도: 프로필/게시글 이미지 구분으로 경로/검증/저장 로직 분기
 */
public enum ImageType {
    /**
     * 사용자 프로필 이미지 (단일)
     * - 저장 위치 규칙: user/{userId}/profile/{filename}
     * - 사용: User.profileImageKey
     */
    PROFILE,
    
    /**
     * 게시글 이미지 (다중)
     * - 저장 위치 규칙: post/{postId}/images/{filename}
     * - 사용: PostImage.objectKey
     */
    POST,
    
    /**
     * 중고거래 상품 이미지 (다중)
     * - 저장 위치 규칙: product/{productId}/images/{filename}
     * - 사용: ProductImage.objectKey
     */
    PRODUCT
}
