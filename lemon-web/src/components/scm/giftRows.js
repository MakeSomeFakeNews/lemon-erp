import { pageStock } from '@/api/wms/stock'
import { availableQty, expiryStatus, locationText, rowKey } from '@/components/wms/wms'
import { newTradeRow } from './scm'

/**
 * 直接开单（销售出库）的买赠赠品行：按促销预览的 gifts 同步到明细末尾（gift: true，单价 0），
 * 并按先到期先出自动选择库位批次（可改、可拆行，同一赠品各行数量合计须与促销一致，后端校验）。
 * 后端在请求没有赠品行时也会按同样的规则自动补上
 */

const slotKey = (s) => `${s.locationId || 0}|${s.batchNo || ''}`

/** 仓库中某物料可出库的库位批次，先到期先出（过期的排除） */
async function fefoSlots(warehouseId, materialId) {
  const res = await pageStock({ warehouseId, materialId, current: 1, size: 200 }).catch(() => ({ records: [] }))
  return (res.records || [])
    .filter((s) => availableQty(s) > 0 && expiryStatus(s.expiryDate)?.color !== 'red')
    .sort((a, b) => (a.expiryDate ? 0 : 1) - (b.expiryDate ? 0 : 1) || String(a.expiryDate || '').localeCompare(String(b.expiryDate || '')))
}

const giftRow = (g, extra) => ({
  ...newTradeRow(),
  key: rowKey(),
  gift: true,
  materialId: g.materialId,
  materialLabel: g.materialName,
  materialCode: g.materialCode,
  materialName: g.materialName,
  spec: g.spec,
  unit: g.unit,
  batchEnabled: g.batchEnabled,
  serialEnabled: g.serialEnabled,
  price: 0,
  priceAuto: false,
  promotionName: g.promotionName,
  giftQty: Number(g.qty),
  ...extra
})

/** 按先到期先出拆成一行或多行（库存不够时剩余数量留在最后一行，不选库位，由用户处理） */
async function pickRows(g, warehouseId) {
  const total = Number(g.qty)
  if (!warehouseId || g.serialEnabled === 1) return [giftRow(g, { qty: total })]
  const rows = []
  let left = total
  for (const s of await fefoSlots(warehouseId, g.materialId)) {
    if (left <= 0) break
    const take = Math.min(left, availableQty(s))
    const label = `${locationText(s.locationCode)}${s.batchNo ? ' · ' + s.batchNo : ''}`
    rows.push(giftRow(g, { qty: take, slot: slotKey(s), slotLabel: label, locationId: s.locationId || null, batchNo: s.batchNo || '', available: availableQty(s) }))
    left = Number((left - take).toFixed(4))
  }
  if (left > 0) rows.push(giftRow(g, { qty: left }))
  return rows
}

/**
 * 把赠品同步到明细：数量与促销一致的赠品保留用户选的库位批次，其余重新带出；促销中已没有的赠品行删除
 *
 * @param items 表单明细（直接修改）
 * @param gifts 促销预览的 gifts，为空表示没有赠品
 */
export async function syncGifts(items, gifts, warehouseId) {
  const list = gifts || []
  const keep = new Map()
  for (const g of list) {
    const rows = items.filter((i) => i.gift && i.materialId === g.materialId)
    const sum = rows.reduce((s, i) => s + (Number(i.qty) || 0), 0)
    if (rows.length && Math.abs(sum - list.filter((x) => x.materialId === g.materialId).reduce((s, x) => s + Number(x.qty), 0)) < 1e-9) {
      rows.forEach((r) => (r.promotionName = g.promotionName))
      keep.set(g.materialId, true)
    }
  }
  // 同一赠品物料（多个促销送同一赠品）合并为一项
  const merged = []
  for (const g of list) {
    if (keep.has(g.materialId)) continue
    const m = merged.find((x) => x.materialId === g.materialId)
    if (m) m.qty = Number(m.qty) + Number(g.qty)
    else merged.push({ ...g })
  }
  const added = (await Promise.all(merged.map((g) => pickRows(g, warehouseId)))).flat()
  for (let i = items.length - 1; i >= 0; i--) {
    if (items[i].gift && !keep.has(items[i].materialId)) items.splice(i, 1)
  }
  items.push(...added)
}

/** 赠品的应赠数量（同一物料合计），用于提示 */
export const giftExpected = (gifts, materialId) =>
  (gifts || []).filter((g) => g.materialId === materialId).reduce((s, g) => s + Number(g.qty), 0)
