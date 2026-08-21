package com.ledger.modules.tag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagUpdateDTO {

    @Size(max = 50, message = "标签名称长度不能超过50")
    private String name;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "颜色格式必须为十六进制（如#FF5733）")
    private String color;

    @Min(value = 0, message = "标签类型无效：0-全部/1-支出/2-收入")
    @Max(value = 2, message = "标签类型无效：0-全部/1-支出/2-收入")
    private Integer type;

    private Integer sort;
}
