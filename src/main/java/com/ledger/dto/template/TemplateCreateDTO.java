package com.ledger.dto.template;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过100个字符")
    private String name;

    @NotNull(message = "收支类型不能为空")
    private Integer type;

    private String category;

    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    private List<Long> tags;
}
