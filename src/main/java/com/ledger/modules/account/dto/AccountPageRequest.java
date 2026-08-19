package com.ledger.modules.account.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 分页查询请求（A-02）
 */
@Data
public class AccountPageRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 收支类型（可选）：1-收入，0-支出
     */
    private Integer type;

    /**
     * 分类（可选）
     */
    private String category;

    /**
     * 起始日期（可选）
     */
    private LocalDate startDate;

    /**
     * 结束日期（可选）
     */
    private LocalDate endDate;

    /**
     * 关键词（可选，备注模糊搜索）
     */
    private String keyword;
}
