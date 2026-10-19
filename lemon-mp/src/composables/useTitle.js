import { onShow } from '@dcloudio/uni-app'
import { t } from '@/locales'

/** 导航栏标题按界面语言设置（pages.json 中只能写一种语言）：useTitle('approval.detailTitle') */
export function useTitle(key, params) {
  onShow(() => uni.setNavigationBarTitle({ title: t(key, typeof params === 'function' ? params() : params) }))
}
