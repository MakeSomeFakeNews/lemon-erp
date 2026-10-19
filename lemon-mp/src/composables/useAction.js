import { t } from '@/locales'

/**
 * 单条操作：确认 → 调接口 → 提示 → 回调
 *
 *   await runAction(() => approveTask(id, data), { confirm: '…', success: '…', then: reload })
 * 返回是否执行成功
 */
export async function runAction(action, { confirm, success, then } = {}) {
  if (confirm) {
    const ok = await new Promise((resolve) =>
      uni.showModal({ title: t('common.tip'), content: confirm, confirmText: t('common.ok'), cancelText: t('common.cancel'), success: (r) => resolve(r.confirm) })
    )
    if (!ok) return false
  }
  uni.showLoading({ title: t('common.processing'), mask: true })
  try {
    await action()
  } catch {
    // 错误已由 request 提示
    return false
  } finally {
    uni.hideLoading()
  }
  // 带图标的提示只能显示约 7 个汉字，文字较长时不带图标
  const title = success || t('common.success')
  uni.showToast({ title, icon: title.length > 7 ? 'none' : 'success' })
  if (then) await then()
  return true
}

/** 扫码（摄像头）；用户取消时返回 null */
export function scanCode() {
  return new Promise((resolve) => {
    uni.scanCode({
      onlyFromCamera: false,
      success: (r) => resolve((r.result || '').trim() || null),
      fail: () => resolve(null)
    })
  })
}

/** 扫码反馈：成功短震一次，失败长震 */
export function vibrate(ok = true) {
  if (ok) uni.vibrateShort({ type: 'medium', fail: () => {} })
  else uni.vibrateLong({ fail: () => {} })
}
