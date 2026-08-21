package com.ledger.modules.statistics.service;

import com.ledger.modules.statistics.dto.DashboardVO;
import com.ledger.modules.statistics.dto.TrendCompareVO;

import java.util.List;

/**
 * 仪表盘服务接口
 */
public interface IDashboardService {

    /**
     * 仪表盘数据聚合（S-01，按详细设计 §7.3）
     * 返回月度总览+分类占比+趋势+预算进度
     */
    DashboardVO getDashboard(Long userId, String month);

    /**
     * 获取月度同比（与去年同月对比）
     */
    TrendCompareVO getYearOverYear(Long userId, String month);

    /**
     * 获取月度环比（与上一个月对比）
     */
    TrendCompareVO getMonthOverMonth(Long userId, String month);

    /**
     * 获取多月趋势对比
     */
    List<TrendCompareVO> getMultiMonthTrend(Long userId, String startMonth, String endMonth);
}
