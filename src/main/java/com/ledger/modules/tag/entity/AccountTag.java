package com.ledger.modules.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("account_tag")
public class AccountTag implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private Long tagId;
}
