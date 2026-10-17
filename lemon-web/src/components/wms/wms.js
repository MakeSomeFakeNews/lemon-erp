import dayjs from 'dayjs'
import { suggestPutaway } from '@/api/wms/location'
import { currentLocale, t } from '@/locales'

/** 出入库单的种类（与后端 StockBizType 的路径一致） */
export const DOC_KINDS = {
  in: { label: t('wms.doc.kind.in'), typeLabel: t('wms.doc.kind.inType'), typeDict: 'wms_in_type', warehouseLabel: t('wms.doc.kind.inWarehouse') },
  out: { label: t('wms.doc.kind.out'), typeLabel: t('wms.doc.kind.outType'), typeDict: 'wms_out_type', warehouseLabel: t('wms.doc.kind.outWarehouse') },
  transfer: { label: t('wms.doc.kind.transfer'), warehouseLabel: t('wms.doc.kind.fromWarehouse') }
}

/** 数量：去掉多余的 0，千分位 */
/** 可用数量：库存 - 冻结（待检、不合格） */
export const availableQty = (s) => Number(s.qty || 0) - Number(s.frozenQty || 0)

export function formatQty(n) {
  if (n === null || n === undefined || n === '') return '-'
  return Number(n).toLocaleString(currentLocale(), { maximumFractionDigits: 4 })
}

/** 已完成、非业务单据生成、非红字、未冲销的单据可以冲销 */
export const canReverse = (d) => d.status === 'DONE' && !d.sourceNo && !d.redOf && d.reversed !== 1

/** 负数（红字单据）标红 */
export const negativeClass = (n) => (Number(n) < 0 ? 'qty-negative' : undefined)

/** 库位显示：0 / 空为未指定 */
export const locationText = (code) => code || t('wms.common.unspecified')

/** 拆分序列号文本（与后端 WmsConstants.splitSerials 一致） */
export function splitSerials(text) {
  if (!text) return []
  return String(text)
    .split(/[,，;；\s]+/)
    .filter(Boolean)
}

export const joinSerials = (list) => (list?.length ? list.join(',') : undefined)

/** 有效期状态：已过期 / 30 天内到期 */
export function expiryStatus(date) {
  if (!date) return null
  const days = dayjs(date).diff(dayjs().startOf('day'), 'day')
  if (days < 0) return { color: 'red', text: t('wms.common.expired') }
  if (days <= 30) return { color: 'orange', text: t('wms.common.expireInDays', { days }) }
  return null
}

export const today = () => dayjs().format('YYYY-MM-DD')

/** 明细行的唯一 key（新增行没有 id） */
let seq = 0
export const rowKey = () => `row-${Date.now()}-${++seq}`

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
    if (res[i]?.locationId) {
      r[key] = res[i].locationId
      filled++
    }
  })
  return filled
}
