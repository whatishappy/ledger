package com.ledger.modules.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 修改记账请求（A-03）
 */
@Data
public class AccountUpdateRequest {

    @NotNull(message = "账目ID不能为空")
    private Long id;

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

    /**
     * 乐观锁版本号（必填）
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
