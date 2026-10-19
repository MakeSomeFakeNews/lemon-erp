import { t } from '@/locales'

/** 底部导航文字按界面语言设置（pages.json 中只能写一种语言） */
const TABS = ['common.tab.workbench', 'common.tab.approval', 'common.tab.message', 'common.tab.mine']

export function applyTabBar() {
  TABS.forEach((key, index) => uni.setTabBarItem({ index, text: t(key), fail: () => {} }))
}

/** 消息未读数显示在「消息」上 */
export function setMessageBadge(count) {
  if (count > 0) uni.setTabBarBadge({ index: 2, text: count > 99 ? '99+' : String(count), fail: () => {} })
  else uni.removeTabBarBadge({ index: 2, fail: () => {} })
}
