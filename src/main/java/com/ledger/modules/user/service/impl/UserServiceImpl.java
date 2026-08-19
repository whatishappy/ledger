package com.ledger.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.constant.CacheConstants;
import com.ledger.common.enums.UserStatusEnum;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.config.minio.MinioProperties;
import com.ledger.modules.user.dto.AvatarUploadUrlVO;
import com.ledger.modules.user.dto.LoginVO;
import com.ledger.modules.user.dto.RegisterRequest;
import com.ledger.modules.user.dto.UserInfoVO;
import com.ledger.modules.user.entity.User;
import com.ledger.modules.user.mapper.UserMapper;
import com.ledger.modules.user.service.IUserService;
import com.ledger.security.JwtUtils;
import com.ledger.security.TokenVersionService;
import com.ledger.service.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenVersionService tokenVersionService;
    private final RedissonClient redissonClient;
    private final MinioStorageService minioStorageService;
    private final MinioProperties minioProperties;

    private static final String DELETED_NICKNAME = "已注销用户";
    private static final String DELETED_USERNAME_PREFIX = "user_del_";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("jpg", "jpeg", "png");
    private static final String LOGIN_FAIL_COUNT_FIELD = "count";
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setTokenVersion(1);
        user.setStatus(UserStatusEnum.NORMAL.getValue());
        userMapper.insert(user);
        log.info("用户注册成功: username={}", request.getUsername());
    }

    @Override
    public LoginVO login(String username, String password) {
        String loginFailKey = CacheConstants.buildLoginFailKey(username);
        RMapCache<String, Integer> failMap = redissonClient.getMapCache(loginFailKey);
        Integer failCount = failMap.get(LOGIN_FAIL_COUNT_FIELD);
        if (failCount != null && failCount >= MAX_LOGIN_FAIL_COUNT) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "登录失败次数过多，请15分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        boolean loginSuccess = true;
        if (user == null) {
            loginSuccess = false;
        } else if (UserStatusEnum.isDeleted(user.getStatus())) {
            loginSuccess = false;
        } else if (!passwordEncoder.matches(password, user.getPassword())) {
            loginSuccess = false;
        }

        if (!loginSuccess) {
            try {
                int newCount = failCount == null ? 1 : failCount + 1;
                failMap.put(LOGIN_FAIL_COUNT_FIELD, newCount,
                        CacheConstants.LOGIN_FAIL_TTL, TimeUnit.MILLISECONDS);
                log.warn("用户登录失败: username={}, failCount={}", username, newCount);
            } catch (Exception e) {
                log.warn("记录登录失败计数异常: username={}", username, e);
            }
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        try {
            RKeys keys = redissonClient.getKeys();
            keys.delete(loginFailKey);
        } catch (Exception e) {
            log.warn("清除登录失败计数异常: username={}", username, e);
        }

        Integer tokenVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion();
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), tokenVersion);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), tokenVersion);
        UserInfoVO userInfo = buildUserInfoVO(user);
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return new LoginVO(accessToken, refreshToken, userInfo);
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return buildUserInfoVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAccount(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (UserStatusEnum.isDeleted(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        String deletedUsername = DELETED_USERNAME_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        User update = new User();
        update.setId(userId);
        update.setUsername(deletedUsername);
        update.setNickname(DELETED_NICKNAME);
        update.setStatus(UserStatusEnum.DELETED.getValue());
        userMapper.updateById(update);
        tokenVersionService.incrementTokenVersion(userId);
        clearUserCache(userId);
        log.info("用户账号注销成功: userId={}", userId);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED, "Refresh Token无效或已过期");
        }
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token类型错误");
        }
        Long userId = jwtUtils.extractUserId(refreshToken);
        Integer tokenVersion = jwtUtils.extractTokenVersion(refreshToken);
        Integer latestVersion = tokenVersionService.getTokenVersion(userId);
        if (latestVersion == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!latestVersion.equals(tokenVersion)) {
            throw new BusinessException(ResultCode.TOKEN_VERSION_MISMATCH);
        }
        User user = userMapper.selectById(userId);
        if (user == null || UserStatusEnum.isDeleted(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return jwtUtils.generateAccessToken(user.getId(), user.getUsername(), latestVersion);
    }

    @Override
    public AvatarUploadUrlVO createAvatarUploadUrl(Long userId, String fileType) {
        if (fileType == null || fileType.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "fileType不能为空");
        }
        String ext = fileType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(ext)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "仅支持jpg/jpeg/png格式");
        }
        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID() + "." + ext;
        String uploadUrl = minioStorageService.generateUploadPresignedUrl(objectKey);
        return new AvatarUploadUrlVO(uploadUrl, objectKey, "PUT", minioProperties.getExpirySeconds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateAvatar(Long userId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "objectKey不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String publicUrl = minioStorageService.getPublicUrl(objectKey);
        User update = new User();
        update.setId(userId);
        update.setAvatarUrl(publicUrl);
        userMapper.updateById(update);
        clearUserCache(userId);
        log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, publicUrl);

        user.setAvatarUrl(publicUrl);
        return buildUserInfoVO(user);
    }

    private UserInfoVO buildUserInfoVO(User user) {
        return new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getStatus(),
                user.getAvatarUrl()
        );
    }

    private void clearUserCache(Long userId) {
        try {
            RKeys keys = redissonClient.getKeys();
            keys.delete(CacheConstants.buildUserKey(userId));
            keys.deleteByPattern("dashboard:" + userId + ":*");
            keys.deleteByPattern("budget:" + userId + ":*");
            log.info("已清除用户全部缓存: userId={}", userId);
        } catch (Exception e) {
            log.warn("清除用户缓存失败: userId={}", userId, e);
        }
    }
}
