/**
 * 账单导入模块 — /api/v1/imports（IM-01~IM-03）
 * 支付宝/微信账单 CSV 上传预览、确认导入、支持来源查询。
 */
import request from '@/utils/request'
import type {
  BillImportConfirmRequest,
  BillImportPreviewVO,
  BillImportResultVO,
} from '@/types/api'

/** IM-01 账单解析预览（multipart：file + source） */
export function previewBill(file: File, source: string) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<BillImportPreviewVO, BillImportPreviewVO>(
    '/v1/imports/preview',
    formData,
    {
      params: { source },
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
}

/** IM-02 确认导入（回传预览 token + 分类映射覆盖） */
export function confirmBillImport(data: BillImportConfirmRequest) {
  return request.post<BillImportResultVO, BillImportResultVO>(
    '/v1/imports/confirm',
    data,
  )
}

/** IM-03 查询支持的账单来源（source -> 状态码） */
export function getSupportedSources() {
  return request.get<Record<string, number>, Record<string, number>>(
    '/v1/imports/support',
  )
}
