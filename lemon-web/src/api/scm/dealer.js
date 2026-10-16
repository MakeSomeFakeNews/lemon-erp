import request from '@/utils/request'

/** 经销商门户：只能访问当前账号关联的经销商的数据 */
export const dealerCatalog = (keyword) => request.get('/dealer/catalog', { params: { keyword } })
export const pageDealerOrder = (params) => request.get('/dealer/order', { params })
export const getDealerOrder = (id) => request.get(`/dealer/order/${id}`)
/** data：{ deliveryDate, remark, rebateAmount, items: [{ materialId, qty }] }，返回订单 ID */
export const placeDealerOrder = (data) => request.post('/dealer/order', data)
export const cancelDealerOrder = (id) => request.delete(`/dealer/order/${id}`)
export const dealerRebateBalance = () => request.get('/dealer/rebate/balance')
export const pageDealerReport = (params) => request.get('/dealer/report', { params })
export const getDealerReport = (id) => request.get(`/dealer/report/${id}`)
/** 上报某月（yyyyMM），data：{ remark, items: [{ materialId, sellQty, stockQty }] } */
export const saveDealerReport = (period, data) => request.put(`/dealer/report/${period}`, data)

/** 内部：渠道上报、外部订单导入 */
export const pageChannelReport = (params) => request.get('/scm/channel/reports', { params })
export const getChannelReport = (id) => request.get(`/scm/channel/reports/${id}`)
/** 外部订单导入：{ rows: [{ rowNum, data }] } → { total, imported, errors, orders, skipped } */
export const importSalesOrders = (data) => request.post('/scm/sales/import', data)
