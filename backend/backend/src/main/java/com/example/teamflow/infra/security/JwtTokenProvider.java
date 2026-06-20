package com.example.teamflow.infra.security;

import com.example.teamflow.common.enums.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry:3600000}") long accessTokenExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiry = accessTokenExpiry;
    }

    public String generateToken(Long memberId, MemberRole role, Long workspaceId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .claim("wid", workspaceId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    /** @deprecated 워크스페이스 ID 없이 생성 — 하위 호환용 */
    @Deprecated
    public String generateToken(Long memberId, MemberRole role) {
        return generateToken(memberId, role, null);
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getMemberId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long getWorkspaceId(String token) {
        Object wid = getClaims(token).get("wid");
        if (wid == null) return null;
        if (wid instanceof Long l) return l;
        if (wid instanceof Integer i) return i.longValue();
        return Long.parseLong(wid.toString());
    }
}
