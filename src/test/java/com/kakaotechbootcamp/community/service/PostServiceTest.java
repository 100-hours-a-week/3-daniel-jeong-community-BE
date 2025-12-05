package com.kakaotechbootcamp.community.service;

import com.kakaotechbootcamp.community.common.ApiResponse;
import com.kakaotechbootcamp.community.dto.post.PostDetailDto;
import com.kakaotechbootcamp.community.dto.post.PostResponseDto;
import com.kakaotechbootcamp.community.entity.Post;
import com.kakaotechbootcamp.community.entity.PostStat;
import com.kakaotechbootcamp.community.entity.User;
import com.kakaotechbootcamp.community.exception.NotFoundException;
import com.kakaotechbootcamp.community.repository.PostRepository;
import com.kakaotechbootcamp.community.repository.PostStatRepository;
import com.kakaotechbootcamp.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostService 테스트
 * - 게시글 목록 조회, 상세 조회, 삭제 등 게시글 관련 비즈니스 로직 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostStatRepository postStatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("게시글 목록 조회 - 첫 페이지")
    void list_whenNoCursor_returnsFirstPage() {
        User user = createUser("author@example.com", "author");
        createPost(user, "Title 1", "Content 1");
        createPost(user, "Title 2", "Content 2");

        ApiResponse<PostResponseDto> response = postService.list(null, 10, null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().items().size());
    }

    @Test
    @DisplayName("게시글 목록 조회 - 커서 페이지네이션")
    void list_whenCursorProvided_returnsNextPage() {
        User user = createUser("author2@example.com", "author2");
        createPost(user, "Title 1", "Content 1");
        createPost(user, "Title 2", "Content 2");
        createPost(user, "Title 3", "Content 3");

        ApiResponse<PostResponseDto> firstPage = postService.list(null, 2, null);
        Integer cursor = firstPage.getData().nextCursor();

        assertTrue(firstPage.isSuccess());
        assertNotNull(cursor);
        assertEquals(2, firstPage.getData().items().size());

        ApiResponse<PostResponseDto> secondPage = postService.list(cursor, 2, null);

        assertTrue(secondPage.isSuccess());
        assertNotNull(secondPage.getData());
        assertEquals(1, secondPage.getData().items().size());
    }

    @Test
    @DisplayName("게시글 목록 조회 - 빈 목록")
    void list_whenNoPosts_returnsEmptyList() {
        ApiResponse<PostResponseDto> response = postService.list(null, 10, null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().items().isEmpty());
        assertNull(response.getData().nextCursor());
        assertFalse(response.getData().hasNext());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 정상 케이스")
    void getDetail_whenPostExists_returnsPostDetail() {
        User user = createUser("author@example.com", "author");
        Post post = createPost(user, "Test Title", "Test Content");

        ApiResponse<PostDetailDto> response = postService.getDetail(post.getId(), null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Test Title", response.getData().title());
        assertEquals("Test Content", response.getData().content());
    }

    @Test
    @DisplayName("게시글 상세 조회 - 존재하지 않는 게시글")
    void getDetail_whenPostNotExists_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> postService.getDetail(999, null));
    }

    @Test
    @DisplayName("게시글 삭제 - 소프트 삭제")
    void delete_whenPostExists_softDeletesPost() {
        User user = createUser("author@example.com", "author");
        Post post = createPost(user, "Test Title", "Test Content");

        ApiResponse<?> response = postService.delete(post.getId());

        assertTrue(response.isSuccess());
        ApiResponse<PostResponseDto> listResponse = postService.list(null, 10, null);
        assertTrue(listResponse.isSuccess());
        assertTrue(listResponse.getData().items().stream()
                .noneMatch(item -> item.postId().equals(post.getId())));
    }

    @Test
    @DisplayName("게시글 삭제 - 존재하지 않는 게시글")
    void delete_whenPostNotExists_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> postService.delete(999));
    }

    // 테스트용 사용자 생성 및 저장
    private User createUser(String email, String nickname) {
        User user = new User(email, passwordEncoder.encode("testPassword123!"), nickname);
        return userRepository.save(user);
    }

    // 테스트용 게시글 생성 및 저장
    private Post createPost(User user, String title, String content) {
        Post post = new Post(user, title, content);
        Post saved = postRepository.save(post);
        postStatRepository.save(new PostStat(saved));
        return saved;
    }
}