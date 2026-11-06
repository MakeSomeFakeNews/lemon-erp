import { computed, ref } from 'vue'
import { priceLevels as fetchPriceLevels } from '@/api/scm/pricing'
import { STATUS_ENABLED } from '@/utils/constants'

/**
 * 物料的价格列（0 零售价，1-8 批发价1-8）：名称和是否启用来自「价格列设置」（/wms/price-level），页面间共用一份缓存。
 * 停用的价格列在物料表单、列表、导入导出和价格选择中隐藏
 */
export const PRICE_FIELDS = ['retailPrice', ...Array.from({ length: 8 }, (_, i) => `wholesalePrice${i + 1}`)]
/** 价格列对应的物料字段 */
export const priceField = (level) => PRICE_FIELDS[level]

const levels = ref([])
let loading

/** 重新读取价格列设置（修改设置后调用） */
export function reloadPriceLevels() {
  loading = fetchPriceLevels()
    .then((list) => {
      levels.value = list.map((l) => ({ ...l, field: priceField(l.level) }))
      return levels.value
    })
    .catch((e) => {
      loading = undefined
      throw e
    })
  return loading
}

/**
 * @returns {{ levels, enabledLevels, levelName, ready }}
 * levels：全部 9 列 [{ level, name, status, field }]；enabledLevels：启用的列；levelName(level)：列名称
 */
export function usePriceLevels() {
  if (!loading) reloadPriceLevels().catch(() => {})
  const enabledLevels = computed(() => levels.value.filter((l) => l.status === STATUS_ENABLED))
  const levelName = (level) => levels.value.find((l) => l.level === level)?.name ?? ''
  return { levels, enabledLevels, levelName, ready: () => loading }
}
