/**
 * 合并同一时刻的「按 ID 取名称」请求：明细表格中每行一个下拉、各自回显已选值时，只发一次请求（每次最多 50 个 ID）。
 * 返回的函数接收 ID 数组，结果为这一批全部 ID 的记录（调用方按 id 取用）
 *
 * @param fetcher (ids) => Promise<记录数组>
 */
export function batchByIds(fetcher, chunk = 50) {
  let queue = null
  return (ids) => {
    if (!queue) {
      const q = { ids: new Set() }
      q.promise = Promise.resolve().then(async () => {
        queue = null
        const all = [...q.ids]
        const parts = []
        for (let i = 0; i < all.length; i += chunk) parts.push(fetcher(all.slice(i, i + chunk)))
        return (await Promise.all(parts)).flat()
      })
      queue = q
    }
    ids.forEach((id) => queue.ids.add(id))
    return queue.promise
  }
}
