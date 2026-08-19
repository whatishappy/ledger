package com.ledger.modules.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账目 VO
 */
@Data
@AllArgsConstructor
public class AccountVO {

    private Long id;
    private Integer type;
    private String category;
    private BigDecimal amount;
    private LocalDate accountDate;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
