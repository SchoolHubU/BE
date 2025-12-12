package com.shu.backend.global.jwt;

import com.shu.backend.global.apiPayload.code.status.ErrorStatus;
import com.shu.backend.global.exception.GeneralException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;  // application.yml의 jwt 설정값

    private Key key; // JWT 서명에 사용할 key

    @PostConstruct
    public void init() {
        // yml에 있는 secret 문자열을 HMAC-SHA256 키 객체로 변환
        // (토큰 생성·검증 시 서명으로 사용)
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * AccessToken 생성
     * - userId 를 subject(주체)에 담음
     * - 발급 시각 + 만료시간 설정
     * - HS256 알고리즘과 secret key로 서명
     */
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 토큰 소유자 정보
                .setIssuedAt(now)                   // 발급 시간
                .setExpiration(expiry)              // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명
                .compact();                         // 최종 문자열 형태로 변환
    }

    /**
     * 토큰에서 userId(subject) 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 토큰 유효성 검증
     * - 서명 불일치, 만료, 형식 오류 등의 예외를 잡아서
     *   프로젝트 내 ErrorStatus로 변환해 던짐
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            throw new GeneralException(ErrorStatus.MALFORMED_JWT_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.EMPTY_JWT_CLAIMS);
        } catch (JwtException e) {
            // 서명 불일치 등 기타 JWT 관련 오류
            throw new GeneralException(ErrorStatus.INVALID_JWT_SIGNATURE);
        }
    }

    /**
     * 실제 Claims 파싱
     * - parserBuilder().setSigningKey() 로 서명 검증
     * - parseClaimsJws(token) 이 내부적으로 서명, 만료 등을 체크함
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)      // 서명 검증에 사용할 key
                .build()
                .parseClaimsJws(token)   // JWS 파싱 및 검증
                .getBody();              // payload (claims) 추출
    }
}
