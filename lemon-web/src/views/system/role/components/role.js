/** 角色管理各面板共用的常量与工具 */

import { t } from '@/locales'

export { ADMIN_ROLE } from '@/utils/constants'

export const DATA_SCOPE_CUSTOM = 2

/** 基本信息字段；保存菜单权限时也要一并提交（后端校验名称和编码） */
export const BASIC_FIELDS = ['name', 'code', 'sort', 'status', 'remark']

export const ROLE_CODE_RULES = [
  { required: true, message: t('system.role.inputCode') },
  { pattern: /^[A-Za-z][\w-]*$/, message: t('system.role.codePattern') }
]

/** 各数据权限的说明，取值与字典 sys_data_scope 一致 */
export const DATA_SCOPE_DESC = {
  1: t('system.role.scopeDesc1'),
  2: t('system.role.scopeDesc2'),
  3: t('system.role.scopeDesc3'),
  4: t('system.role.scopeDesc4'),
  5: t('system.role.scopeDesc5')
}

export const pick = (obj, keys) => Object.fromEntries(keys.map((k) => [k, obj?.[k]]))

/** 两个 id 数组是否包含相同的元素（忽略顺序） */
export function sameIds(a = [], b = []) {
  if (a.length !== b.length) return false
  const set = new Set(a)
  return b.every((id) => set.has(id))
}
