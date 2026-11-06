import request from '@/utils/request'

/**
 * 进销存报表：report = sales | purchase。
 * 查询参数：begin、end（必填）、partnerId、salesmanId、materialId、category、brand、region、warehouseId、keyword；
 * 明细的 dim + key 为穿透的汇总维度和取值（key 为空表示「未设置」）
 */
export const jxcSummary = (report, params) => request.get(`/bi/jxc/${report}`, { params })
export const jxcDetail = (report, params) => request.get(`/bi/jxc/${report}/detail`, { params })
export const jxcExport = (report, params) => request.get(`/bi/jxc/${report}/export`, { params })

/** 经营状况、资产状况 */
export const jxcBusiness = (params) => request.get('/bi/jxc/business', { params })
export const jxcAssets = () => request.get('/bi/jxc/assets')

/** 商品分析：type = top | slow | overstock | shortage | turnover */
export const jxcGoods = (type, params) => request.get(`/bi/jxc/goods/${type}`, { params })

/** 客户 RFM */
export const jxcRfm = (params) => request.get('/bi/jxc/rfm', { params })
