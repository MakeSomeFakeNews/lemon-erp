import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 客户（scope：mine / team / pool）与联系人 */
export const pageCrmCustomer = (params) => request.get('/crm/customer', { params })
export const getCrmCustomer = (customerId) => request.get(`/crm/customer/${customerId}`)
export const crmCustomerSummary = (customerId) => request.get(`/crm/customer/${customerId}/summary`)
export const addCrmCustomer = (data) => request.post('/crm/customer', data)
export const updateCrmCustomer = (customerId, data) => request.put(`/crm/customer/${customerId}`, data)
export const assignCustomers = (customerIds, ownerId) => request.post('/crm/customer/assign', { customerIds, ownerId })
export const claimCustomers = (customerIds) => request.post('/crm/customer/claim', customerIds)
export const releaseCustomers = (customerIds) => request.post('/crm/customer/release', customerIds)
export const listContacts = (customerId) => request.get('/crm/contact', { params: { customerId } })
export const addContact = (data) => request.post('/crm/contact', data)
export const updateContact = (id, data) => request.put(`/crm/contact/${id}`, data)
export const deleteContact = (id) => request.delete(`/crm/contact/${id}`)

/** 线索 */
export const pageLead = (params) => request.get('/crm/lead', { params })
export const getLead = (id) => request.get(`/crm/lead/${id}`)
export const addLead = (data) => request.post('/crm/lead', data)
export const updateLead = (id, data) => request.put(`/crm/lead/${id}`, data)
export const deleteLead = (id) => request.delete(`/crm/lead/${ids(id)}`)
export const invalidLead = (id, reason) => request.post(`/crm/lead/${id}/invalid`, { reason })
export const convertLead = (id, data) => request.post(`/crm/lead/${id}/convert`, data)

/** 销售机会 */
export const pageOpportunity = (params) => request.get('/crm/opportunity', { params })
export const getOpportunity = (id) => request.get(`/crm/opportunity/${id}`)
export const opportunityStages = (id) => request.get(`/crm/opportunity/${id}/stages`)
export const addOpportunity = (data) => request.post('/crm/opportunity', data)
export const updateOpportunity = (id, data) => request.put(`/crm/opportunity/${id}`, data)
export const changeOpportunityStage = (id, data) => request.put(`/crm/opportunity/${id}/stage`, data)
export const deleteOpportunity = (id) => request.delete(`/crm/opportunity/${ids(id)}`)
export const opportunityToOrder = (id, data) => request.post(`/crm/opportunity/${id}/order`, data)

/** 销售合同 */
export const pageCrmContract = (params) => request.get('/crm/contract', { params })
export const getCrmContract = (id) => request.get(`/crm/contract/${id}`)
export const addCrmContract = (data) => request.post('/crm/contract', data)
export const updateCrmContract = (id, data) => request.put(`/crm/contract/${id}`, data)
export const deleteCrmContract = (id) => request.delete(`/crm/contract/${id}`)
/** 返回 true 已发起审批流程，false 已直接生效 */
export const submitCrmContract = (id) => request.post(`/crm/contract/${id}/submit`)
export const linkContractOrder = (id, orderId) => request.post(`/crm/contract/${id}/order/${orderId}`)
/** 按关联的报价单生成销售订单草稿，返回订单 ID */
export const contractToOrder = (id, warehouseId) => request.post(`/crm/contract/${id}/to-order`, null, { params: { warehouseId } })
export const completeCrmContract = (id) => request.post(`/crm/contract/${id}/complete`)
export const terminateCrmContract = (id, reason) => request.post(`/crm/contract/${id}/terminate`, null, { params: { reason } })

/** 跟进记录 */
export const pageFollowup = (params) => request.get('/crm/followup', { params })
export const addFollowup = (data) => request.post('/crm/followup', data)
export const deleteFollowup = (id) => request.delete(`/crm/followup/${id}`)

/** 市场活动 */
export const pageCampaign = (params) => request.get('/crm/campaign', { params })
export const campaignOptions = () => request.get('/crm/campaign/options')
export const getCampaign = (id) => request.get(`/crm/campaign/${id}`)
export const addCampaign = (data) => request.post('/crm/campaign', data)
export const updateCampaign = (id, data) => request.put(`/crm/campaign/${id}`, data)
export const deleteCampaign = (id) => request.delete(`/crm/campaign/${ids(id)}`)

/** 服务工单（action：assign / process / resolve / close / reopen） */
export const pageTicket = (params) => request.get('/crm/ticket', { params })
export const getTicket = (id) => request.get(`/crm/ticket/${id}`)
export const ticketLogs = (id) => request.get(`/crm/ticket/${id}/logs`)
export const ticketWarranty = (serialNo) => request.get('/crm/ticket/warranty', { params: { serialNo } })
export const addTicket = (data) => request.post('/crm/ticket', data)
export const updateTicket = (id, data) => request.put(`/crm/ticket/${id}`, data)
export const ticketAction = (id, action, data) => request.post(`/crm/ticket/${id}/${action}`, data)
/** 备件领用记录、领用备件（生成服务领用出库单）、服务收费（生成应收单） */
export const ticketParts = (id) => request.get(`/crm/ticket/${id}/parts`)
export const issueTicketParts = (id, data) => request.post(`/crm/ticket/${id}/parts`, data)
export const chargeTicket = (id, data) => request.post(`/crm/ticket/${id}/charge`, data)
export const deleteTicket = (id) => request.delete(`/crm/ticket/${ids(id)}`)

/** 客户分析 */
export const crmOverview = () => request.get('/crm/analysis/overview')
export const crmFunnel = () => request.get('/crm/analysis/funnel')
export const crmRfm = () => request.get('/crm/analysis/rfm')
export const crmTrend = () => request.get('/crm/analysis/trend')
export const crmSources = () => request.get('/crm/analysis/sources')
export const crmOwners = () => request.get('/crm/analysis/owners')
export const crmIndustries = () => request.get('/crm/analysis/industries')
export const crmTickets = () => request.get('/crm/analysis/tickets')
