/** localStorage 读写，异常（隐私模式等）时静默降级 */
export function load(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw == null ? fallback : JSON.parse(raw)
  } catch {
    return fallback
  }
}

export function save(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    /* ignore */
  }
}

export function remove(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}
