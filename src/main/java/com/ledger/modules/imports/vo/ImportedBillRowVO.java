package com.ledger.modules.imports.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportedBillRowVO {

    private String date;

    private String counterparty;

    private BigDecimal amount;

    private String type;

    private String source;

    private String preCategory;
}
