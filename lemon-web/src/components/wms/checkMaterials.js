import dayjs from 'dayjs'
import { checkMaterialItems, pageCheckMaterials } from '@/api/wms/check'
import { t } from '@/locales'

/** 列表分页上限（后端 MybatisPlusConfig.MAX_PAGE_SIZE），取全部符合条件的物料时按此分批 */
const BATCH = 500
/** 盘点单明细上限（后端 CheckRequest.items） */
export const MAX_CHECK_LINES = 2000

const fmt = (d) => d.format('YYYY-MM-DD')
const monday = (d) => d.subtract((d.day() + 6) % 7, 'day')

/** 动态盘点的变动日期快捷选项：今天（日终盘点）、昨天（次日早上盘点）、本周、本月 */
export function changeRanges() {
  const now = dayjs()
  const yesterday = now.subtract(1, 'day')
  return [
    { key: 'today', label: t('wms.checkPicker.today'), range: [fmt(now), fmt(now)] },
    { key: 'yesterday', label: t('wms.checkPicker.yesterday'), range: [fmt(yesterday), fmt(yesterday)] },
    { key: 'week', label: t('wms.checkPicker.thisWeek'), range: [fmt(monday(now)), fmt(now)] },
    { key: 'month', label: t('wms.checkPicker.thisMonth'), range: [fmt(now.startOf('month')), fmt(now)] }
  ]
}

/** a-range-picker 的 presets */
export const changePresets = () => changeRanges().map((r) => ({ label: r.label, value: r.range.map((d) => dayjs(d)) }))

/**
 * 取全部符合条件的物料 ID（每次 500 条分批取）。超过 limit 时不再继续，返回 { ids, total }，由调用方提示缩小范围
 */
export async function fetchAllMaterialIds(params, limit = MAX_CHECK_LINES) {
  const ids = []
  let total = 0
  for (let current = 1; ; current++) {
    const res = await pageCheckMaterials({ ...params, current, size: BATCH })
    total = res.total
    if (total > limit) return { ids: [], total }
    ids.push(...res.records.map((r) => r.id))
    if (res.records.length < BATCH || ids.length >= total) return { ids, total }
  }
}

/** 物料转为盘点明细（带出账面数量），每次 500 个分批请求 */
export async function materialItems(warehouseId, materialIds) {
  const rows = []
  for (let i = 0; i < materialIds.length; i += BATCH) {
    rows.push(...(await checkMaterialItems({ warehouseId, materialIds: materialIds.slice(i, i + BATCH) })))
  }
  return rows
}
