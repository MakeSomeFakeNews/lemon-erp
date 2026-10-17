import { defineStore } from 'pinia'
import { getAuthConfig } from '@/api/auth'
import { t } from '@/locales'

/**
 * 公开的系统配置（/auth/config）：系统名称、登录验证码、密码策略、水印等，进入页面前加载一次，修改参数设置后重新加载
 */
export const useConfigStore = defineStore('config', {
  state: () => ({
    loaded: false,
    tenantEnabled: false,
    systemName: t('print.common.defaultCompany'),
    showDemoAccount: false,
    watermark: false,
    /** OFF / ALWAYS / AFTER_FAILURE */
    captchaMode: 'OFF',
    passwordMinLength: 8,
    passwordComplexity: 'LETTER_DIGIT',
    attachmentMaxMb: 20,
    /** 演示模式：只能查看，不能新增、修改、删除 */
    demoMode: false
  }),
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return
      Object.assign(this, await getAuthConfig(), { loaded: true })
    }
  }
})
