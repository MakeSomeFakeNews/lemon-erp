import { onBeforeUnmount, ref } from 'vue'

/**
 * 远程搜索下拉的数据：客户、供应商、物料、员工等可能上千条的资料不一次性加载。
 * 打开时取第一页，输入时防抖搜索，滚动到底部加载下一页；已选值不在结果中时按 ID 取名称回显。
 *
 * @param fetch       ({ keyword, current, size }) => 数组，或 { records, total }（分页接口）
 * @param options.pageSize   每页条数，默认 20（下拉一屏约 8 行）
 * @param options.fetchByIds (ids) => 数组，按 ID 取已选的记录；不传时无法回显的值显示 label 或 ID
 */
export function useRemoteOptions(fetch, { pageSize = 20, delay = 300, fetchByIds } = {}) {
  /** 当前关键字的结果（已加载的各页） */
  const list = ref([])
  /** 按 ID 取到的已选记录、选中过的记录：搜索其他关键字后仍能回显 */
  const known = ref(new Map())
  const loading = ref(false)
  const hasMore = ref(false)
  const keyword = ref('')
  let seq = 0
  /** 数据来源切换（reset）的次数：切换前发出的按 ID 取名称请求结果作废 */
  let epoch = 0
  let page = 1
  let loaded = false
  let timer

  async function load(kw = '', more = false) {
    const current = ++seq
    if (!more) {
      keyword.value = (kw || '').trim()
      page = 1
    }
    const target = more ? page + 1 : 1
    loading.value = true
    try {
      const res = await fetch({ keyword: keyword.value || undefined, current: target, size: pageSize })
      if (current !== seq) return null
      const rows = Array.isArray(res) ? res : res?.records || []
      page = target
      if (more) {
        const ids = new Set(list.value.map((r) => r.id))
        list.value = [...list.value, ...rows.filter((r) => !ids.has(r.id))]
      } else {
        list.value = rows
      }
      hasMore.value = Array.isArray(res) ? rows.length >= pageSize : target * pageSize < (res?.total || 0)
      loaded = true
      return rows
    } finally {
      if (current === seq) loading.value = false
    }
  }

  /** 输入关键字：防抖后搜索 */
  function search(kw) {
    clearTimeout(timer)
    timer = setTimeout(() => load(kw), delay)
  }

  /** 获得焦点：第一次打开或上次按关键字搜索过时取第一页 */
  function focus() {
    if (!loaded || keyword.value) load('')
  }

  /** 下拉列表滚动到底部：加载下一页 */
  function popupScroll(e) {
    const el = e?.target
    if (!el || !hasMore.value || loading.value) return
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 24) load(keyword.value, true)
  }

  /** 取消待执行的搜索（扫码等直接处理时） */
  function cancel() {
    clearTimeout(timer)
    seq++
    loading.value = false
  }

  /**
   * 数据来源变化（如客户 / 供应商切换）：清空已加载的结果和记住的记录，作废进行中的请求，
   * 下次获得焦点时按新的来源重新取第一页
   */
  function reset() {
    clearTimeout(timer)
    seq++
    epoch++
    list.value = []
    known.value = new Map()
    loading.value = false
    hasMore.value = false
    keyword.value = ''
    page = 1
    loaded = false
    resolving.clear()
  }

  function find(id) {
    return list.value.find((r) => r.id === id) || known.value.get(id)
  }

  /** 记住记录（选中、扫码、外部传入），之后不在搜索结果中也能回显 */
  function remember(...records) {
    const map = new Map(known.value)
    records.filter(Boolean).forEach((r) => map.set(r.id, r))
    known.value = map
  }

  /** 已选值中既不在结果中也没有记住的，按 ID 取回 */
  const resolving = new Set()
  async function resolve(ids) {
    if (!fetchByIds) return
    const missing = ids.filter((id) => id != null && !find(id) && !resolving.has(id))
    if (!missing.length) return
    const current = epoch
    missing.forEach((id) => resolving.add(id))
    try {
      const rows = (await fetchByIds(missing)) || []
      if (current === epoch) remember(...rows)
    } catch {
      // 回显失败时显示 label 或 ID，不影响选择
    } finally {
      if (current === epoch) missing.forEach((id) => resolving.delete(id))
    }
  }

  onBeforeUnmount(() => clearTimeout(timer))

  return { list, known, loading, hasMore, keyword, load, search, focus, popupScroll, cancel, reset, find, remember, resolve }
}
