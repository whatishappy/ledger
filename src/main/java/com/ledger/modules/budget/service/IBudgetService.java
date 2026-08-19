package com.ledger.modules.budget.service;

import com.ledger.modules.budget.dto.BudgetAddRequest;
import com.ledger.modules.budget.dto.BudgetVO;

import java.util.List;

/**
 * 预算服务接口
 */
public interface IBudgetService {

    /**
     * 设定月度预算（B-01，按详细设计 §6.3）
     * @return 新生成的预算ID
     */
    Long addBudget(Long userId, BudgetAddRequest request);

    /**
     * 查询预算列表（B-04，按详细设计 §6.4）
     * 含进度计算，N+1已优化（SQL固定2次）
     * @return 预算进度列表
     */
    List<BudgetVO> listBudgetsWithProgress(Long userId, String month);
}
