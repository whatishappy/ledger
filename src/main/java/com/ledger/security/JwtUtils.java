package com.ledger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（基于详细设计 §3.2）
 * - Access Token：2小时有效期，Claims: userId/username/tokenVersion
 * - Refresh Token：7天有效期，Claims: userId/tokenVersion
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:7200}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token（有效期2小时）
     */
    public String generateAccessToken(Long userId, String username, Integer tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token（有效期7天）
     */
    public String generateRefreshToken(Long userId, Integer tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token，提取声明信息
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 签名与有效期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 提取用户ID
     */
    public Long extractUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 Token 提取版本号
     */
    public Integer extractTokenVersion(String token) {
        Claims claims = parseToken(token);
        Object version = claims.get(CLAIM_TOKEN_VERSION);
        if (version instanceof Integer) {
            return (Integer) version;
        }
        return ((Number) version).intValue();
    }

    /**
     * 从 Token 提取用户名
     */
    public String extractUsername(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get(CLAIM_USERNAME);
    }

    /**
     * 判断是否为 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为 Access Token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE));
        } catch (Exception e) {
            return false;
        }
    }
}
