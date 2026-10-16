package com.lemon.erp.framework.security;

import com.lemon.erp.framework.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param tokenId 会话 ID，写入 jti，服务端据此判断 token 是否仍然有效
     * @param expire  有效期（参数「登录有效期」）
     */
    public String createToken(String tokenId, Long userId, String username, Duration expire) {
        Date now = new Date();
        return Jwts.builder()
                .id(tokenId)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，返回会话 ID（jti）；token 无效或过期时返回 null
     */
    public String parseTokenId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.getId();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
