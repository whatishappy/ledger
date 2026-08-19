package com.ledger.modules.budget.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ledger.modules.budget.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算 Mapper
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
