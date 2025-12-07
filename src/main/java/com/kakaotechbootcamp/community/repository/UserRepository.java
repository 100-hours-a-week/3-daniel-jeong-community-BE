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

    // 활성 사용자만 조회 (@SQLRestriction 적용)
    Optional<User> findByEmail(String email);
    Optional<User> findById(Integer id);
    boolean existsById(Integer id);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Integer id);

    /**
     * 이메일로 사용자 조회 (삭제된 사용자 포함)
     * - 사용처: UserService (로그인, 회원가입 중복 검사, 이메일 체크)
     */
    @Query(value = "SELECT * FROM `user` WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    /**
     * ID로 사용자 조회 (삭제된 사용자 포함)
     * - 사용처: UserService (복구)
     */
    @Query(value = "SELECT * FROM `user` WHERE user_id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Integer id);

    /**
     * 닉네임 중복 검사 (삭제 대기 중인 계정도 포함)
     * - 사용처: UserService (회원가입, 닉네임 체크)
     */
    @Query(value = "SELECT COUNT(*) FROM `user` WHERE nickname = :nickname", nativeQuery = true)
    Long countByNicknameIncludingDeleted(@Param("nickname") String nickname);
}
