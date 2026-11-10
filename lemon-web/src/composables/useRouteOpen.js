import { onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * 从其他页面跳转过来时（如应收应付单的来源单号 SourceDocLink）：?id=单据ID 打开详情，?keyword=单号 按单号筛选；
 * 处理后去掉地址中的参数（刷新页面不再重复打开）。页面已打开时再次跳转（只有参数变化）同样处理。
 *
 *   useRouteOpen({ id: (id) => openDetail({ id }) })
 *   useRouteOpen({ keyword: (kw) => (query.keyword = kw), reload: () => search() })
 *
 * keyword 在 setup 时就设置到查询条件（在页面的 onMounted(load) 之前调用 useRouteOpen 不是必须的），页面首次加载即带上条件；
 * 页面已打开时设置后调用 reload 重新查询
 *
 * @param {{ id?: (id: number) => void, keyword?: (keyword: string) => void, reload?: () => void }} handlers
 */
export function useRouteOpen({ id, keyword, reload }) {
  const route = useRoute()
  const router = useRouter()
  const clear = () => router.replace({ query: { ...route.query, id: undefined, keyword: undefined } })

  function applyKeyword() {
    if (!keyword || !route.query.keyword) return false
    keyword(String(route.query.keyword))
    return true
  }
  function openId() {
    if (!id || !Number(route.query.id)) return false
    id(Number(route.query.id))
    return true
  }

  const filtered = applyKeyword()
  onMounted(() => {
    if (openId() || filtered) clear()
  })
  watch(
    () => [route.query.id, route.query.keyword],
    () => {
      const k = applyKeyword()
      if (k) reload?.()
      if (openId() || k) clear()
    }
  )
}
