import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ gfm: true, breaks: true })

// 链接在新窗口打开，并禁止访问 opener
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A') {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

/**
 * Markdown 转为安全的 HTML（模型的回答等不可信内容）：marked 解析后由 DOMPurify 清理脚本、事件属性、javascript: 链接等，
 * 结果才能用于 v-html。不允许图片（避免借图片地址向外发送数据）
 */
export function renderMarkdown(text) {
  if (!text) return ''
  const html = marked.parse(text, { async: false })
  return DOMPurify.sanitize(html, { FORBID_TAGS: ['img', 'style', 'form', 'input', 'iframe'], FORBID_ATTR: ['style'] })
}
