/**
 * 表格省略单元格的悬停提示：antd 只在单元格内容是纯文本时给 ellipsis 列加 title，
 * 使用 #bodyCell 插槽渲染的列没有提示。这里全局委托 mouseover，内容确实被截断时把文字设为 title。
 */
const CELL = '.ant-table-cell-ellipsis'

function onMouseOver(e) {
  const cell = e.target instanceof Element ? e.target.closest(CELL) : null
  if (!cell || cell.tagName === 'TH') return
  // antd 自己设置过 title（纯文本列）的保持不变
  if (cell.hasAttribute('title') && !cell.dataset.autoTitle) return
  const text = cell.textContent.trim()
  const truncated = text && (cell.scrollWidth > cell.clientWidth || [...cell.querySelectorAll('*')].some((el) => el.clientWidth > 0 && el.scrollWidth > el.clientWidth))
  if (truncated) {
    cell.setAttribute('title', text)
    cell.dataset.autoTitle = '1'
  } else if (cell.dataset.autoTitle) {
    cell.removeAttribute('title')
    delete cell.dataset.autoTitle
  }
}

export function installEllipsisTitle() {
  document.addEventListener('mouseover', onMouseOver, { passive: true })
}
