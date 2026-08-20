package com.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("transaction_image")
public class TransactionImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("account_id")
    private Long accountId;

    @TableField("image_url")
    private String imageUrl;

    @TableField("image_type")
    private Integer imageType;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
