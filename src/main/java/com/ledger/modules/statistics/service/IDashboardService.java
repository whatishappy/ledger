package com.ledger.modules.statistics.service;

import com.ledger.modules.statistics.dto.DashboardVO;

/**
 * 仪表盘服务接口
 */
public interface IDashboardService {

    /**
     * 仪表盘数据聚合（S-01，按详细设计 §7.3）
     * 返回月度总览+分类占比+趋势+预算进度
     */
    DashboardVO getDashboard(Long userId, String month);
}
