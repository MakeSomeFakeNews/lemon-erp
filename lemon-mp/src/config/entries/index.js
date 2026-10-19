import { hasPerm } from '@/store/user'
import approval from './approval'
import hr from './hr'
import crm from './crm'
import wms from './wms'
import pm from './pm'
import mes from './mes'

/**
 * 工作台入口。每个功能区在本目录的同名文件中登记：
 *   { key, group, title（消息键）, color, url, tab?（底部导航页）, perm?（任一权限，空为登录即可）, permAll?（还必须同时具备的权限）, links?（对应的 Web 端路由前缀） }
 * group：office 办公 / sales 销售与服务 / warehouse 仓储生产 / quality 设备与质量。links 用于把首页待办、站内消息中的 Web 端链接转到小程序页面
 */
export const GROUPS = ['office', 'sales', 'warehouse', 'quality']
const ALL = [...approval, ...hr, ...pm, ...crm, ...wms, ...mes]

export function visibleEntries() {
  return ALL.filter((e) => hasPerm(e.perm) && (e.permAll || []).every((p) => hasPerm(p)))
}

/** Web 端链接（如 /workflow/todo、/crm/customer?id=1）对应的小程序入口，没有对应或没有权限时为 null */
export function entryOfLink(link) {
  if (!link) return null
  const path = link.split('?')[0]
  let best = null
  for (const e of visibleEntries()) {
    for (const prefix of e.links || []) {
      if ((path === prefix || path.startsWith(prefix + '/')) && (!best || prefix.length > best.len)) best = { entry: e, len: prefix.length }
    }
  }
  return best?.entry || null
}

/** 底部导航页不能带参数：从链接进入时把链接暂存，由页面在 onShow 中用 takeTabLink 取出（如审批页按链接切换分段） */
let tabLink = null

/** @param link 从首页待办、消息进入时的 Web 端链接 */
export function openEntry(entry, link) {
  if (entry.tab) {
    tabLink = link ? { url: entry.url, link } : null
    uni.switchTab({ url: entry.url })
  } else uni.navigateTo({ url: entry.url })
}

/** 取出并清除进入该底部导航页时的链接 */
export function takeTabLink(url) {
  const hit = tabLink?.url === url ? tabLink.link : null
  tabLink = null
  return hit
}
