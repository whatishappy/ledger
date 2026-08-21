/**
 * 标签模块 — /api/v1/tags（TG-01~TG-07）
 * 标签 CRUD、账目关联、月度统计。
 */
import request from '@/utils/request'
import type {
  TagCreateRequest,
  TagStatisticsVO,
  TagUpdateRequest,
  TagVO,
} from '@/types/api'

/** TG-01 查询标签列表（支持名称/类型过滤、排序） */
export function listTags(params?: {
  name?: string
  type?: number
  sortBy?: string
}) {
  return request.get<TagVO[], TagVO[]>('/v1/tags', { params: params ?? {} })
}

/** TG-02 创建标签 */
export function createTag(data: TagCreateRequest) {
  return request.post<TagVO, TagVO>('/v1/tags', data)
}

/** TG-03 更新标签 */
export function updateTag(id: number, data: TagUpdateRequest) {
  return request.put<TagVO, TagVO>(`/v1/tags/${id}`, data)
}

/** TG-04 删除标签 */
export function deleteTag(id: number) {
  return request.delete<unknown, void>(`/v1/tags/${id}`)
}

/** TG-05 分配标签到账目（全量覆盖该账目的标签） */
export function assignTagsToAccount(accountId: number, tagIds: number[]) {
  return request.put<unknown, void>(`/v1/tags/accounts/${accountId}`, tagIds)
}

/** TG-06 查询账目关联的标签 */
export function getTagsByAccount(accountId: number) {
  return request.get<TagVO[], TagVO[]>(`/v1/tags/accounts/${accountId}`)
}

/** TG-07 标签月度统计 */
export function getTagStatistics(year: number, month: number) {
  return request.get<TagStatisticsVO, TagStatisticsVO>('/v1/tags/statistics', {
    params: { year, month },
  })
}
