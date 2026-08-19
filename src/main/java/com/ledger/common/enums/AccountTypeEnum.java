package com.ledger.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收支类型（account_book.type）
 * 1-收入，0-支出
 */
@Getter
@AllArgsConstructor
public enum AccountTypeEnum {

    EXPENSE(0, "支出"),
    INCOME(1, "收入");

    private final int value;
    private final String description;

    public static boolean isValid(Integer type) {
        if (type == null) {
            return false;
        }
        return type == EXPENSE.getValue() || type == INCOME.getValue();
    }

    public static AccountTypeEnum fromValue(int value) {
        for (AccountTypeEnum type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的收支类型: " + value);
    }
}
