package com.ledger.modules.imports.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillImportResultVO {

    private Integer imported;

    private Integer skipped;

    private BigDecimal amountSumIncome;

    private BigDecimal amountSumExpense;
}
