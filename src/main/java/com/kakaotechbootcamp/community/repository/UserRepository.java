package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 사용자(User) 리포지토리
 * - 의도: 사용자 관련 중복 검사/조회 질의 집합화
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    // 로그인/인증 등 이메일 기반 단건 조회
	Optional<User> findByEmail(String email);

    /**
     * 이메일로 사용자 조회
     * - 사용처: UserService (로그인, 회원가입 중복 검사, 이메일 체크)
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    /**
     * ID로 사용자 조회
     * - 사용처: UserService (복구)
     */
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdIncludingDeleted(@Param("id") Integer id);

    /**
     * 닉네임 중복 검사
     * - 사용처: UserService (회원가입, 닉네임 체크)
     */
	boolean existsByNickname(String nickname);

    /**
     * 닉네임 중복 검사
     * - 사용처: UserService (닉네임 변경)
     */
	boolean existsByNicknameAndIdNot(String nickname, Integer id);
}
