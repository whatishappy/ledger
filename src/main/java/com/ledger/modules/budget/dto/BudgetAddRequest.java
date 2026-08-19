package com.ledger.modules.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 设定预算请求（B-01，按详细设计 §6.3）
 */
@Data
public class BudgetAddRequest {

    /**
     * 支出分类（仅支出类可设定预算）
     */
    @NotBlank(message = "分类不能为空")
    private String category;

    /**
     * 预算月份，格式 YYYY-MM，不能晚于当前月份
     */
    @NotBlank(message = "月份不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式必须为YYYY-MM")
    private String month;

    /**
     * 预算上限金额，必须大于0
     */
    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal amountLimit;
}
