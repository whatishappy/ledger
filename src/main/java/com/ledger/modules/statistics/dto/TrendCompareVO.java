package com.ledger.modules.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrendCompareVO {

    private String period;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    private BigDecimal incomeChange;
    private BigDecimal expenseChange;
    private BigDecimal incomeChangeRate;
    private BigDecimal expenseChangeRate;

    public static TrendCompareVO of(String period, BigDecimal income, BigDecimal expense,
                                     BigDecimal prevIncome, BigDecimal prevExpense) {
        TrendCompareVO vo = new TrendCompareVO();
        vo.setPeriod(period);
        vo.setIncome(income);
        vo.setExpense(expense);
        vo.setBalance(income.subtract(expense));

        BigDecimal incomeChange = income.subtract(prevIncome);
        BigDecimal expenseChange = expense.subtract(prevExpense);
        vo.setIncomeChange(incomeChange);
        vo.setExpenseChange(expenseChange);

        if (prevIncome.compareTo(BigDecimal.ZERO) > 0) {
            vo.setIncomeChangeRate(incomeChange.multiply(new BigDecimal("100"))
                    .divide(prevIncome, 2, java.math.RoundingMode.HALF_UP));
        } else {
            vo.setIncomeChangeRate(BigDecimal.ZERO);
        }
        if (prevExpense.compareTo(BigDecimal.ZERO) > 0) {
            vo.setExpenseChangeRate(expenseChange.multiply(new BigDecimal("100"))
                    .divide(prevExpense, 2, java.math.RoundingMode.HALF_UP));
        } else {
            vo.setExpenseChangeRate(BigDecimal.ZERO);
        }
        return vo;
    }
}
