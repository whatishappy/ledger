package com.ledger.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账户状态（user.status）
 * 1-正常，0-已注销
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DELETED(0, "已注销"),
    NORMAL(1, "正常");

    private final int value;
    private final String description;

    public static boolean isNormal(Integer status) {
        return status != null && status == NORMAL.getValue();
    }

    public static boolean isDeleted(Integer status) {
        return status != null && status == DELETED.getValue();
    }
}
