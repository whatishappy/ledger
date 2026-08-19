package com.ledger.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 收支分类（account_book.category / budget.category）
 * 餐饮/交通/购物/工资/娱乐/其他（共 6 个，LLD §10.3）
 * 其中"工资"属收入类，"其他"收入/支出均可
 */
@Getter
@AllArgsConstructor
public enum AccountCategoryEnum {

    DINING("餐饮", "三餐、外卖、零食等", false),
    TRANSPORT("交通", "公交、打车、加油等", false),
    SHOPPING("购物", "衣服、日用品、数码等", false),
    SALARY("工资", "薪资收入", true),
    ENTERTAINMENT("娱乐", "电影、游戏、旅游等", false),
    OTHER("其他", "无法归类的收支", null);

    private final String value;
    private final String description;
    // null 表示收入/支出均可
    private final Boolean incomeOnly;

    /**
     * 是否为支出类（仅支出类可设定预算）
     */
    public boolean isExpense() {
        return !Boolean.TRUE.equals(incomeOnly);
    }

    /**
     * 是否为收入类
     */
    public boolean isIncome() {
        return !Boolean.FALSE.equals(incomeOnly);
    }

    public static boolean isValid(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(e -> e.getValue().equals(category));
    }

    /**
     * 根据分类值获取枚举对象
     */
    public static AccountCategoryEnum getByValue(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getValue().equals(category))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有支出分类（用于预算设定）
     */
    public static List<AccountCategoryEnum> getExpenseCategories() {
        return Arrays.stream(values())
                .filter(AccountCategoryEnum::isExpense)
                .toList();
    }
}
