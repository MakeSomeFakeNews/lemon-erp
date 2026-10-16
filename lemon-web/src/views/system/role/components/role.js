/** 角色管理各面板共用的常量与工具 */

export { ADMIN_ROLE } from '@/utils/constants'

export const DATA_SCOPE_CUSTOM = 2

/** 基本信息字段；保存菜单权限时也要一并提交（后端校验名称和编码） */
export const BASIC_FIELDS = ['name', 'code', 'sort', 'status', 'remark']

export const ROLE_CODE_RULES = [
  { required: true, message: '请输入角色编码' },
  { pattern: /^[A-Za-z][\w-]*$/, message: '以字母开头，仅包含字母、数字、下划线、横线' }
]

/** 各数据权限的说明，取值与字典 sys_data_scope 一致 */
export const DATA_SCOPE_DESC = {
  1: '可以查看和操作所有部门的数据',
  2: '只能访问勾选的部门的数据',
  3: '只能访问用户所在部门的数据',
  4: '可以访问用户所在部门及其全部下级部门的数据',
  5: '只能访问用户本人的数据'
}

export const pick = (obj, keys) => Object.fromEntries(keys.map((k) => [k, obj?.[k]]))

/** 两个 id 数组是否包含相同的元素（忽略顺序） */
export function sameIds(a = [], b = []) {
  if (a.length !== b.length) return false
  const set = new Set(a)
  return b.every((id) => set.has(id))
}
