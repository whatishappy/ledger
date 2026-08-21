/**
 * 交易图片附件模块 — /api/images
 * 小票/发票/截图上传、查询、删除。
 *
 * 说明：后端 TransactionImageController 通过 @RequestHeader("X-User-Id") 读取用户，
 * 前端不主动发送 X-User-Id（避免客户端伪造身份），统一依赖 JWT Authorization 头
 *（由 utils/request.ts 拦截器注入）。若后端缺少将 JWT userId 写入该头的过滤器，
 * 需后端补过滤器或改为 UserContext 解析。
 */
import request from '@/utils/request'
import type { TransactionImageVO } from '@/types/api'

/** 上传交易图片（multipart） */
export function uploadImage(
  accountId: number,
  file: File,
  imageType: number = 1,
) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<TransactionImageVO, TransactionImageVO>(
    '/images/upload',
    formData,
    {
      params: { accountId, imageType },
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
}

/** 查询账目下的所有图片 */
export function listImagesByAccount(accountId: number) {
  return request.get<TransactionImageVO[], TransactionImageVO[]>(
    `/images/account/${accountId}`,
  )
}

/** 删除图片 */
export function deleteImage(id: number) {
  return request.delete<unknown, void>(`/images/${id}`)
}
