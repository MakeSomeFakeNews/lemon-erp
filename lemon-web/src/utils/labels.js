import JsBarcode from 'jsbarcode'
import QRCode from 'qrcode'
import { loadTemplates } from './printTemplates'
import { printHtml } from './print'
import { t as $t } from '@/locales'

/**
 * 条码标签：物料、库位、箱号。标签模板（尺寸、条码类型、显示的字段）在「打印模板」中维护，按模板标识保存（label.*）。
 *
 * 模板配置：{ width, height（mm）, codeType: 'CODE128' | 'QR', showText, fontSize, fields: [{ key, alias, hidden }] }
 * 条码内容：物料为条码（没有时为编码），库位为库位编码，箱为箱号；移动作业页面扫码时按这些内容识别
 */
export const LABEL_TYPES = [
  {
    key: 'label.material',
    name: $t('wms.label.material.name'),
    codeType: 'CODE128',
    fields: [
      { key: 'name', label: $t('wms.label.material.fieldName') },
      { key: 'code', label: $t('wms.label.material.fieldCode') },
      { key: 'spec', label: $t('wms.label.material.fieldSpec') },
      { key: 'unit', label: $t('wms.label.material.fieldUnit') }
    ],
    value: (d) => d.barcode || d.code,
    sample: { code: 'IPC-3000', name: $t('wms.label.material.sampleName'), spec: 'i5-1235U/16G/512G', unit: $t('wms.label.material.sampleUnit'), barcode: '6970000000017' }
  },
  {
    key: 'label.location',
    name: $t('wms.label.location.name'),
    codeType: 'QR',
    fields: [
      { key: 'code', label: $t('wms.label.location.fieldCode') },
      { key: 'name', label: $t('wms.label.location.fieldName') },
      { key: 'zone', label: $t('wms.label.location.fieldZone') },
      { key: 'warehouse', label: $t('wms.label.location.fieldWarehouse') }
    ],
    value: (d) => d.code,
    sample: { code: 'A-01-02', name: $t('wms.label.location.sampleName'), zone: $t('wms.label.location.sampleZone'), warehouse: $t('wms.label.location.sampleWarehouse') }
  },
  {
    key: 'label.box',
    name: $t('wms.label.box.name'),
    codeType: 'CODE128',
    fields: [
      { key: 'boxNo', label: $t('wms.label.box.fieldBoxNo') },
      { key: 'customer', label: $t('wms.label.box.fieldCustomer') },
      { key: 'deliveryNo', label: $t('wms.label.box.fieldDeliveryNo') },
      { key: 'seq', label: $t('wms.label.box.fieldSeq') },
      { key: 'contents', label: $t('wms.label.box.fieldContents') },
      { key: 'weight', label: $t('wms.label.box.fieldWeight') }
    ],
    value: (d) => d.boxNo,
    sample: { boxNo: 'FH202610150001-01', customer: $t('wms.label.box.sampleCustomer'), deliveryNo: 'FH202610150001', seq: '1 / 3',
      contents: $t('wms.label.box.sampleContents'), weight: '12.5 kg' }
  }
]
export const LABEL_TYPE_MAP = Object.fromEntries(LABEL_TYPES.map((t) => [t.key, t]))
export const isLabel = (key) => !!LABEL_TYPE_MAP[key]

/** 已保存的模板与默认值合并 */
export function labelConfig(type, saved = {}) {
  const t = LABEL_TYPE_MAP[type]
  const byKey = new Map((saved.fields || []).map((f) => [f.key, f]))
  const ordered = (saved.fields || []).filter((f) => t.fields.some((x) => x.key === f.key))
  t.fields.filter((f) => !byKey.has(f.key)).forEach((f) => ordered.push({ key: f.key }))
  return {
    width: saved.width || 60,
    height: saved.height || 40,
    codeType: saved.codeType || t.codeType,
    showText: saved.showText !== false,
    fontSize: saved.fontSize || 9,
    fields: ordered.map((f) => ({ key: f.key, label: t.fields.find((x) => x.key === f.key).label, alias: f.alias || '', hidden: !!f.hidden }))
  }
}

