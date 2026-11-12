import { ref } from 'vue'
import { qty4 } from '../mobile'

/**
 * 快速开单的买赠赠品行：按促销预览生成；业务参数 scm.salesOut.lineWarehouse 开启时同一赠品可拆成多行，
 * 各选仓库、库位批次，数量合计等于促销算出的赠品数量（后端按赠品物料合计校验）
 *
 * @param newRow      新明细行 (material, extra) => row
 * @param reloadSlots 按行的实际仓库加载库位批次
 * @param activeRow   当前选中的行（扫序列号用）
 */
export function useGiftRows({ newRow, reloadSlots, activeRow }) {
  const giftRows = ref([])

  /**
   * 促销预览结果；赠品行按预览重建（数量为基本单位）。同一促销的同一赠品是一组（giftKey，合计 giftTotal），
   * 可拆成多行各选仓库、库位批次；重新预览时保留已有的行，数量差额调整到第一行（不够扣时合并为一行）。
   * 没有赠品行提交时后端从表头仓库按先到期先出自动补上（序列号赠品除外）
   */
  function applyGifts(gifts) {
    const old = giftRows.value
    const next = []
    ;(gifts || []).filter((g) => Number(g.qty) > 0).forEach((g) => {
      const promotionId = g.promotionId ?? null
      const key = `${promotionId}-${g.materialId}`
      const total = Number(g.qty)
      const group = old.filter((r) => r.giftKey === key)
      if (group.length) {
        const rest = qty4(group.slice(1).reduce((sum, r) => sum + (Number(r.qty) || 0), 0))
        const kept = rest <= total ? group : group.slice(0, 1)
        kept.forEach((r) => (r.giftTotal = total))
        kept[0].qty = qty4(total - (kept.length > 1 ? rest : 0))
        kept.forEach(trimSerials)
        next.push(...kept)
        return
      }
      const name = String(g.materialName || '')
      const code = g.materialCode || ''
      const m = {
        id: g.materialId, code, name: code && name.startsWith(`${code} `) ? name.slice(code.length + 1) : name,
        unit: g.unit, spec: g.spec, batchEnabled: g.batchEnabled, serialEnabled: g.serialEnabled, isVirtual: g.isVirtual
      }
      next.push(newRow(m, { gift: true, qty: total, price: 0, priceAuto: false, promotionId, promotionName: g.promotionName || null, giftKey: key, giftTotal: total }))
    })
    giftRows.value = next
    // 新赠品行加载库位批次：要通过响应式代理修改，加载结果才会刷新页面
    giftRows.value.filter((r) => !old.includes(r)).forEach(reloadSlots)
    if (activeRow.value?.gift && !giftRows.value.includes(activeRow.value)) activeRow.value = null
  }

  const trimSerials = (r) => r.serials.length > Number(r.qty) && (r.serials = r.serials.slice(0, Math.max(Number(r.qty), 0)))
  const giftGroup = (row) => giftRows.value.filter((r) => r.giftKey === row.giftKey)

  /**
   * 拆分赠品（明细可选仓库时）：在本行之后加一行同一赠品，数量取本行在本仓库的缺口（库存够时为 0，自行调整），
   * 本行相应减少；新行默认同表头仓库，选择其他仓库后加载库位批次
   */
  function splitGift(row) {
    const available = row.slots.reduce((sum, s) => sum + s.available, 0)
    const move = qty4(Math.max(Number(row.qty) - available, 0))
    row.qty = qty4(Number(row.qty) - move)
    trimSerials(row)
    const added = { ...newRow({ id: row.materialId, code: row.materialCode, name: row.materialName, unit: row.unit, spec: row.spec,
      batchEnabled: row.batchEnabled, serialEnabled: row.serialEnabled, isVirtual: row.isVirtual }), gift: true, qty: move, price: 0, priceAuto: false,
      promotionId: row.promotionId, promotionName: row.promotionName, giftKey: row.giftKey, giftTotal: row.giftTotal }
    const list = [...giftRows.value]
    list.splice(list.indexOf(row) + 1, 0, added)
    giftRows.value = list
    const proxy = giftRows.value.find((r) => r.key === added.key)
    activeRow.value = proxy
    reloadSlots(proxy)
  }

  /** 拆分行之间调整数量：改了某一行后，差额由同组第一行（改的就是第一行时由第二行）承担 */
  function changeGiftQty(row, v) {
    const group = giftGroup(row)
    const target = group[0] === row ? group[1] : group[0]
    const others = qty4(group.filter((r) => r !== row && r !== target).reduce((sum, r) => sum + (Number(r.qty) || 0), 0))
    const n = Math.min(Math.max(qty4(Number(v) || 0), 0), qty4(row.giftTotal - others))
    row.qty = n
    if (target) target.qty = qty4(row.giftTotal - others - n)
    group.forEach(trimSerials)
  }

  /** 删除拆分行：数量并回同组第一行 */
  function removeGiftSplit(row) {
    const group = giftGroup(row)
    if (group[0] === row) return
    group[0].qty = qty4(Number(group[0].qty) + (Number(row.qty) || 0))
    giftRows.value = giftRows.value.filter((r) => r !== row)
    if (activeRow.value === row) activeRow.value = null
  }

  /** 各组赠品数量合计与促销数量不一致的第一行，都一致时为空 */
  const mismatchRow = () =>
    giftRows.value.find((r) => qty4(giftGroup(r).reduce((s, x) => s + (Number(x.qty) || 0), 0)) !== qty4(r.giftTotal))

  return { giftRows, applyGifts, giftGroup, splitGift, changeGiftQty, removeGiftSplit, mismatchRow }
}
