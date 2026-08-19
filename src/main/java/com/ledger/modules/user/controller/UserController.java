package com.ledger.modules.user.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.user.dto.AvatarUploadUrlVO;
import com.ledger.modules.user.dto.LoginRequest;
import com.ledger.modules.user.dto.LoginVO;
import com.ledger.modules.user.dto.RegisterRequest;
import com.ledger.modules.user.dto.UpdateAvatarRequest;
import com.ledger.modules.user.dto.UserInfoVO;
import com.ledger.modules.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "用户注册、登录、登出、信息查询、账号注销、头像管理")
public class UserController {

    private final IUserService userService;

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                 HttpServletResponse response) {
        LoginVO loginVO = userService.login(request.getUsername(), request.getPassword());
        setRefreshTokenCookie(response, loginVO.getRefreshToken());
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoVO> getInfo() {
        Long userId = UserContext.requireUserId();
        return Result.success(userService.getUserInfo(userId));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "账号注销")
    public Result<Void> deleteAccount(@RequestParam @NotBlank String password,
                                       HttpServletResponse response) {
        Long userId = UserContext.requireUserId();
        userService.cancelAccount(userId, password);
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    @GetMapping("/me/avatar-upload-url")
    @Operation(summary = "生成头像上传预签名URL（U-08）")
    public Result<AvatarUploadUrlVO> getAvatarUploadUrl(@RequestParam @NotBlank String fileType) {
        Long userId = UserContext.requireUserId();
        return Result.success(userService.createAvatarUploadUrl(userId, fileType));
    }

    @PutMapping("/me/avatar")
    @Operation(summary = "更新用户头像（U-09）")
    public Result<UserInfoVO> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.success(userService.updateAvatar(userId, request.getObjectKey()));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
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
