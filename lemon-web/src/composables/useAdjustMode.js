import { ref, watch } from 'vue'
import { AdjustMode } from '@/constants/enums'

/**
 * 毛利明细中调整项（整单优惠、本公司运费、收付款折让、坏账核销）的显示方式（issue #93）：汇总到底部（默认）/ 分摊到各行。
 * 销售报表、进货报表、经营分析的明细共用一个设置，记在本机浏览器（只是查看习惯，不影响数据）
 */
const KEY = 'lemon.adjustMode'

function load() {
  try {
    const v = localStorage.getItem(KEY)
    return Object.values(AdjustMode).includes(v) ? v : AdjustMode.BOTTOM
  } catch {
    return AdjustMode.BOTTOM
  }
}

const mode = ref(load())
watch(mode, (v) => {
  try {
    localStorage.setItem(KEY, v)
  } catch {
    // 无法使用本地存储时只在本次会话内生效
  }
})

export function useAdjustMode() {
  return mode
}
