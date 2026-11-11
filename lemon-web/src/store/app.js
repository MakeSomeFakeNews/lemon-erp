import { defineStore } from 'pinia'
import { load, save } from '@/utils/storage'
import { currentLocale, savedLocale, setLocale, t } from '@/locales'
import { getToken } from '@/utils/auth'
import { saveUiSettings } from '@/api/profile'
import { DEFAULT_SETTINGS, KEEP_ON_RESET, ThemeMode, isDefaultSettings, normalizeSettings } from '@/utils/uiSettings'
import { useConfigStore } from './config'

/** 界面设置的本地缓存（旧版本只保存 { dark, collapsed }，读取时兼容） */
const SETTINGS_KEY = 'lemon_erp_settings'
const TABS_KEY = 'lemon_erp_tabs'
/** 修改界面设置后延迟保存到服务端，连续调整（如拖动取色）只保存一次 */
const SYNC_DELAY = 800
const SYNC_URL = '/profile/settings/ui'

export const HOME_TAB = { path: '/dashboard', fullPath: '/dashboard', title: t('route.dashboard'), icon: 'HomeOutlined', affix: true }

const darkMedia = window.matchMedia?.('(prefers-color-scheme: dark)')

// 登录并应用了服务端的设置之后才同步（登录页的修改只保存在本地，登录时没有服务端设置的再上传）
let syncing = false
let syncTimer = null

/**
 * 界面状态：界面设置（主题、主题色、布局等，每个用户一份，服务端为准、本地缓存）、多标签页
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    settings: normalizeSettings(load(SETTINGS_KEY, null)),
    /** 系统是否为深色（主题模式为「跟随系统」时使用） */
    systemDark: darkMedia?.matches ?? false,
    tabs: load(TABS_KEY, [HOME_TAB]),
    // 刷新当前页时临时卸载 router-view
    reloading: false
  }),
  getters: {
    dark: (s) => (s.settings.themeMode === ThemeMode.SYSTEM ? s.systemDark : s.settings.themeMode === ThemeMode.DARK),
    collapsed: (s) => s.settings.collapsed,
    layout: (s) => s.settings.layout
  },
  actions: {
    /** 修改一项界面设置：立即生效、写入本地缓存，稍后保存到服务端 */
    setSetting(key, value) {
      if (this.settings[key] === value) return
      this.settings[key] = value
      this.persistSettings()
    },
    resetSettings() {
      const keep = Object.fromEntries(KEEP_ON_RESET.filter((k) => k in this.settings).map((k) => [k, this.settings[k]]))
      this.settings = { ...DEFAULT_SETTINGS, ...keep }
      this.persistSettings()
    },
    persistSettings() {
      save(SETTINGS_KEY, this.settings)
      if (!syncing) return
      clearTimeout(syncTimer)
      syncTimer = setTimeout(() => this.syncSettings(), SYNC_DELAY)
    },
    /** 立即保存到服务端（演示模式下所有账号共用，只保存在本地） */
    async syncSettings() {
      clearTimeout(syncTimer)
      syncTimer = null
      if (!syncing || useConfigStore().demoMode) return
      await saveUiSettings({ ...this.settings })
    },
    /** 有尚未保存的修改时立即保存（退出登录前） */
    async flushSettings() {
      if (syncTimer) await this.syncSettings().catch(() => {})
    },
    /** 页面关闭、刷新时还没到保存时间的修改：用 keepalive 请求发出，页面卸载后浏览器继续发送 */
    flushSettingsOnUnload() {
      if (!syncTimer || !syncing || useConfigStore().demoMode) return
      clearTimeout(syncTimer)
      syncTimer = null
      try {
        fetch(`/api${SYNC_URL}`, {
          method: 'PUT',
          keepalive: true,
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${getToken()}` },
          body: JSON.stringify(this.settings)
        })
      } catch {
        /* ignore */
      }
    },
    /**
     * 登录后（加载用户信息时）应用服务端保存的设置。服务端没有时，把本地的设置（含旧版本的本地设置）上传；
     * 语言：本浏览器没有选择过时采用服务端保存的语言（需要刷新页面），选择过的以本浏览器为准
     */
    applyServerSettings(raw) {
      syncing = true
      if (raw == null) {
        const locale = savedLocale()
        if (locale) this.settings.locale = locale
        if (!isDefaultSettings(this.settings) || locale) this.syncSettings().catch(() => {})
        return
      }
      this.settings = normalizeSettings(raw)
      save(SETTINGS_KEY, this.settings)
      const locale = this.settings.locale
      if (locale && !savedLocale() && locale !== currentLocale()) setLocale(locale)
    },
    /** 退出登录：停止同步，本地缓存保留（登录页沿用上次的外观） */
    stopSync() {
      syncing = false
      clearTimeout(syncTimer)
      syncTimer = null
    },
    /** 切换语言：已登录时先保存到服务端，再刷新页面 */
    async changeLocale(locale) {
      if (syncing) {
        this.settings.locale = locale
        save(SETTINGS_KEY, this.settings)
        await this.syncSettings().catch(() => {})
      }
      setLocale(locale)
    },
    toggleDark() {
      this.setSetting('themeMode', this.dark ? ThemeMode.LIGHT : ThemeMode.DARK)
    },
    toggleCollapsed() {
      this.setSetting('collapsed', !this.settings.collapsed)
    },

    /** 路由加载后按当前语言刷新标签标题（标签保存的是上次显示的标题） */
    retitleTabs(titleOf) {
      this.tabs.forEach((tab) => {
        const title = tab.path === HOME_TAB.path ? HOME_TAB.title : titleOf(tab.path)
        if (title) tab.title = title
      })
      this.persistTabs()
    },
    persistTabs() {
      save(TABS_KEY, this.tabs)
    },
    addTab(route) {
      if (!route.meta?.title) return
      const exist = this.tabs.find((t) => t.path === route.path)
      if (exist) {
        exist.fullPath = route.fullPath
        exist.title = route.meta.title
      } else {
        this.tabs.push({ path: route.path, fullPath: route.fullPath, title: route.meta.title, icon: route.meta.icon })
      }
      this.persistTabs()
    },
    /**
     * 关闭标签，返回关闭后应跳转的 fullPath（关闭的不是当前页时返回 null）
     */
    closeTab(path, activePath) {
      const idx = this.tabs.findIndex((t) => t.path === path)
      if (idx < 0 || this.tabs[idx].affix) return null
      this.tabs.splice(idx, 1)
      this.persistTabs()
      if (path !== activePath) return null
      return (this.tabs[idx] || this.tabs[idx - 1] || HOME_TAB).fullPath
    },
    /** mode: others | left | right | all */
    closeTabs(mode, path) {
      const idx = this.tabs.findIndex((t) => t.path === path)
      this.tabs = this.tabs.filter((t, i) => {
        if (t.affix) return true
        if (mode === 'others') return i === idx
        if (mode === 'left') return i >= idx
        if (mode === 'right') return i <= idx
        return false
      })
      this.persistTabs()
    },
    resetTabs() {
      this.tabs = [HOME_TAB]
      this.persistTabs()
    },
    async reloadPage() {
      this.reloading = true
      await new Promise((r) => setTimeout(r, 50))
      this.reloading = false
    }
  }
})
