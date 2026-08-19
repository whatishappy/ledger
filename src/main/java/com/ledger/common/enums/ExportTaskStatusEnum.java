package com.ledger.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 导出任务状态（export_task.status）
 * 0-待处理，1-处理中，2-已完成，3-失败，4-已过期
 * 状态机：待处理(0)→处理中(1)→已完成(2)→已过期(4)；失败(3)→已过期(4)
 */
@Getter
@AllArgsConstructor
public enum ExportTaskStatusEnum {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败"),
    EXPIRED(4, "已过期");

    private final int value;
    private final String description;

    public static ExportTaskStatusEnum fromValue(int value) {
        for (ExportTaskStatusEnum status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的导出任务状态: " + value);
    }

    /**
     * 是否为终态（不可再变更）
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == EXPIRED;
    }
}
