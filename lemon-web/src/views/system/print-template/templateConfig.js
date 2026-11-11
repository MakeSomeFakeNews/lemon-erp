import { DEFAULT_FONT_SIZE, DEFAULT_MARGIN } from '@/utils/printTemplates'

/**
 * 打印模板页面的编辑状态：已保存的配置与单据目录合并成可编辑的配置（buildConfig），保存时再整理为配置 JSON（toPayload）
 */

/** 已保存的配置与目录合并：按保存的顺序，目录中新增的项放末尾 */
function merge(names, saved, nameKey, extra = {}) {
  const byName = new Map((saved || []).map((s) => [s[nameKey], s]))
  const ordered = (saved || []).filter((s) => names.includes(s[nameKey]))
  names.filter((n) => !byName.has(n)).forEach((n) => ordered.push({ [nameKey]: n }))
  return ordered.map((s) => ({ [nameKey]: s[nameKey], alias: s.alias || '', hidden: !!s.hidden, ...extra, ...(s.width ? { width: s.width } : {}) }))
}

/** 可编辑的配置：tp 为单据目录（PRINT_TYPES 中的一项），saved 为已保存的配置 */
export function buildConfig(tp, saved = {}) {
  return {
    title: saved.title || '',
    orientation: saved.orientation || 'portrait',
    paper: saved.paper || 'A4',
    paperWidth: saved.paperWidth || 210,
    paperHeight: saved.paperHeight || 297,
    margin: saved.margin ?? DEFAULT_MARGIN,
    fontSize: saved.fontSize || DEFAULT_FONT_SIZE,
    showCompany: saved.showCompany !== false,
    showNote: saved.showNote !== false,
    fields: merge(tp.fields, saved.fields, 'label'),
    columns: merge(tp.columns.map((c) => c.title), saved.columns, 'title', { width: null }),
    signs: merge(tp.signs, saved.signs, 'label'),
    overlay: saved.overlay || null
  }
}

/** 保存的内容：去掉空的显示名称、列宽 */
export function toPayload(cfg) {
  const clean = (rows, nameKey) =>
    rows.map((r) => ({
      [nameKey]: r[nameKey],
      ...(r.alias?.trim() ? { alias: r.alias.trim() } : {}),
      ...(r.hidden ? { hidden: true } : {}),
      ...(r.width ? { width: r.width } : {})
    }))
  return {
    ...(cfg.title?.trim() ? { title: cfg.title.trim() } : {}),
    orientation: cfg.orientation,
    paper: cfg.paper,
    ...(cfg.paper === 'custom' ? { paperWidth: cfg.paperWidth || 210, paperHeight: cfg.paperHeight || 297 } : {}),
    margin: cfg.margin ?? DEFAULT_MARGIN,
    fontSize: cfg.fontSize || DEFAULT_FONT_SIZE,
    showCompany: cfg.showCompany,
    showNote: cfg.showNote,
    fields: clean(cfg.fields, 'label'),
    columns: clean(cfg.columns, 'title'),
    signs: clean(cfg.signs, 'label'),
    ...(cfg.overlay ? { overlay: cfg.overlay } : {})
  }
}
