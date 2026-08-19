package com.ledger.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /**
     * Access Token（有效期2小时，客户端内存存储）
     */
    private String accessToken;

    /**
     * Refresh Token（有效期7天，写入 HttpOnly Cookie）
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private UserInfoVO userInfo;
}
