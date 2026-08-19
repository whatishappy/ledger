package com.ledger.modules.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账目实体（对应 account_book 表，详细设计 §10.2.2）
 */
@Data
@TableName(value = "account_book", autoResultMap = true)
public class Account implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID（数据隔离关键字段）
     */
    private Long userId;

    /**
     * 收支类型：1-收入，0-支出
     */
    private Integer type;

    /**
     * 收支分类：餐饮/交通/购物/工资/娱乐/其他
     */
    private String category;

    /**
     * 金额（精确到分）
     */
    private BigDecimal amount;

    /**
     * 业务发生日期（支持补录）
     */
    private LocalDate accountDate;

    /**
     * 备注描述
     */
    private String remark;

    /**
     * 扩展JSON字段（AI标签等）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String extraJson;

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
