import { describe, it, expect, vi, afterEach } from 'vitest'
import { downloadBlob, parseFilenameFromDisposition, resolveDownloadFilename } from '@/utils/auth'

describe('auth 工具', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('解析 RFC5987 编码的中文文件名', () => {
    const disp = "attachment;filename*=utf-8''%E8%B4%A6%E7%9B%AE%E5%AF%BC%E5%87%BA.xlsx"
    expect(parseFilenameFromDisposition(disp)).toBe('账目导出.xlsx')
  })

  it('解析普通 filename', () => {
    expect(parseFilenameFromDisposition('attachment;filename="ledger.xlsx"')).toBe('ledger.xlsx')
    expect(parseFilenameFromDisposition('attachment;filename=plain.xlsx')).toBe('plain.xlsx')
  })

  it('空 header 返回空字符串', () => {
    expect(parseFilenameFromDisposition()).toBe('')
    expect(parseFilenameFromDisposition(undefined)).toBe('')
  })

  it('resolveDownloadFilename 缺省给兜底名', () => {
    expect(resolveDownloadFilename({ headers: {} }, 'fallback.xlsx')).toBe('fallback.xlsx')
    expect(
      resolveDownloadFilename(
        { headers: { 'content-disposition': "attachment;filename*=utf-8''a.xlsx" } },
        'fallback.xlsx',
      ),
    ).toBe('a.xlsx')
  })

  it('downloadBlob 触发 <a> 点击下载', () => {
    const createURL = vi.fn(() => 'blob:mock-url')
    const revokeURL = vi.fn()
    vi.stubGlobal('URL', { ...URL, createObjectURL: createURL, revokeObjectURL: revokeURL })

    // 用真实 <a> 元素（保证 jsdom 的 appendChild 接受），仅覆写 click
    const anchor = document.createElement('a')
    const clickSpy = vi.fn()
    anchor.click = clickSpy
    vi.spyOn(document, 'createElement').mockReturnValue(anchor)
    const appendSpy = vi.spyOn(document.body, 'appendChild')
    const removeSpy = vi.spyOn(document.body, 'removeChild')

    downloadBlob(new Blob(['x']), 'test.xlsx')

    expect(createURL).toHaveBeenCalled()
    expect(clickSpy).toHaveBeenCalled()
    expect(anchor.download).toBe('test.xlsx')
    expect(anchor.href).toBe('blob:mock-url')
    expect(appendSpy).toHaveBeenCalledWith(anchor)
    expect(removeSpy).toHaveBeenCalledWith(anchor)
    expect(revokeURL).toHaveBeenCalledWith('blob:mock-url')
  })
})
