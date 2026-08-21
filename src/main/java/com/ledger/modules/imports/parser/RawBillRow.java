package com.ledger.modules.imports.parser;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RawBillRow implements Serializable {

    private String tradeNo;

    private LocalDateTime tradeTime;

    private Integer type;

    private String counterparty;

    private String goods;

    private BigDecimal amount;

    private String paymentMethod;

    private String source;
}
