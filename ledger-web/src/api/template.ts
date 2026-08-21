/**
 * 交易模板模块 — /api/v1/templates（TP-01~TP-05）
 * 模板 CRUD、从模板生成账目。
 */
import request from '@/utils/request'
import type {
  AccountVO,
  TemplateApplyRequest,
  TemplateCreateRequest,
  TemplateUpdateRequest,
  TemplateVO,
} from '@/types/api'

export interface TemplateListQuery {
  keyword?: string
  type?: number
  sortBy?: string
  page?: number
  size?: number
}

/** TP-01 查询模板列表（关键词/类型过滤、排序、分页） */
export function listTemplates(params?: TemplateListQuery) {
  return request.get<TemplateVO[], TemplateVO[]>('/v1/templates', {
    params: params ?? {},
  })
}

/** TP-02 创建交易模板（同名校验） */
export function createTemplate(data: TemplateCreateRequest) {
  return request.post<TemplateVO, TemplateVO>('/v1/templates', data)
}

/** TP-03 更新交易模板 */
export function updateTemplate(id: number, data: TemplateUpdateRequest) {
  return request.put<TemplateVO, TemplateVO>(`/v1/templates/${id}`, data)
}

/** TP-04 删除交易模板 */
export function deleteTemplate(id: number) {
  return request.delete<unknown, void>(`/v1/templates/${id}`)
}

/** TP-05 应用模板生成账目（可选覆盖金额/备注/日期） */
export function applyTemplate(id: number, data?: TemplateApplyRequest) {
  return request.post<AccountVO, AccountVO>(`/v1/templates/${id}/apply`, data ?? {})
}
