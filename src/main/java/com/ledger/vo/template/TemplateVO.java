package com.ledger.vo.template;

import com.ledger.modules.account.dto.AccountVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVO {

    private Long id;

    private Long userId;

    private String name;

    private Integer type;

    private String category;

    private BigDecimal amount;

    private String remark;

    private List<Long> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private AccountVO applyExample;
}
