package com.ledger.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户信息 VO
 */
@Data
@AllArgsConstructor
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private Integer status;
}
