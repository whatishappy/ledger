package com.ledger.modules.budget.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算实体（对应 budget 表，详细设计 §10.2.3）
 */
@Data
@TableName("budget")
public class Budget implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID（数据隔离关键字段）
     */
    private Long userId;

    /**
     * 支出分类（关联 account_book.category，仅支出类可设定预算）
     */
    private String category;

    /**
     * 预算月份，格式：YYYY-MM
     */
    private String month;

    /**
     * 月度预算上限金额
     */
    private BigDecimal amountLimit;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 逻辑删除标识：0-未删除，1-已删除
     */
    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
