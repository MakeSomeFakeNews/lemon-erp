import { Marked } from 'marked'

/**
 * 公告正文（Markdown）转为 <rich-text> 可用的 HTML：正文中的原始 HTML 按文字显示、不显示图片（与 Web 端 utils/markdown.js 一致，
 * 不借图片地址向外发送请求）；rich-text 本身不执行脚本、不支持事件。小程序中页面样式对 rich-text 内部不生效，标签样式写成内联
 */
const escape = (s) => String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const md = new Marked({
  gfm: true,
  breaks: true,
  renderer: {
    html: ({ text }) => escape(text),
    image: ({ text }) => escape(text)
  }
})

const STYLES = {
  h1: 'font-size:18px;font-weight:600;margin:16px 0 8px;',
  h2: 'font-size:17px;font-weight:600;margin:16px 0 8px;',
  h3: 'font-size:16px;font-weight:600;margin:12px 0 6px;',
  h4: 'font-size:15px;font-weight:600;margin:12px 0 6px;',
  p: 'margin:0 0 8px;',
  ul: 'padding-left:20px;margin:0 0 8px;',
  ol: 'padding-left:20px;margin:0 0 8px;',
  blockquote: 'margin:0 0 8px;padding-left:10px;border-left:3px solid #e5e6eb;color:#86909c;',
  table: 'border-collapse:collapse;margin:8px 0;',
  th: 'border:1px solid #e5e6eb;padding:4px 8px;background:#f7f8fa;',
  td: 'border:1px solid #e5e6eb;padding:4px 8px;',
  code: 'padding:1px 4px;background:#f2f3f5;border-radius:4px;font-size:13px;',
  pre: 'padding:8px;background:#f2f3f5;border-radius:4px;overflow:auto;',
  a: 'color:#1677ff;'
}

export function renderNotice(text) {
  if (!text) return ''
  const html = md.parse(text, { async: false })
  return html.replace(/<(h[1-4]|p|ul|ol|blockquote|table|th|td|code|pre|a)(\s[^>]*)?>/g, (m, tag, attrs = '') => `<${tag}${attrs} style="${STYLES[tag]}">`)
}
