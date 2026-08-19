package com.ledger.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.modules.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 用户 Token 版本号自增（改密/注销时调用，使所有旧Token失效）
     */
    @Update("UPDATE user SET token_version = token_version + 1 WHERE id = #{userId}")
    int incrementTokenVersion(@Param("userId") Long userId);
}
