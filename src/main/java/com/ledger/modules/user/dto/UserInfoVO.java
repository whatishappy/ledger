package com.ledger.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private Integer status;
    private String avatarUrl;

    public UserInfoVO(Long id, String username, String nickname, Integer status) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.status = status;
    }
}
