package com.ledger.modules.imports.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillImportPreviewVO {

    private String token;

    private Integer count;

    private BigDecimal amountSum;

    private Long conflicts;

    private List<ImportedBillRowVO> sampleRows;
}
