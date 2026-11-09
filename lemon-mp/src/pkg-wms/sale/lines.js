import { t } from '@/locales'
import { materialUnits, pageStock } from '@/api/wms'
import { qty, today } from '@/utils/format'
import { availableQty, locationText, qty4 } from '../mobile'

/**
 * 快速开单的明细行（与 Web 端 OrderForm 的辅助单位处理一致）：
 * 行上的 qty / price 是所选单位（unitName，空为基本单位）的数量和含税单价，提交、默认价、促销、库存比较都折算为基本单位
 * （基本数量 = qty × 换算率，基本单价 = price ÷ 换算率）。序列号物料只按基本单位录入（数量 = 序列号个数）
 */

/** 所选单位的换算率，基本单位为 1 */
export const ratioOf = (row) => (row.unitName ? Number(row.unitRatio) || 1 : 1)

/** 基本单位数量 */
export const baseQty = (row) => qty4((Number(row.qty) || 0) * ratioOf(row))

/** 基本单位含税单价（四位小数，与后端保存的单价一致） */
export const basePrice = (row) => (row.price == null ? null : qty4(Number(row.price) / ratioOf(row)))

/** 行金额：按后端的口径（基本数量 × 基本单价，四舍五入到分） */
export const lineAmount = (row) => Math.round(baseQty(row) * (Number(basePrice(row)) || 0) * 100) / 100

/** 单位选项：第一项为基本单位 */
export function unitRange(row) {
  return [
    { unit: '', ratio: 1, label: row.unit || t('wms.sale.baseUnit') },
    ...(row.units || []).map((u) => ({ ...u, label: t('wms.sale.unitOption', { unit: u.unit, ratio: `${qty(u.ratio)}${row.unit || ''}` }) }))
  ]
}

/** 切换录入单位：数量、单价按新单位折算（基本数量、基本单价不变） */
export function changeUnit(row, unit) {
  const base = baseQty(row)
  const price = basePrice(row)
  row.unitName = unit?.unit || null
  row.unitRatio = unit?.unit ? Number(unit.ratio) || 1 : 1
  row.qty = qty4(base / row.unitRatio)
  if (price != null) row.price = qty4(price * row.unitRatio)
}

/** 物料的辅助单位：materialId → Promise<[{ unit, ratio }]>，页面打开期间缓存，失败时不缓存 */
const unitCache = new Map()
export function loadUnits(materialId) {
  if (!unitCache.has(materialId)) {
    unitCache.set(
      materialId,
      materialUnits(materialId)
        .then((list) => (list || []).map((u) => ({ unit: u.unit, ratio: Number(u.ratio) })).filter((u) => u.unit && u.ratio > 0))
        .catch(() => {
          unitCache.delete(materialId)
          return []
        })
    )
  }
  return unitCache.get(materialId)
}

export const clearUnitCache = () => unitCache.clear()

/**
 * 库位 / 批次：指定仓库中该物料的可用库存，先到期先出（过期的排最后），默认第一项；
 * keep 为原来选中的 { locationId, batchNo }，仍有库存时保持选中
 */
export async function loadSlots(row, warehouseId, keep) {
  row.slots = []
  row.slotIndex = 0
  if (!warehouseId) return
  row.slotsLoading = true
  try {
    const res = await pageStock({ warehouseId, materialId: row.materialId, current: 1, size: 200 })
    const todayStr = today()
    const rank = (s) => (s.expiryDate && s.expiryDate < todayStr ? 2 : s.expiryDate ? 0 : 1)
    row.slots = (res.records || [])
      .filter((s) => availableQty(s) > 0)
      .sort((a, b) => rank(a) - rank(b) || String(a.expiryDate || '').localeCompare(String(b.expiryDate || '')))
      .map((s) => ({
        locationId: s.locationId || null,
        batchNo: s.batchNo || null,
        available: availableQty(s),
        label: [locationText(s.locationCode), s.batchNo, t('wms.common.availableN', { n: qty(availableQty(s)) })].filter(Boolean).join(' · ')
      }))
    if (keep) row.slotIndex = Math.max(0, row.slots.findIndex((s) => s.locationId === keep.locationId && s.batchNo === keep.batchNo))
  } catch {
    // 错误已提示
  } finally {
    row.slotsLoading = false
  }
}

/** 所选库位批次的可用数量不足（按基本单位比较） */
export const overStock = (row) => baseQty(row) > (row.slots[row.slotIndex]?.available || 0)
