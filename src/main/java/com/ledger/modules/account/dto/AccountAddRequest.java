package com.ledger.modules.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增记账请求（A-01）
 */
@Data
public class AccountAddRequest {

    @NotNull(message = "收支类型不能为空")
    private Integer type;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "业务日期不能为空")
    private LocalDate accountDate;

    private String remark;

    private String extraJson;
}
