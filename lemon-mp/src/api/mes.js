import { get, post, put } from '@/utils/request'

/** 生产工单：{ current, size, woNo, keyword, status } */
export const pageWorkOrder = (params) => get('/mes/wo', params)
/** 工单详情（含工序 ops） */
export const getWorkOrder = (id) => get(`/mes/wo/${id}`)
/** 报工：{ opId, reportDate, goodQty, scrapQty, hours, remark, operatorId, equipmentId, moldId } */
export const reportWorkOrder = (id, data) => post(`/mes/wo/${id}/report`, data)
/** 报工记录：{ current, size, woId } */
export const pageReport = (params) => get('/mes/wo/reports', params)
/** 设备选项（登录即可访问） */
export const equipmentOptions = () => get('/mes/equipment/options')
/** 领用到工单的模具 */
export const moldsOfWo = (woId) => get(`/mes/mold/by-wo/${woId}`)

// ---------------------------------------------------------------- 设备与质量（pkg-mes）

/** 设备台账：{ current, size, keyword, status, dueDays } */
export const pageEquipment = (params) => get('/mes/equipment', params)
export const getEquipment = (id) => get(`/mes/equipment/${id}`)
/** 维修 / 保养单：{ current, size, equipmentId, type, status, keyword } */
export const pageMaintenance = (params) => get('/mes/maintenance', params)
/** 新建维修 / 保养单：{ equipmentId, type, content, planDate, handler }，返回 ID */
export const addMaintenance = (data) => post('/mes/maintenance', data)
/** 开始处理：{ handler } */
export const startMaintenance = (id, data) => post(`/mes/maintenance/${id}/start`, data)
/** 完成：{ result, cost, handler } */
export const finishMaintenance = (id, data) => post(`/mes/maintenance/${id}/finish`, data)

/** 质量检验：{ current, size, type, result, keyword } */
export const pageInspection = (params) => get('/mes/inspection', params)
/** 检验单详情（含逐项结果 items） */
export const getInspection = (id) => get(`/mes/inspection/${id}`)
export const addInspection = (data) => post('/mes/inspection', data)
export const updateInspection = (id, data) => put(`/mes/inspection/${id}`, data)
/** 确认后不能修改；不合格时生成不合格品处理单 */
export const confirmInspection = (id) => post(`/mes/inspection/${id}/confirm`)
/** 匹配检验方案与抽样结果：{ type, materialId, lotQty } → { plan, sampleQty, sampleCode, acceptQty, rejectQty } */
export const matchPlan = (params) => get('/qms/plan/match', params, { silent: true })

/** 不合格品：{ current, size, status, keyword } */
export const pageNcr = (params) => get('/qms/ncr', params)
export const getNcr = (id) => get(`/qms/ncr/${id}`)
/** 手工登记：{ source, materialId, batchNo, qty, woNo, defectType, description } */
export const addNcr = (data) => post('/qms/ncr', data)

/** 物料选项（登录即可访问）：按编码、名称搜索 */
export const materialOptions = (keyword) => get('/wms/material/options', { keyword })
