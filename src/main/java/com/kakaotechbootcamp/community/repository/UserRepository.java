package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자(User) 리포지토리
 * - 의도: 사용자 관련 중복 검사/조회 질의 집합화
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    // 로그인/인증 등 이메일 기반 단건 조회
	Optional<User> findByEmail(String email);

    // 회원가입 전 이메일 중복 검사
	boolean existsByEmail(String email);

    // 회원가입 전 닉네임 중복 검사
	boolean existsByNickname(String nickname);

    // 닉네임 변경 시 자기 자신 제외 중복 검사
	boolean existsByNicknameAndIdNot(String nickname, Integer id);
}
