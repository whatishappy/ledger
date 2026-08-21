package com.ledger.modules.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ledger.dto.calendar.CalendarDayAggregate;
import com.ledger.modules.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 账目 Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 统计指定月份指定类型金额总和（用于仪表盘月度总览）
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM account_book " +
            "WHERE user_id = #{userId} AND type = #{type} " +
            "AND account_date >= #{startDate} AND account_date <= #{endDate} " +
            "AND is_deleted = 0")
    BigDecimal sumAmountByTypeAndDateRange(@Param("userId") Long userId,
                                            @Param("type") int type,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 按分类汇总支出金额（用于仪表盘分类占比）
     */
    @Select("SELECT category, COALESCE(SUM(amount), 0) AS amount " +
            "FROM account_book " +
            "WHERE user_id = #{userId} AND type = 0 " +
            "AND account_date >= #{startDate} AND account_date <= #{endDate} " +
            "AND is_deleted = 0 " +
            "GROUP BY category")
    List<Map<String, Object>> sumExpenseGroupByCategory(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    /**
     * 按日汇总收支金额（用于仪表盘7日趋势）
     */
    @Select("SELECT account_date AS accountDate, type, COALESCE(SUM(amount), 0) AS amount " +
            "FROM account_book " +
            "WHERE user_id = #{userId} " +
            "AND account_date >= #{startDate} AND account_date <= #{endDate} " +
            "AND is_deleted = 0 " +
            "GROUP BY account_date, type " +
            "ORDER BY account_date")
    List<Map<String, Object>> sumAmountGroupByDate(@Param("userId") Long userId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 统计指定月份各支出分类已消费总额（用于预算进度计算）
     */
    @Select("SELECT category, COALESCE(SUM(amount), 0) AS spent " +
            "FROM account_book " +
            "WHERE user_id = #{userId} AND type = 0 " +
            "AND account_date >= #{startDate} AND account_date <= #{endDate} " +
            "AND is_deleted = 0 " +
            "GROUP BY category")
    List<Map<String, Object>> sumSpentGroupByCategory(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    /**
     * 统计符合条件的数据量（用于导出判断同步/异步）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM account_book WHERE user_id = #{userId} AND is_deleted = 0 " +
            "<if test='type != null'> AND type = #{type} </if>" +
            "<if test='category != null and category != \"\"'> AND category = #{category} </if>" +
            "<if test='startDate != null'> AND account_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND account_date &lt;= #{endDate} </if>" +
            "</script>")
    long countByConditions(@Param("userId") Long userId,
                            @Param("type") Integer type,
                            @Param("category") String category,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    /**
     * 按日汇总收支金额和笔数（用于日历热力图）
     */
    List<CalendarDayAggregate> getDailyAggregates(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
