/** 与后端约定的常量 */

/** 超级管理员角色编码，拥有全部权限 */
export const ADMIN_ROLE = 'admin'

/** 内置超级管理员用户 ID，不允许删除、停用、移除超级管理员角色 */
export const SUPER_ADMIN_ID = 1

/** 启用 / 停用状态（字典 sys_common_status，后端 EnableStatus，接口中是数字 1 / 0） */
export const STATUS_ENABLED = 1
export const STATUS_DISABLED = 0
