package com.ledger.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表，详细设计 §10.2.1）
 */
@Data
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录用户名（唯一）
     */
    private String username;

    /**
     * BCrypt 加密密码
     */
    private String password;

    /**
     * 用户昵称（注册时默认取 username）
     */
    private String nickname;

    /**
     * Token 版本号（改密/注销时自增，旧Token失效）
     */
    private Integer tokenVersion;

    /**
     * 账户状态：1-正常，0-已注销
     */
    private Integer status;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
