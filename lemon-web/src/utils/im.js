/** 第三方集成的平台：与后端 ImPlatform 一致 */
export const IM_PLATFORMS = [
  { value: 'DINGTALK', label: '钉钉', short: '钉', color: '#1677ff' },
  { value: 'WECOM', label: '企业微信', short: '企', color: '#07c160' },
  { value: 'FEISHU', label: '飞书', short: '飞', color: '#00b2a9' }
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
