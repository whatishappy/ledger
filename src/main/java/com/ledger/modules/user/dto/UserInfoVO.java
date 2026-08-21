package com.ledger.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String avatarUrl;
}
