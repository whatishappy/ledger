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
    EXPORT_TASK_NOT_FOUND(4002, "导出任务不存在或已过期"),

    // 标签模块 5xxx
    TAG_NAME_ALREADY_EXISTS(5001, "标签名已存在"),
    TAG_NOT_FOUND(5002, "标签不存在或无权访问"),
    TAG_LIMIT_EXCEEDED(5003, "标签数量超过上限（每用户最多50个）"),

    // 模板模块 51xx
    TEMPLATE_NAME_EXISTS(5101, "模板名已存在"),
    TEMPLATE_NOT_FOUND(5102, "模板不存在或无权访问"),

    // 导入模块 52xx
    IMPORT_FORMAT_UNSUPPORTED(5201, "账单文件格式不支持"),
    IMPORT_PARSE_FAILED(5202, "账单解析失败"),
    IMPORT_LIMIT_EXCEEDED(5203, "账单导入记录数超过上限（单次最多1000条）"),

    // 定时交易 53xx
    CRON_INVALID(5301, "定时交易cron表达式无效"),
    SCHEDULED_LIMIT_EXCEEDED(5302, "定时交易数量超过上限（每用户最多20个）"),

    // AI模块 6xxx
    AI_CHAT_FAILED(6001, "AI对话失败（模型超时或异常）"),
    AI_QUOTA_EXHAUSTED(6002, "AI配额已用尽（当日50次或10万Token）"),
    AI_CONTENT_VIOLATION(6003, "AI请求内容违规"),
    AI_OCR_FAILED(6004, "OCR识别失败"),
    AI_REPORT_FAILED(6005, "报告生成失败"),
    AI_OPERATION_BLACKLISTED(6006, "AI操作在黑名单中，禁止执行"),
    AI_SERVICE_UNCONFIGURED(6011, "AI服务未配置，请联系管理员配置API Key");

    private final int code;
    private final String message;
}
