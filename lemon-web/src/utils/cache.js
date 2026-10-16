/**
 * 短时缓存的请求：同一页面里的多个下拉（如每行一个科目、库存位置选择）只请求一次，
 * 组件重新挂载时也能直接拿到结果；过几秒后重新加载以反映修改
 *
 * @param {string}   key    缓存键
 * @param {Function} loader () => Promise
 * @param {number}   ttl    有效期（毫秒）
 */
const cache = {}
export function cached(key, loader, ttl = 5000) {
  const hit = cache[key]
  if (hit && Date.now() - hit.at < ttl) return hit.promise
  const promise = loader()
  cache[key] = { at: Date.now(), promise }
  promise.catch(() => delete cache[key])
  return promise
}
