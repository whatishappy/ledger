package com.ledger.modules.user.service;

import com.ledger.modules.user.dto.LoginVO;
import com.ledger.modules.user.dto.RegisterRequest;
import com.ledger.modules.user.dto.UserInfoVO;

/**
 * 用户服务接口
 */
public interface IUserService {

    /**
     * 用户注册（U-01）
     */
    void register(RegisterRequest request);

    /**
     * 用户登录（U-02），返回 Access Token 和用户信息
     */
    LoginVO login(String username, String password);

    /**
     * 获取用户信息（U-05）
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 账号注销（U-06）：密码校验 → 数据脱敏 → Token失效
     * @param userId 当前用户ID
     * @param password 用户当前密码
     */
    void cancelAccount(Long userId, String password);

    /**
     * 刷新 Access Token（U-04）
     * @param refreshToken Refresh Token
     * @return 新的 Access Token
     */
    String refreshAccessToken(String refreshToken);
}
