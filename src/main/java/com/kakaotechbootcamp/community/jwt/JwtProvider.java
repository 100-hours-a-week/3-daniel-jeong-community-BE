package com.kakaotechbootcamp.community.jwt;

import com.kakaotechbootcamp.community.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;

    private Key getKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.getSecretKey())
        );
    }

    public Jws<Claims> parse(String jwt) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(jwt);
    }

    private String createToken(Long userId, long ttlSeconds, Consumer<JwtBuilder> customizer) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(ttlSeconds)))
                .signWith(getKey(), SignatureAlgorithm.HS256);
        
        customizer.accept(builder);
        return builder.compact();
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, jwtProperties.getAccessTokenTtlSeconds(), builder -> {
            builder.claim("role", role);
        });
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, jwtProperties.getRefreshTokenTtlSeconds(), builder -> {
            builder.claim("typ", "refresh")
                    .setId(UUID.randomUUID().toString());
        });
    }
}
