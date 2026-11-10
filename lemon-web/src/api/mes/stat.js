import request from '@/utils/request'

/**
 * 生产统计报表（/mes/stat）：report 为 piece 人员计件 | output 工序产量 | wo 工单完成情况 | product 产品产量 | usage 用料差异，
 * params 含 begin、end、groupBy 和筛选条件，返回 { rows, total, truncated }
 */
export const mesStat = (report, params) => request.get(`/mes/stat/${report}`, { params })
/** 报工明细（人员计件、工序产量的汇总行穿透），分页 */
export const mesStatReports = (params) => request.get('/mes/stat/reports', { params })
