package com.ledger.dto.template;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TemplateApplyDTO {

    @DecimalMin(value = "0.01", message = "覆盖金额必须大于0")
    private BigDecimal amount;

    @Size(max = 255, message = "覆盖备注不能超过255个字符")
    private String remark;

    private LocalDate dateAt;

    private Long bookId;
}
