import request from '@/utils/request'

// 租户（SaaS 模式，仅平台租户可用）
export const pageTenant = (params) => request.get('/system/tenant', { params })
export const getTenant = (id) => request.get(`/system/tenant/${id}`)
export const addTenant = (data) => request.post('/system/tenant', data)
export const updateTenant = (id, data) => request.put(`/system/tenant/${id}`, data)
export const changeTenantStatus = (id, status) => request.put(`/system/tenant/${id}/status`, { status })
export const deleteTenant = (ids) => request.delete(`/system/tenant/${[].concat(ids).join(',')}`)

// 租户套餐
export const listTenantPackage = (params) => request.get('/system/tenant-package', { params })
export const tenantPackageOptions = () => request.get('/system/tenant-package/options')
export const tenantPackageMenuTree = () => request.get('/system/tenant-package/menu-tree')
export const getTenantPackage = (id) => request.get(`/system/tenant-package/${id}`)
export const addTenantPackage = (data) => request.post('/system/tenant-package', data)
export const updateTenantPackage = (id, data) => request.put(`/system/tenant-package/${id}`, data)
export const deleteTenantPackage = (ids) => request.delete(`/system/tenant-package/${[].concat(ids).join(',')}`)
