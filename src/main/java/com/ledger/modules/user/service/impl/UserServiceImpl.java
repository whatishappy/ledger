package com.ledger.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.constant.CacheConstants;
import com.ledger.common.enums.UserStatusEnum;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.user.dto.LoginVO;
import com.ledger.modules.user.dto.RegisterRequest;
import com.ledger.modules.user.dto.UserInfoVO;
import com.ledger.modules.user.entity.User;
import com.ledger.modules.user.mapper.UserMapper;
import com.ledger.modules.user.service.IUserService;
import com.ledger.security.JwtUtils;
import com.ledger.security.TokenVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenVersionService tokenVersionService;
    private final RedissonClient redissonClient;

    private static final String DELETED_NICKNAME = "已注销用户";
    private static final String DELETED_USERNAME_PREFIX = "user_del_";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 步骤2：查询数据库确认用户名是否已存在
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }
        // 步骤4~5：构建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setTokenVersion(1);
        user.setStatus(UserStatusEnum.NORMAL.getValue());
        // 步骤6：保存用户记录
        userMapper.insert(user);
        log.info("用户注册成功: username={}", request.getUsername());
    }

    @Override
    public LoginVO login(String username, String password) {
        // 步骤2：根据用户名查询用户记录
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        // 步骤3：用户不存在 → 返回1002（防撞库）
        // 步骤4~5：密码不匹配或用户已注销 → 返回1002
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (UserStatusEnum.isDeleted(user.getStatus())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        // 步骤6~8：生成 Access Token 和 Refresh Token
        Integer tokenVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion();
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), tokenVersion);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), tokenVersion);
        UserInfoVO userInfo = new UserInfoVO(user.getId(), user.getUsername(), user.getNickname(), user.getStatus());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return new LoginVO(accessToken, refreshToken, userInfo);
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return new UserInfoVO(user.getId(), user.getUsername(), user.getNickname(), user.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAccount(Long userId, String password) {
        // 步骤2：查询用户当前密码密文和状态
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (UserStatusEnum.isDeleted(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 步骤3~4：校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        // 步骤5：更新用户记录：用户名脱敏、昵称变更、状态置为已注销、token_version自增
        String deletedUsername = DELETED_USERNAME_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        User update = new User();
        update.setId(userId);
        update.setUsername(deletedUsername);
        update.setNickname(DELETED_NICKNAME);
        update.setStatus(UserStatusEnum.DELETED.getValue());
        userMapper.updateById(update);
        // token_version 自增（使所有旧Token失效）
        tokenVersionService.incrementTokenVersion(userId);
        // 步骤6：清除该用户所有缓存Key
        clearUserCache(userId);
        log.info("用户账号注销成功: userId={}", userId);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        // 步骤3：验证 Refresh Token 签名和有效期
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED, "Refresh Token无效或已过期");
        }
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token类型错误");
        }
        // 步骤4：从 Refresh Token 提取 userId 和 tokenVersion
        Long userId = jwtUtils.extractUserId(refreshToken);
        Integer tokenVersion = jwtUtils.extractTokenVersion(refreshToken);
        // 步骤5：验证版本号一致
        Integer latestVersion = tokenVersionService.getTokenVersion(userId);
        if (latestVersion == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!latestVersion.equals(tokenVersion)) {
            throw new BusinessException(ResultCode.TOKEN_VERSION_MISMATCH);
        }
        // 步骤6：生成新的 Access Token
        User user = userMapper.selectById(userId);
        if (user == null || UserStatusEnum.isDeleted(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return jwtUtils.generateAccessToken(user.getId(), user.getUsername(), latestVersion);
    }

    /**
     * 清除用户所有缓存 Key
     */
    private void clearUserCache(Long userId) {
        try {
            RKeys keys = redissonClient.getKeys();
            // 清除 user 缓存
            keys.delete(CacheConstants.buildUserKey(userId));
            // 清除 dashboard、budget 相关缓存（按月份的）
            keys.deleteByPattern("dashboard:" + userId + ":*");
            keys.deleteByPattern("budget:" + userId + ":*");
            log.info("已清除用户全部缓存: userId={}", userId);
        } catch (Exception e) {
            log.warn("清除用户缓存失败: userId={}", userId, e);
        }
    }
}
