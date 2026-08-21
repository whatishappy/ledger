/**
 * Markdown 渲染工具（V2.1 新增）
 *
 * 用途：AI 对话流式输出渲染为 HTML（支持表格/列表/代码块高亮）。
 * - markdown-it 实例禁用原始 HTML（防 XSS）
 * - highlight.js 提供代码高亮（github 主题 CSS 在此引入）
 *
 * 流式策略：每次 chunk 到达后对「已累积的全文」重新渲染。AI 回复通常较短（<2k 字），
 * 全量重渲染性能可接受，且能正确处理跨 chunk 的 Markdown 结构（如未闭合代码块）。
 */
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
// 引入 highlight.js 主题样式（Vite 支持 .css 副作用导入）
import 'highlight.js/styles/github.css'

const md = new MarkdownIt({
  html: false, // 禁用原始 HTML，防止 AI 输出注入 XSS
  linkify: true,
  breaks: true,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        const highlighted = hljs.highlight(str, { language: lang }).value
        return `<pre class="hljs"><code>${highlighted}</code></pre>`
      } catch {
        // 降级为转义输出
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  },
})

/** 将 Markdown 文本渲染为 HTML 字符串 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  return md.render(text)
}

/**
 * 流式渲染：传入当前已累积的全文，返回其 HTML。
 * 每次新 chunk 到达后调用，UI 用返回值替换消息气泡内容。
 */
export function renderStreaming(buffer: string): string {
  return renderMarkdown(buffer)
}
