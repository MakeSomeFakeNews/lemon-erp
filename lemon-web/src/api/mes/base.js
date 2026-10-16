import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 工作中心 */
export const pageWorkCenter = (params) => request.get('/mes/work-center', { params })
export const getWorkCenter = (id) => request.get(`/mes/work-center/${id}`)
/** 人工费率参考：生产岗位员工的平均小时人工成本 */
export const laborRateReference = () => request.get('/mes/work-center/labor-rate-reference')
export const addWorkCenter = (data) => request.post('/mes/work-center', data)
export const updateWorkCenter = (id, data) => request.put(`/mes/work-center/${id}`, data)
export const changeWorkCenterStatus = (id, status) => request.put(`/mes/work-center/${id}/status`, { status })
export const deleteWorkCenter = (id) => request.delete(`/mes/work-center/${ids(id)}`)
export const workCenterOptions = () => request.get('/mes/work-center/options')

/** BOM */
export const pageBom = (params) => request.get('/mes/bom', { params })
export const getBom = (id) => request.get(`/mes/bom/${id}`)
export const bomsOfMaterial = (materialId) => request.get(`/mes/bom/material/${materialId}`)
export const addBom = (data) => request.post('/mes/bom', data)
export const updateBom = (id, data) => request.put(`/mes/bom/${id}`, data)
export const changeBomStatus = (id, status) => request.put(`/mes/bom/${id}/status`, { status })
export const setDefaultBom = (id) => request.put(`/mes/bom/${id}/default`)
export const deleteBom = (id) => request.delete(`/mes/bom/${ids(id)}`)

/** 设备与维护 */
export const pageEquipment = (params) => request.get('/mes/equipment', { params })
export const getEquipment = (id) => request.get(`/mes/equipment/${id}`)
export const addEquipment = (data) => request.post('/mes/equipment', data)
export const updateEquipment = (id, data) => request.put(`/mes/equipment/${id}`, data)
export const deleteEquipment = (id) => request.delete(`/mes/equipment/${ids(id)}`)
export const equipmentOptions = () => request.get('/mes/equipment/options')
export const pageMaintenance = (params) => request.get('/mes/maintenance', { params })
export const addMaintenance = (data) => request.post('/mes/maintenance', data)
export const startMaintenance = (id, handler) => request.post(`/mes/maintenance/${id}/start`, { handler })
export const finishMaintenance = (id, data) => request.post(`/mes/maintenance/${id}/finish`, data)

/** 设备 OEE：params { beginDate, endDate, workCenterId } */
export const oeeSummary = (params) => request.get('/mes/oee', { params })
/** { total, days, downtime } */
export const oeeDetail = (equipmentId, params) => request.get(`/mes/oee/${equipmentId}`, { params })
export const pageStop = (params) => request.get('/mes/equipment-stop', { params })
/** data：{ equipmentId, startTime, endTime, reason, remark } */
export const addStop = (data) => request.post('/mes/equipment-stop', data)
export const updateStop = (id, data) => request.put(`/mes/equipment-stop/${id}`, data)
export const deleteStop = (id) => request.delete(`/mes/equipment-stop/${id}`)

/** 模具工装 */
export const pageMold = (params) => request.get('/mes/mold', { params })
export const getMold = (id) => request.get(`/mes/mold/${id}`)
export const moldLogs = (id) => request.get(`/mes/mold/${id}/logs`)
export const moldsOfWo = (woId) => request.get(`/mes/mold/by-wo/${woId}`)
export const addMold = (data) => request.post('/mes/mold', data)
export const updateMold = (id, data) => request.put(`/mes/mold/${id}`, data)
export const deleteMold = (id) => request.delete(`/mes/mold/${id}`)
/** action：issue | return | repair | repaired | scrap；data：{ woId, equipmentId, date, location, remark } */
export const moldAction = (id, action, data) => request.post(`/mes/mold/${id}/${action}`, data)
export const deleteMaintenance = (id) => request.delete(`/mes/maintenance/${ids(id)}`)

/** 质量检验 */
export const pageInspection = (params) => request.get('/mes/inspection', { params })
export const getInspection = (id) => request.get(`/mes/inspection/${id}`)
export const addInspection = (data) => request.post('/mes/inspection', data)
export const updateInspection = (id, data) => request.put(`/mes/inspection/${id}`, data)
export const deleteInspection = (id) => request.delete(`/mes/inspection/${ids(id)}`)
export const confirmInspection = (id) => request.post(`/mes/inspection/${id}/confirm`)
/** by：TYPE 按检验类型 | SUPPLIER 按供应商 */
export const qualitySummary = (params) => request.get('/mes/inspection/summary', { params })

/** 物料的生产计划属性与替代料（在仓库物料管理中维护） */
export const materialPlans = (ids) => request.get('/mes/material/plans', { params: { ids: [].concat(ids).join(',') } })
export const getMaterialPlan = (id) => request.get(`/mes/material/${id}`)
export const saveMaterialPlan = (id, data) => request.put(`/mes/material/${id}/plan`, data)

/** 齐套分析 */
export const kitOrders = (params) => request.get('/mes/kit/orders', { params })
export const kitOrderLines = (woId) => request.get(`/mes/kit/orders/${woId}`)
export const kitMaterials = (params) => request.get('/mes/kit/materials', { params })
export const kitMaterialLines = (materialId) => request.get(`/mes/kit/materials/${materialId}`)
/** 缺料下单，返回生成的单据号 */
export const kitRelease = (data) => request.post('/mes/kit/release', data)
export const switchSubstitute = (data) => request.post('/mes/kit/substitute', data)
