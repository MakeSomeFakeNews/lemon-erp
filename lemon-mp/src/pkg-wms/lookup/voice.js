import { isZh } from '@/locales'

/**
 * 语音输入（按住说话 → 识别为文字）。
 *
 * 小程序本身没有语音识别 API，可用的方式：
 * 1. 输入法自带的语音输入（微信键盘、系统输入法的麦克风）：不需要任何配置，搜索框聚焦后即可使用，这是默认方式；
 * 2. 微信官方「同声传译」插件（WechatSI，AppID wx069ba97219f66d99）：在小程序管理后台「设置 - 第三方设置 - 插件管理」中添加，
 *    并在 pages.json 的 pkg-wms 分包中声明 "plugins": { "WechatSI": { "version": "0.3.6", "provider": "wx069ba97219f66d99" } }
 *    （见 lemon-mp/README.md）。声明后搜索框旁出现「按住说话」按钮；没有声明时 requirePlugin 失败，按钮不显示。
 * 插件没有在后台添加就声明会导致小程序无法编译、预览，所以默认不声明
 */
let plugin = null
// #ifdef MP-WEIXIN
try {
  // eslint-disable-next-line no-undef
  plugin = typeof requirePlugin === 'function' ? requirePlugin('WechatSI') : null
} catch (e) {
  plugin = null
}
// #endif

/** 是否可以使用插件识别（否则用输入法的语音输入） */
export const voiceAvailable = () => !!plugin?.getRecordRecognitionManager

let manager = null

/**
 * 开始录音识别；松开时调用 stopVoice。识别结果（去掉句末标点）通过 onResult 返回，失败时 onError(消息键)
 */
export function startVoice({ onResult, onError }) {
  if (!voiceAvailable()) return false
  if (!manager) manager = plugin.getRecordRecognitionManager()
  manager.onStop = (res) => {
    const text = String(res?.result || '').replace(/[。.，,？?！!\s]+$/, '')
    if (text) onResult(text)
    else onError('lookup.voiceFail')
  }
  manager.onError = (res) => onError(res?.retcode === -30001 || /auth/i.test(res?.msg || '') ? 'lookup.voiceDenied' : 'lookup.voiceFail')
  manager.start({ lang: isZh() ? 'zh_CN' : 'en_US', duration: 30000 })
  return true
}

export function stopVoice() {
  manager?.stop()
}
