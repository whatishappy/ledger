package com.ledger.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiQuotaVO {

    private int chatUsed;
    private int chatTotal;
    private long tokenUsed;
    private long tokenTotal;
    private double chatPercent;
    private double tokenPercent;
}
