package com.ledger.modules.user.service;

import com.ledger.modules.user.dto.AvatarUploadUrlVO;
import com.ledger.modules.user.dto.LoginVO;
import com.ledger.modules.user.dto.RegisterRequest;
import com.ledger.modules.user.dto.UserInfoVO;

public interface IUserService {

    void register(RegisterRequest request);

    LoginVO login(String username, String password);

    UserInfoVO getUserInfo(Long userId);

    void cancelAccount(Long userId, String password);

    String refreshAccessToken(String refreshToken);

    AvatarUploadUrlVO createAvatarUploadUrl(Long userId, String fileType);

    UserInfoVO updateAvatar(Long userId, String objectKey);
}