const esc = (v) => String(v ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])

async function codeSvg(value, cfg) {
  if (!value) return ''
  if (cfg.codeType === 'QR') {
    return QRCode.toString(String(value), { type: 'svg', margin: 0, errorCorrectionLevel: 'M' })
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  try {
    JsBarcode(svg, String(value), { format: 'CODE128', displayValue: cfg.showText, fontSize: 12, textMargin: 1, height: 40, margin: 0, width: 1.6 })
  } catch {
    return `<div class="code-error">${esc($t('wms.label.codeError', { value }))}</div>`
  }
  svg.setAttribute('preserveAspectRatio', 'none')
  return svg.outerHTML
}

/** 一张标签的 HTML */
async function renderLabel(type, data, cfg) {
  const t = LABEL_TYPE_MAP[type]
  const value = t.value(data)
  const lines = cfg.fields.filter((f) => !f.hidden && data[f.key] != null && data[f.key] !== '')
    .map((f, i) => `<div class="${i === 0 ? 'line first' : 'line'}">${f.alias ? `<span class="k">${esc(f.alias)}</span>` : ''}${esc(data[f.key])}</div>`)
    .join('')
  const code = await codeSvg(value, cfg)
  if (cfg.codeType === 'QR') {
    const text = cfg.showText ? `<div class="qr-text">${esc(value)}</div>` : ''
    return `<div class="label qr"><div class="qr-code">${code}${text}</div><div class="texts">${lines}</div></div>`
  }
  return `<div class="label bar"><div class="texts">${lines}</div><div class="bar-code">${code}</div></div>`
}

/** 标签页面的 HTML：每张标签一页（标签纸尺寸） */
export async function renderLabelsHtml(type, list, cfg) {
  const labels = await Promise.all(list.map((d) => renderLabel(type, d, cfg)))
  const qrSize = Math.min(cfg.height - 4, cfg.width * 0.45)
  const style = `
    @page { size: ${cfg.width}mm ${cfg.height}mm; margin: 0; }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; color: #000; }
    .label { width: ${cfg.width}mm; height: ${cfg.height}mm; padding: 2mm; overflow: hidden; page-break-after: always;
      font-size: ${cfg.fontSize}pt; line-height: 1.25; }
    .label:last-child { page-break-after: auto; }
    .line { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .line.first { font-weight: 600; font-size: ${cfg.fontSize + 2}pt; }
    .k { color: #444; margin-right: 1mm; }
    .bar { display: flex; flex-direction: column; }
    .bar .texts { flex: 0 0 auto; }
    .bar-code { flex: 1; min-height: 0; margin-top: 1mm; }
    .bar-code svg { width: 100%; height: 100%; }
    .qr { display: flex; gap: 2mm; align-items: center; }
    .qr-code { flex: 0 0 ${qrSize}mm; text-align: center; }
    .qr-code svg { width: ${qrSize}mm; height: ${qrSize}mm; display: block; }
    .qr-text { font-size: 7pt; word-break: break-all; }
    .qr .texts { flex: 1; min-width: 0; }
    .code-error { color: #c00; font-size: 8pt; }
    @media screen { body { background: #f0f0f0; padding: 8px; } .label { background: #fff; margin-bottom: 8px; box-shadow: 0 0 0 1px #ccc; } }
  `
  return `<!doctype html><html><head><meta charset="utf-8"><title>${esc($t('wms.label.title'))}</title><style>${style}</style></head><body>${labels.join('')}</body></html>`
}

/**
 * 打印标签
 *
 * @param type 标签模板标识（LABEL_TYPES）
 * @param list 标签数据（字段见 LABEL_TYPES.fields，物料还可带 barcode），按需重复即为多份
 */
export async function printLabels(type, list) {
  let saved = {}
  try {
    saved = (await loadTemplates())[type] || {}
  } catch {
    // 模板加载失败时按默认样式打印
  }
  printHtml(await renderLabelsHtml(type, list, labelConfig(type, saved)))
}
