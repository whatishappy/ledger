/**
 * API 类型定义 — 与后端 Result<T> 及各 DTO/VO 1:1 对应。
 * 契约来源：后端 Controller + DTO 源码（2026-08-18 核对）。
 *
 * 关键约定：
 * - 成功判定：`code === 0`（非 200）
 * - 字段名 `message`（非 `msg`）
 * - 分页响应为 MyBatis-Plus IPage 结构：records/total/size/current
 * - 时间均为 ISO 字符串：LocalDate="YYYY-MM-DD"，LocalDateTime="YYYY-MM-DDTHH:mm:ss"
 */
import type { AccountTypeValue } from './business'

/** 后端统一响应 Result<T> */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

/* ==================== 用户模块 ==================== */

export interface RegisterRequest {
  username: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  status: number
}

export interface LoginVO {
  accessToken: string
  refreshToken: string
  userInfo: UserInfoVO
}

/* ==================== 账目模块 ==================== */

export interface AccountAddRequest {
  type: AccountTypeValue
  category: string
  amount: number
  /** YYYY-MM-DD */
  accountDate: string
  remark?: string
  extraJson?: string
}

export interface AccountPageQuery {
  pageNum: number
  pageSize: number
  type?: AccountTypeValue
  category?: string
  /** YYYY-MM-DD */
  startDate?: string
  /** YYYY-MM-DD */
  endDate?: string
  keyword?: string
}

export interface AccountUpdateRequest {
  id: number
  type: AccountTypeValue
  category: string
  amount: number
  accountDate: string
  remark?: string
  /** 乐观锁版本号，编辑时从 AccountVO.version 回传 */
  version: number
}

export interface AccountVO {
  id: number
  type: AccountTypeValue
  category: string
  amount: number
  accountDate: string
  remark?: string
  version: number
  createTime: string
  updateTime: string
}

/** MyBatis-Plus IPage 序列化结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/* ==================== 预算模块 ==================== */

export interface BudgetAddRequest {
  category: string
  /** YYYY-MM */
  month: string
  amountLimit: number
}

export interface BudgetVO {
  id: number
  category: string
  amountLimit: number
  spent: number
  /** 进度百分比（保留 2 位小数） */
  progress: number
  isOverBudget: boolean
}

/* ==================== 统计模块 ==================== */

export interface CategoryStatVO {
  category: string
  amount: number
  percentage: number
}

export interface TrendVO {
  /** YYYY-MM-DD */
  date: string
  income: number
  expense: number
}

export interface DashboardVO {
  /** YYYY-MM */
  month: string
  monthIncome: number
  monthExpense: number
  balance: number
  categoryStats: CategoryStatVO[]
  trend: TrendVO[]
  budgetProgress: BudgetVO[]
}

/* ==================== 导出模块 ==================== */

export interface ExportRequest {
  type?: AccountTypeValue
  category?: string
  /** YYYY-MM-DD */
  startDate?: string
  /** YYYY-MM-DD */
  endDate?: string
}

/** 导出任务状态：0待处理 1处理中 2已完成 3失败 4已过期 */
export const ExportTaskStatusCode = {
  PENDING: 0,
  PROCESSING: 1,
  COMPLETED: 2,
  FAILED: 3,
  EXPIRED: 4,
} as const

export interface ExportTaskVO {
  taskId: string
  /** 状态描述：待处理/处理中/已完成/失败/已过期 */
  status: string
  statusCode: number
  fileUrl?: string
  fileSize?: number
  rowCount?: number
  errorMsg?: string
  createTime: string
  expireTime?: string
}
