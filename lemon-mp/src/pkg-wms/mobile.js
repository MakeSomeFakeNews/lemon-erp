import { reactive } from 'vue'
import { t } from '@/locales'
import { vibrate } from '@/composables/useAction'
import { materialByBarcode, suggestPutaway } from '@/api/wms'

/**
 * 扫码作业的公共逻辑（与 Web 端 components/mobile/mobile.js、components/wms/wms.js 一致）
 */

/**
 * 扫码解析：先按库位编码匹配当前仓库的库位，再按物料条码 / 包装条码 / 物料编码查物料；都不匹配时由页面按序列号等处理
 *
 * @param locations 当前仓库的库位选项 [{ id, code, name }]
 * @returns {{ type: 'location', location } | { type: 'material', material, unit, ratio } | { type: 'unknown', code }}
 */
export async function resolveScan(code, locations = []) {
  const location = locations.find((l) => l.code === code)
  if (location) return { type: 'location', location }
  const hit = await materialByBarcode(code).catch(() => null)
  if (hit?.material) return { type: 'material', material: hit.material, unit: hit.unit, ratio: Number(hit.ratio) || 1 }
  return { type: 'unknown', code }
}

/** 扫码结果提示：显示在扫码框下方，成功短震、失败长震。模板中 tip.text / tip.ok */
export function useScanTip() {
  const tip = reactive({ text: '', ok: true })
  tip.show = (text, ok = true) => {
    tip.text = text
    tip.ok = ok
    vibrate(ok)
  }
  return tip
}

/** 四位小数舍入（数量累加时避免浮点误差） */
export const qty4 = (n) => Math.round((Number(n) || 0) * 10000) / 10000

/** 可用数量 = 库存 − 冻结 */
export const availableQty = (s) => Number(s.qty || 0) - Number(s.frozenQty || 0)

/** 订单明细的未入库数量（与后端一致：数量 − 已完成 + 退货补发） */
export const openQty = (i) => Math.max(qty4(Number(i.qty) - Number(i.doneQty || 0) + Number(i.replenishQty || 0)), 0)

/** 库位显示：空为未指定 */
export const locationText = (code) => code || t('wms.common.unspecified')

/** 拆分序列号文本（与后端 WmsConstants.splitSerials 一致） */
export function splitSerials(text) {
  if (!text) return []
  return String(text)
    .split(/[,，;；\s]+/)
    .filter(Boolean)
}

let seq = 0
/** 前端新增行的唯一键 */
export const rowKey = () => `new-${Date.now()}-${++seq}`

/**
 * 上架推荐：为没有选库位的行按上架策略（默认库位 → 同批次 → 同物料 → 空库位）填入库位，返回填入的行数
 *
 * @param key 库位字段，调拨的调入库位为 toLocationId
 */
export async function fillPutaway(warehouseId, rows, key = 'locationId') {
  const targets = rows.filter((r) => r.materialId && !r[key])
  if (!warehouseId || !targets.length) return 0
  const res = await suggestPutaway({ warehouseId, lines: targets.map((r) => ({ materialId: r.materialId, batchNo: r.batchNo || null })) })
  let filled = 0
  targets.forEach((r, i) => {
    if (res?.[i]?.locationId) {
      r[key] = res[i].locationId
      filled++
    }
  })
  return filled
}

/** 数字输入框的值转为数字，空为 null；按 [min, max] 截断 */
export function toNumber(v, min = 0, max) {
  if (v === '' || v === null || v === undefined) return null
  let n = Number(v)
  if (!Number.isFinite(n)) return null
  if (n < min) n = min
  if (max !== undefined && max !== null && n > max) n = max
  return qty4(n)
}

/** 选库位的 picker 选项：第一项为空（未指定 / 推荐） */
export function locationRange(locations, emptyLabel) {
  return [{ id: null, label: emptyLabel }, ...locations.map((l) => ({ id: l.id, label: l.name && l.name !== l.code ? `${l.code} ${l.name}` : l.code }))]
}

export function locationIndex(range, id) {
  return Math.max(0, range.findIndex((r) => r.id === (id || null)))
}
