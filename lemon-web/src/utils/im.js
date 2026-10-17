import { t } from '@/locales'

/** 第三方集成的平台：与后端 ImPlatform 一致 */
export const IM_PLATFORMS = [
  { value: 'DINGTALK', label: t('components.im.dingtalk'), short: t('components.im.dingtalkShort'), color: '#1677ff' },
  { value: 'WECOM', label: t('components.im.wecom'), short: t('components.im.wecomShort'), color: '#07c160' },
  { value: 'FEISHU', label: t('components.im.feishu'), short: t('components.im.feishuShort'), color: '#00b2a9' }
]

export const imPlatform = (value) => IM_PLATFORMS.find((p) => p.value === value) || { value, label: value, short: '?', color: '#999' }

/** 当前是否在平台客户端内打开（客户端内用静默授权） */
export function detectImApp() {
  const ua = navigator.userAgent
  if (/DingTalk/i.test(ua)) return 'DINGTALK'
  if (/wxwork/i.test(ua)) return 'WECOM'
  if (/Lark|Feishu/i.test(ua)) return 'FEISHU'
  return null
}
