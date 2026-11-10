import { computed, ref, watch } from 'vue'
import { load, save } from '@/utils/storage'

export const columnKey = (c) => c.key || c.dataIndex

/** 默认显示的列 key：列定义上写 defaultHidden: true 的次要列默认隐藏，用户可在「列设置」中勾选显示 */
export const defaultKeys = (columns) => columns.filter((c) => !c.defaultHidden).map(columnKey)

/** 没有写宽度的列（自适应剩余宽度，如备注）按此最小宽度计入横向滚动宽度 */
const DEFAULT_WIDTH = 80

/** 列宽合计：表格的 scroll.x 用它，与列定义保持一致（勾选列另加 32） */
export const sumWidth = (columns) => columns.reduce((sum, c) => sum + (Number(c.width) || DEFAULT_WIDTH), 0)

/** 与 Ant Design Vue 默认字体一致，用于测量按钮文字宽度 */
const FONT = "14px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif"
let measureCtx
function textWidth(text) {
  measureCtx ||= document.createElement('canvas').getContext('2d')
  if (!measureCtx) return String(text).length * 8
  measureCtx.font = FONT
  return measureCtx.measureText(String(text)).width
}

/**
 * 操作列宽度：按当前语言的按钮文字计算（英文按钮比中文长，写死的宽度在英文界面下按钮会溢出）。
 * labels 为一行中最多同时出现的按钮文字（已翻译，如 t('common.action.edit')；「更多」下拉也算一个按钮）；
 * 按状态互斥出现的按钮写成数组，按其中最宽的计算，如 [t('common.action.view'), [t('common.action.edit'), t('scm.order.inbound')]]。
 * 默认按钮带图标（.table-actions 中的 link 小按钮）；不带图标的按钮写成 { text, icon: false }。
 * 用法：{ title: t('common.action.operation'), key: 'action', width: actionWidth([...]), fixed: 'right' }
 */
export function actionWidth(labels) {
  const one = (l) => (typeof l === 'string' ? textWidth(l) + 34 : textWidth(l.text) + (l.icon === false ? 12 : 34))
  // 按钮：文字 + 图标 14 与间距 8 + 左右内边距 4 × 2 + 边框，每个按钮另留 2 像素余量（字体渲染差异）；按钮之间 gap 4；单元格左右内边距 8 × 2
  const widths = labels.map((l) => (Array.isArray(l) ? Math.max(...l.map(one)) : one(l)))
  return Math.ceil(widths.reduce((s, w) => s + w, 0) + 4 * (widths.length - 1) + 16 + 2)
}

/**
 * 列设置：checked 为显示中的列 key，配合 TableToolbar 使用。
 * 列定义上 defaultHidden: true 的列默认隐藏（备注、制单人、创建时间等次要信息），1440 宽下默认不需要横向滚动；
 * 隐藏只影响显示，导出仍包含这些列（见 TableToolbar）。
 * 传入 storageKey 时记住用户与默认不同的选择：{ hidden: 默认显示但用户隐藏的列, shown: 默认隐藏但用户勾选的列 }，
 * 以后新增的列和偏好中没有记录的列按列定义的默认处理；旧版本存的是隐藏列数组，读取时视为 hidden。
 * scrollX 为显示中的列宽合计，用作表格的 scroll.x（有勾选列时 + 32）：
 * 不要写死数字：写死的值比列宽合计大时各列被拉宽，用户隐藏列后表格仍然横向滚动
 */
export function useColumns(columns, storageKey) {
  const allKeys = columns.map(columnKey)
  const defaults = new Set(defaultKeys(columns))
  const cacheKey = storageKey ? `table-hidden-columns:${storageKey}` : null
  const saved = cacheKey ? load(cacheKey, null) : null
  const pref = Array.isArray(saved) ? { hidden: saved, shown: [] } : { hidden: saved?.hidden || [], shown: saved?.shown || [] }
  const checked = ref(allKeys.filter((k) => (defaults.has(k) ? !pref.hidden.includes(k) : pref.shown.includes(k))))
  if (cacheKey) {
    watch(checked, (keys) =>
      save(cacheKey, {
        hidden: allKeys.filter((k) => defaults.has(k) && !keys.includes(k)),
        shown: allKeys.filter((k) => !defaults.has(k) && keys.includes(k))
      })
    )
  }
  const visibleColumns = computed(() => columns.filter((c) => checked.value.includes(columnKey(c))))
  const scrollX = computed(() => sumWidth(visibleColumns.value))
  return { checked, visibleColumns, scrollX }
}
