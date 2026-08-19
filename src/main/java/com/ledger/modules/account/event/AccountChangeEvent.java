package com.ledger.modules.account.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 账目变更事件
 * 用于事务提交后清除 dashboard 和 budget 缓存
 * 跨月修改时发布两个事件（oldMonth + newMonth）
 */
@Data
@AllArgsConstructor
public class AccountChangeEvent {

    private Long userId;

    /**
     * 受影响的月份（YYYY-MM）
     */
    private String month;
}
