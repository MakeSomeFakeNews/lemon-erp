export function joinPath(parent, path) {
  if (!path) return parent || '/'
  if (path.startsWith('/') || /^https?:/.test(path)) return path
  return `${(parent || '').replace(/\/$/, '')}/${path}`
}
