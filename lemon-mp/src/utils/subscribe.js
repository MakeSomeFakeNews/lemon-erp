import { get } from './request'
import { t } from '@/locales'

/**
 * 微信订阅消息（审批提醒）：后端在有新待办时推送，需要用户授权，每次授权可推送一条。
 * 微信要求在用户点击的事件中同步调用 requestSubscribe（不能在 await 之后），所以模板 ID 在启动时预先加载；
 * 用户勾选「总是保持以上选择」后，之后的申请不再弹窗，直接按选择累计次数
 */
let templateIds = []

export function loadSubscribeConfig() {
  // #ifdef MP-WEIXIN
  get('/auth/miniapp/config', null, { silent: true })
    .then((c) => (templateIds = c?.subscribeTemplateIds || []))
    .catch(() => {})
  // #endif
}

/** 是否配置了订阅消息（「我的」中显示入口） */
export const subscribeAvailable = () => templateIds.length > 0

/**
 * 申请订阅授权，必须在点击事件中同步调用
 *
 * @param feedback 显示结果提示（「我的 - 审批提醒」中主动开启时）
 */
export function requestSubscribe(feedback = false) {
  // #ifdef MP-WEIXIN
  if (!templateIds.length) return
  uni.requestSubscribeMessage({
    tmplIds: templateIds,
    success: (res) => {
      if (!feedback) return
      const accepted = templateIds.some((id) => res[id] === 'accept')
      uni.showToast({ title: accepted ? t('common.subscribe.accepted') : t('common.subscribe.rejected'), icon: 'none' })
    },
    fail: () => {
      if (feedback) uni.showToast({ title: t('common.subscribe.failed'), icon: 'none' })
    }
  })
  // #endif
}
