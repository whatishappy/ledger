package com.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.entity.ScheduledTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduledTransactionMapper extends BaseMapper<ScheduledTransaction> {
}
