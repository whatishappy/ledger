/**
 * 业务字典 — 与后端 AccountCategoryEnum（LLD §10.3）严格一致，共 6 类：
 * 餐饮/交通/购物/工资/娱乐/其他
 * 其中「工资」仅收入，「其他」收入/支出均可，其余 4 类仅支出。
 * 注意：必须与后端枚举一致，否则后端返回「收支分类无效」(code=400)。
 */

export const AccountType = {
  EXPENSE: 0,
  INCOME: 1,
} as const

export type AccountTypeValue = (typeof AccountType)[keyof typeof AccountType]

/** 支出分类（仅支出：餐饮/交通/购物/娱乐 + 其他） */
export const EXPENSE_CATEGORIES = ['餐饮', '交通', '购物', '娱乐', '其他'] as const

/** 收入分类（工资 + 其他） */
export const INCOME_CATEGORIES = ['工资', '其他'] as const

/** 全部分类（按后端枚举顺序） */
export const ALL_CATEGORIES = ['餐饮', '交通', '购物', '工资', '娱乐', '其他'] as const

export type CategoryName = (typeof ALL_CATEGORIES)[number]

/** 根据收支类型返回可用分类列表（「其他」两类通用） */
export function getCategoriesByType(type: AccountTypeValue): readonly CategoryName[] {
  if (type === AccountType.INCOME) return INCOME_CATEGORIES
  return EXPENSE_CATEGORIES
}
