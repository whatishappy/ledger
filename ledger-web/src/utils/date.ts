/**
 * 日期工具 — LocalDate("YYYY-MM-DD") 与 Date 互转
 */

function pad(n: number): string {
  return n < 10 ? `0${n}` : `${n}`
}

/** Date → "YYYY-MM-DD" */
export function formatLocalDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** "YYYY-MM-DD" → Date（本地时区，避免 UTC 偏移一天） */
export function parseLocalDate(s: string): Date {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, (m || 1) - 1, d || 1)
}

/** Date → "YYYY-MM" */
export function formatYearMonth(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

/** "YYYY-MM" → { start: "YYYY-MM-DD", end: "YYYY-MM-DD" } 该月首末日 */
export function getMonthRange(month: string): { start: string; end: string } {
  const [y, m] = month.split('-').map(Number)
  const lastDay = new Date(y, m, 0).getDate()
  return { start: `${month}-01`, end: `${month}-${pad(lastDay)}` }
}

/** 当前日期 → "YYYY-MM-DD" */
export function today(): string {
  return formatLocalDate(new Date())
}

/** 当前月份 → "YYYY-MM" */
export function currentMonth(): string {
  return formatYearMonth(new Date())
}

/** 取月份中与当前年份的差异（用于月份选择器禁未来月） */
export function isFutureMonth(month: string): boolean {
  return month > currentMonth()
}
