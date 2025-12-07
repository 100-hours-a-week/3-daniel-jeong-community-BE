package com.kakaotechbootcamp.community.dto.image;

import com.kakaotechbootcamp.community.common.ImageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Presigned URL 요청 DTO
public record PresignedUrlRequestDto(
        @NotNull(message = "imageType은 필수입니다")
        ImageType imageType,
        
        @NotNull(message = "resourceId는 필수입니다")
        Integer resourceId,
        
        @NotBlank(message = "filename은 필수입니다")
        String filename,
        
        @NotBlank(message = "contentType은 필수입니다")
        String contentType
) {
}
