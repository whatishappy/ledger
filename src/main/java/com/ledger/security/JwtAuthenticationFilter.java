package com.ledger.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * - 解析 Authorization: Bearer → 校验签名&有效期 → 提取 tokenVersion → 比对最新版本号
 * - 一致：UserContext.setUserId(userId)，放行
 * - 不一致：返回 1004 TOKEN_VERSION_MISMATCH
 * - finally 块强制 UserContext.clear()（防 ThreadLocal 泄露）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenVersionService tokenVersionService;
    private final ObjectMapper objectMapper;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 校验 Token 签名与有效期
            if (!jwtUtils.validateToken(token)) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED, "Token无效或已过期");
                return;
            }

            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Number.class).longValue();
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);

            // 比对最新版本号
            Integer latestVersion = tokenVersionService.getTokenVersion(userId);
            if (latestVersion == null) {
                writeUnauthorized(response, ResultCode.USER_NOT_FOUND, "用户不存在");
                return;
            }
            if (!latestVersion.equals(tokenVersion)) {
                writeUnauthorized(response, ResultCode.TOKEN_VERSION_MISMATCH, "Token已失效，请重新登录");
                return;
            }

            // 设置 Spring Security 认证上下文（供授权检查）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 设置业务层用户上下文
            UserContext.setUserId(userId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT 认证异常", e);
            writeUnauthorized(response, ResultCode.UNAUTHORIZED, "认证失败");
        } finally {
            // 强制清理 ThreadLocal，防止内存泄露
            UserContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> result = Result.fail(resultCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 公开路径不过滤
        return path.startsWith("/api/user/register")
                || path.startsWith("/api/user/login")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/doc.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars/")
                || path.startsWith("/actuator")
                || path.startsWith("/favicon.ico")
                || path.startsWith("/swagger-resources")
                // 错误页 dispatch：SSE 响应已提交后 error 转发无需重复鉴权
                || path.startsWith("/error");
    }
}
