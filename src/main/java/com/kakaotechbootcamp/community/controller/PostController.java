package com.kakaotechbootcamp.community.controller;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.post.PostCreateRequestDto;
import com.kakaotechbootcamp.community.dto.post.PostDetailDto;
import com.kakaotechbootcamp.community.dto.post.PostResponseDto;
import com.kakaotechbootcamp.community.dto.post.PostUpdateRequestDto;
import com.kakaotechbootcamp.community.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

/**
 * 게시글(Post) API 컨트롤러
 * - 목록 조회(커서 기반), 상세 조회, 생성, 수정, 삭제
 * @RestController: @Controller + @ResponseBody
 * @RequestMapping: 기본 경로 /posts
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 목록 조회 (커서 기반 페이지네이션)
     * - 의도: 최신순(id desc)으로 10개 기본, cursor 기준 이후 데이터 반환
     * - 파라미터: cursor(마지막 항목 id), size(페이지 크기)
     * - 응답: items, nextCursor(null이면 끝), hasNext
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PostResponseDto>> list(
            @RequestParam(value = "cursor", required = false) Integer cursor,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestAttribute(value = "userId", required = false) Integer userId
    ) {
        ApiResponse<PostResponseDto> response = postService.list(cursor, size, userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 게시글 상세 조회
     * - 의도: 본문/작성자/이미지/통계/댓글 반환, 조회수 +1
     * - 에러: 존재하지 않으면 404(NotFound)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailDto>> getDetail(
            @PathVariable("id") Integer id,
            @RequestAttribute(value = "userId", required = false) Integer userId
    ) {
        ApiResponse<PostDetailDto> response = postService.getDetail(id, userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 게시글 생성
     * - 의도: 작성자/제목/내용/이미지 키 배열로 게시글 작성
     * - 에러: 작성자 미존재 시 404(NotFound), 유효성 위반 시 400
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailDto>> create(@Valid @RequestBody PostCreateRequestDto request,
                                                             @RequestAttribute(value = "userId", required = false) Integer userId) {
        ApiResponse<PostDetailDto> response = postService.create(request, userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 게시글 부분 수정
     * - 의도: 제목/내용 선택 수정, 이미지 배열 전달 시 전체 교체
     * - 정책: null=미변경, 빈 배열=[]=이미지 전부 제거
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailDto>> update(
            @PathVariable("id") Integer id,
            @Valid @RequestBody PostUpdateRequestDto request,
            @RequestAttribute(value = "userId", required = false) Integer userId
    ) {
        ApiResponse<PostDetailDto> response = postService.update(id, request, userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * 게시글 삭제(소프트 삭제)
     * - 의도: deletedAt 설정으로 비활성화
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Integer id) {
        ApiResponse<Void> response = postService.delete(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}


