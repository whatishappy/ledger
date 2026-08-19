package com.ledger.modules.budget.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 预算变更事件
 * 用于事务提交后清除 budget 和 dashboard 缓存
 */
@Data
@AllArgsConstructor
public class BudgetChangeEvent {

    private Long userId;

    /**
     * 受影响的月份（YYYY-MM）
     */
    private String month;
}
