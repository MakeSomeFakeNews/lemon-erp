import { get, post, put, upload } from '@/utils/request'

/** 客户：{ current, size, scope: mine | team, keyword, dueToday }（数据权限由后端处理） */
export const pageCustomers = (params) => get('/crm/customer', params)
export const getCustomer = (customerId) => get(`/crm/customer/${customerId}`)
/** 客户经营概况：销售额、订单数、应收余额、赢单等 */
export const customerSummary = (customerId) => get(`/crm/customer/${customerId}/summary`, null, { silent: true })
export const listContacts = (customerId) => get('/crm/contact', { customerId })

/** 跟进记录：{ current, size, customerId, targetType, targetId, mine } */
export const pageFollowups = (params) => get('/crm/followup', params)
/** 写跟进：{ targetType, targetId, method, content, followTime, nextTime, nextPlan } */
export const addFollowup = (data) => post('/crm/followup', data)

/** 销售机会：{ current, size, keyword, customerId, mine } */
export const pageOpportunities = (params) => get('/crm/opportunity', params)
/** 推进阶段：{ stage, lostReason } */
export const changeOpportunityStage = (id, data) => put(`/crm/opportunity/${id}/stage`, data)

/** 销售订单：{ current, size, orderNo, partnerName（客户名称或编码）, partnerId, status } */
export const pageSalesOrders = (params) => get('/scm/sales/order', params)
export const getSalesOrder = (id) => get(`/scm/sales/order/${id}`)
/** 订单追溯：出库单、应收单（已核销金额）、发运等，按权限返回 */
export const salesOrderTrace = (id) => get(`/bi/trace/order/sales/${id}`, null, { silent: true })

/** 线索：{ current, size, keyword, status, mine } */
export const pageLeads = (params) => get('/crm/lead', params)
export const getLead = (id) => get(`/crm/lead/${id}`)
/** 新建 / 修改线索：{ company, contactName, title, phone, email, industry, source, region, requirement } */
export const addLead = (data) => post('/crm/lead', data)
export const updateLead = (id, data) => put(`/crm/lead/${id}`, data)
export const invalidLead = (id, reason) => post(`/crm/lead/${id}/invalid`, { reason })
/** 转化为客户（可同时生成商机）：{ customerCode, createOpportunity, opportunityName, amount, expectedDate }，返回 { customerId, opportunityId } */
export const convertLead = (id, data) => post(`/crm/lead/${id}/convert`, data)

/** 服务工单：{ current, size, keyword, status, mine（处理人是我）, overdue } */
export const pageTickets = (params) => get('/crm/ticket', params)
export const getTicket = (id) => get(`/crm/ticket/${id}`)
export const ticketLogs = (id) => get(`/crm/ticket/${id}/logs`)
/** 受理工单：{ customerId, contactId, type, priority, channel, title, description, serialNo } */
export const addTicket = (data) => post('/crm/ticket', data)
/** 处理工单：action 为 process（处理记录）/ resolve（解决方案）/ close（满意度）/ reopen（原因），data { content, satisfaction } */
export const ticketAction = (id, action, data) => post(`/crm/ticket/${id}/${action}`, data)

/** 跟进照片（附件，业务类型 crm_followup） */
export const FOLLOWUP_BIZ_TYPE = 'crm_followup'
/** 多条跟进的照片，按跟进 ID 分组：{ [followupId]: [attachment] } */
export const batchFollowupPhotos = (followupIds) =>
  post(`/system/attachment/batch?bizType=${FOLLOWUP_BIZ_TYPE}`, followupIds, { silent: true })
export const uploadFollowupPhoto = (followupId, filePath) => upload('/system/attachment', filePath, { bizType: FOLLOWUP_BIZ_TYPE, bizId: followupId })
