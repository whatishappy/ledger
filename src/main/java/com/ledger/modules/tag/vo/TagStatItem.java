package com.ledger.modules.tag.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagStatItem {

    private Long tagId;

    private String tagName;

    private String color;

    private BigDecimal totalAmount;

    private Long transactionCount;

    private BigDecimal percentage;
}
