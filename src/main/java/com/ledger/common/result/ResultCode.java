package com.ledger.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 完整错误码表（基于详细设计 §12）
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 通用错误
    SUCCESS(0, "成功"),
    PARAM_VALIDATION_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未认证或Token无效"),
    FORBIDDEN(403, "无权限操作"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),

    // 用户模块 1xxx
    USERNAME_ALREADY_EXISTS(1001, "用户名已存在"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    USER_NOT_FOUND(1003, "用户不存在"),
    TOKEN_VERSION_MISMATCH(1004, "Token已失效，请重新登录"),
    REFRESH_TOKEN_EXPIRED(1005, "Refresh Token已过期"),
    PASSWORD_ERROR(1006, "密码错误"),

    // 账目模块 2xxx
    ACCOUNT_NOT_FOUND(2001, "账目不存在"),
    OPTIMISTIC_LOCK_CONFLICT(2002, "数据已被修改，请刷新重试"),

    // 预算模块 3xxx
    BUDGET_ALREADY_EXISTS(3001, "该分类本月已有预算"),
    BUDGET_NOT_FOUND(3002, "预算不存在"),

    // 导出模块 4xxx
    EXPORT_TOO_MANY_ROWS(4001, "导出数据量超过上限"),
    EXPORT_TASK_NOT_FOUND(4002, "导出任务不存在或已过期");

    private final int code;
    private final String message;
}
