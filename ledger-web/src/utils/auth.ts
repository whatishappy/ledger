/**
 * Token 与文件下载工具
 *
 * accessToken 纯内存存储（Pinia），不落 localStorage（防 XSS 持久窃取）。
 * refreshToken 由后端 HttpOnly Cookie 管理，前端不读写。
 */

/**
 * 解析 Content-Disposition 中的文件名。
 * 支持 RFC 5987 编码：`attachment;filename*=utf-8''%E8%B4%A6%E7%9B%AE.xlsx`
 */
export function parseFilenameFromDisposition(disposition?: string): string {
  if (!disposition) return ''
  const utf8 = disposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1])
    } catch {
      return utf8[1]
    }
  }
  const plain = disposition.match(/filename="?([^";]+)"?/i)
  return plain?.[1] ?? ''
}

/** 触发浏览器下载 Blob 文件 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 从下载响应中提取文件名：优先 Content-Disposition，缺省给兜底名 */
export function resolveDownloadFilename(
  response: { headers: Record<string, unknown> | undefined },
  fallback: string,
): string {
  const disposition = response.headers?.['content-disposition'] as string | undefined
  return parseFilenameFromDisposition(disposition) || fallback
}
