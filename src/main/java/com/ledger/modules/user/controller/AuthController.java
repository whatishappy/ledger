package com.ledger.modules.user.controller;

import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.Result;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器（Token 刷新）
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "Access Token 刷新")
public class AuthController {

    private final IUserService userService;

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    /**
     * 刷新 Access Token（U-04）
     * 从 HttpOnly Cookie 中读取 Refresh Token，校验后签发新的 Access Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Access Token")
    public Result<String> refreshAccessToken(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED, "Refresh Token不存在");
        }
        String newAccessToken = userService.refreshAccessToken(refreshToken);
        return Result.success(newAccessToken);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
