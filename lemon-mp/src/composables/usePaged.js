import { reactive } from 'vue'

/**
 * 下拉刷新 + 触底加载的分页列表。fetcher 接收 { current, size }，返回 { records, total }（与后端分页接口一致）
 *
 *   const paged = usePaged((p) => pageCustomers({ ...p, keyword: keyword.value }))
 *   onLoad(() => paged.refresh()); onPullDownRefresh(() => paged.refresh()); onReachBottom(() => paged.more())
 *   模板：paged.list、<l-load-more :status="paged.status" />
 */
export function usePaged(fetcher, size = 20) {
  const state = reactive({
    list: [],
    total: 0,
    current: 0,
    /** loading | more | noMore */
    status: 'more',
    loaded: false
  })

  async function fetch(page) {
    state.status = 'loading'
    try {
      const res = await fetcher({ current: page, size })
      const records = res?.records || []
      state.list = page === 1 ? records : state.list.concat(records)
      state.total = res?.total ?? state.list.length
      state.current = page
      state.status = state.list.length >= state.total || records.length < size ? 'noMore' : 'more'
    } catch (e) {
      state.status = 'more'
      throw e
    } finally {
      state.loaded = true
    }
  }

  state.refresh = () => fetch(1).catch(() => {}).finally(() => uni.stopPullDownRefresh())
  state.more = () => (state.status === 'more' ? fetch(state.current + 1).catch(() => {}) : Promise.resolve())
  return state
}
