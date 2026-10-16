import { defineStore } from 'pinia'
import { load, save } from '@/utils/storage'

const SETTINGS_KEY = 'lemon_erp_settings'
const TABS_KEY = 'lemon_erp_tabs'

export const HOME_TAB = { path: '/dashboard', fullPath: '/dashboard', title: '首页', icon: 'HomeOutlined', affix: true }

const prefersDark = () => window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false

/**
 * 界面状态：主题、侧边栏折叠、多标签页
 */
export const useAppStore = defineStore('app', {
  state: () => {
    const settings = load(SETTINGS_KEY, {})
    return {
      dark: settings.dark ?? prefersDark(),
      collapsed: settings.collapsed ?? false,
      tabs: load(TABS_KEY, [HOME_TAB]),
      // 刷新当前页时临时卸载 router-view
      reloading: false
    }
  },
  actions: {
    persistSettings() {
      save(SETTINGS_KEY, { dark: this.dark, collapsed: this.collapsed })
    },
    toggleDark() {
      this.dark = !this.dark
      this.persistSettings()
    },
    toggleCollapsed() {
      this.collapsed = !this.collapsed
      this.persistSettings()
    },

    persistTabs() {
      save(TABS_KEY, this.tabs)
    },
    addTab(route) {
      if (!route.meta?.title) return
      const exist = this.tabs.find((t) => t.path === route.path)
      if (exist) {
        exist.fullPath = route.fullPath
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
