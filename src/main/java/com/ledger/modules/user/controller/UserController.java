package com.ledger.modules.user.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.user.dto.LoginRequest;
import com.ledger.modules.user.dto.LoginVO;
import com.ledger.modules.user.dto.RegisterRequest;
import com.ledger.modules.user.dto.UserInfoVO;
import com.ledger.modules.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "用户注册、登录、登出、信息查询、账号注销")
public class UserController {

    private final IUserService userService;

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7天（秒）

    /**
     * 用户注册（U-01）
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    /**
     * 用户登录（U-02）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                 HttpServletResponse response) {
        LoginVO loginVO = userService.login(request.getUsername(), request.getPassword());
        // 将 Refresh Token 写入 HttpOnly Cookie
        setRefreshTokenCookie(response, loginVO.getRefreshToken());
        return Result.success(loginVO);
    }

    /**
     * 用户登出（U-03）
     * 客户端清除Token，服务端无状态（版本号方案）
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    /**
     * 获取当前用户信息（U-05）
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> getInfo() {
        Long userId = UserContext.requireUserId();
        return Result.success(userService.getUserInfo(userId));
    }

    /**
     * 账号注销（U-06）
     */
    @DeleteMapping("/delete")
    @Operation(summary = "账号注销")
    public Result<Void> deleteAccount(@RequestParam @NotBlank String password,
                                       HttpServletResponse response) {
        Long userId = UserContext.requireUserId();
        userService.cancelAccount(userId, password);
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 生产环境通过Nginx启用HTTPS后设为true
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
