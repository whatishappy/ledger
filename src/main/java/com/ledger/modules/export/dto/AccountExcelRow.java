package com.ledger.modules.export.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Excel 导出行数据（EasyExcel 注解）
 * 表头：账目ID/类型/分类/金额/业务日期/备注/创建时间
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountExcelRow {

    @ExcelProperty("账目ID")
    @ColumnWidth(15)
    private Long id;

    @ExcelProperty("类型")
    @ColumnWidth(10)
    private String type;

    @ExcelProperty("分类")
    @ColumnWidth(12)
    private String category;

    @ExcelProperty("金额")
    @ColumnWidth(12)
    private BigDecimal amount;

    @ExcelProperty("业务日期")
    @ColumnWidth(15)
    private LocalDate accountDate;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private LocalDateTime createTime;
}
