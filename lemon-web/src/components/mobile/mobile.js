import { materialByBarcode } from '@/api/wms/material'

/**
 * 移动作业的扫码解析：先按库位编码匹配当前仓库的库位，再按物料条码 / 包装条码 / 物料编码查物料；
 * 都不匹配时由页面按序列号等处理
 *
 * @param locations 当前仓库的库位选项 [{ id, code, name }]
 * @returns {{ type: 'location', location } | { type: 'material', material, unit, ratio } | { type: 'unknown', code }}
 */
export async function resolveScan(code, locations = []) {
  const location = locations.find((l) => l.code === code)
  if (location) return { type: 'location', location }
  const hit = await materialByBarcode(code)
  if (hit?.material) return { type: 'material', material: hit.material, unit: hit.unit, ratio: Number(hit.ratio) || 1 }
  return { type: 'unknown', code }
}

/** 扫码反馈：成功短震一次，失败震两次（设备支持时） */
export function feedback(ok = true) {
  try {
    navigator.vibrate?.(ok ? 60 : [80, 60, 80])
  } catch {
    // 不支持振动时忽略
  }
}
