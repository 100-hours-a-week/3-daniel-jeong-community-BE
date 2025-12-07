package com.kakaotechbootcamp.community.repository;

import com.kakaotechbootcamp.community.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    void deleteByUserId(Long userId);
}
