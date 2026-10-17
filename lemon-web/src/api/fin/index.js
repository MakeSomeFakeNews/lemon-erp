import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 会计科目 */
export const accountTree = (params) => request.get('/fin/account', { params })
/** 全部科目（含上级），账簿、预算选择 */
export const allAccounts = () => request.get('/fin/account/all')
/** 启用的末级科目 */
export const accountOptions = () => request.get('/fin/account/options')
export const addAccount = (data) => request.post('/fin/account', data)
export const updateAccount = (id, data) => request.put(`/fin/account/${id}`, data)
export const changeAccountStatus = (id, status) => request.put(`/fin/account/${id}/status`, { status })
export const deleteAccount = (id) => request.delete(`/fin/account/${ids(id)}`)

/** 凭证 */
export const pageVoucher = (params) => request.get('/fin/voucher', { params })
export const getVoucher = (id) => request.get(`/fin/voucher/${id}`)
export const addVoucher = (data) => request.post('/fin/voucher', data)
export const updateVoucher = (id, data) => request.put(`/fin/voucher/${id}`, data)
export const deleteVoucher = (id) => request.delete(`/fin/voucher/${ids(id)}`)
/** action：audit / unaudit / post / unpost */
export const voucherAction = (action, id) => request.post(`/fin/voucher/${action}/${ids(id)}`)

/** 会计期间 */
export const pagePeriod = (params) => request.get('/fin/period', { params })
export const closePeriod = (period) => request.post(`/fin/period/${period}/close`)
export const reopenPeriod = (period) => request.post(`/fin/period/${period}/reopen`)

/** 账簿与报表，期间格式 yyyyMM */
export const trialBalance = (params) => request.get('/fin/report/trial-balance', { params })
export const ledger = (params) => request.get('/fin/report/ledger', { params })
/** 辅助核算余额：{ dimension: project | customer | supplier, begin, end, accountId? } */
export const auxBalance = (params) => request.get('/fin/report/aux', { params })
export const balanceSheet = (period) => request.get('/fin/report/balance-sheet', { params: { period } })
export const incomeStatement = (period) => request.get('/fin/report/income', { params: { period } })

/** 应收 / 应付：kind = ar | ap */
export const pageBill = (kind, params) => request.get(`/fin/bill/${kind}`, { params })
export const unsettledBills = (kind, partnerId) => request.get(`/fin/bill/${kind}/unsettled`, { params: { partnerId } })
export const billAging = (kind, params) => request.get(`/fin/bill/${kind}/aging`, { params })
export const addBill = (kind, data) => request.post(`/fin/bill/${kind}`, data)
export const deleteBill = (kind, id) => request.delete(`/fin/bill/${kind}/${ids(id)}`)
/** 返回生成的凭证数 */
export const billVoucher = (kind, id) => request.post(`/fin/bill/${kind}/voucher/${ids(id)}`)

/** 收款 / 付款：kind = receipt | payment */
export const pageSettlement = (kind, params) => request.get(`/fin/settlement/${kind}`, { params })
export const getSettlement = (kind, id) => request.get(`/fin/settlement/${kind}/${id}`)
export const addSettlement = (kind, data) => request.post(`/fin/settlement/${kind}`, data)
export const deleteSettlement = (kind, id) => request.delete(`/fin/settlement/${kind}/${ids(id)}`)
export const settlementVoucher = (kind, id) => request.post(`/fin/settlement/${kind}/voucher/${ids(id)}`)

/** 固定资产 */
export const pageAsset = (params) => request.get('/fin/asset', { params })
export const getAsset = (id) => request.get(`/fin/asset/${id}`)
export const addAsset = (data) => request.post('/fin/asset', data)
export const updateAsset = (id, data) => request.put(`/fin/asset/${id}`, data)
export const deleteAsset = (id) => request.delete(`/fin/asset/${ids(id)}`)
/** 返回处置凭证 ID */
/** data：{ date, income, expense, settleAccountId, remark } */
export const disposeAsset = (id, data) => request.post(`/fin/asset/${id}/dispose`, data)
export const pageDepreciation = (params) => request.get('/fin/asset/depreciation', { params })
export const depreciate = (period) => request.post(`/fin/asset/depreciation/${period}`)
export const undoDepreciation = (period) => request.delete(`/fin/asset/depreciation/${period}`)
/** data：{ moveType: ASSIGN | RETURN | TRANSFER, moveDate, deptId, userId, location, remark } */
export const moveAsset = (id, data) => request.post(`/fin/asset/${id}/move`, data)
export const assetMoves = (id) => request.get(`/fin/asset/${id}/moves`)

/** 资产盘点 */
export const pageAssetCheck = (params) => request.get('/fin/asset-check', { params })
export const getAssetCheck = (id) => request.get(`/fin/asset-check/${id}`)
/** data：{ checkDate, deptId, location, remark }，返回盘点单 ID */
export const addAssetCheck = (data) => request.post('/fin/asset-check', data)
/** data：{ result, actualDeptId, actualUserId, actualLocation, remark } */
export const recordAssetCheck = (id, itemId, data) => request.put(`/fin/asset-check/${id}/items/${itemId}`, data)
export const addAssetGain = (id, data) => request.post(`/fin/asset-check/${id}/gains`, data)
export const removeAssetGain = (id, itemId) => request.delete(`/fin/asset-check/${id}/gains/${itemId}`)
/** 返回盘亏处置的资产数 */
export const finishAssetCheck = (id) => request.post(`/fin/asset-check/${id}/finish`)
export const deleteAssetCheck = (id) => request.delete(`/fin/asset-check/${id}`)

/** 低值易耗品领用 */
export const pageConsumable = (params) => request.get('/fin/consumable', { params })
export const consumableLedger = (params) => request.get('/fin/consumable/ledger', { params })
export const getConsumable = (id) => request.get(`/fin/consumable/${id}`)
export const addConsumable = (data) => request.post('/fin/consumable', data)
export const updateConsumable = (id, data) => request.put(`/fin/consumable/${id}`, data)
export const confirmConsumable = (id) => request.post(`/fin/consumable/${id}/confirm`)
export const deleteConsumable = (id) => request.delete(`/fin/consumable/${id}`)

/** 成本核算 */
export const pageCostRun = (params) => request.get('/fin/cost', { params })
export const materialCosts = (period, params) => request.get(`/fin/cost/${period}/materials`, { params })
export const woCosts = (period, params) => request.get(`/fin/cost/${period}/work-orders`, { params })
/** 成本差异：{ versionId, versionName, costRun, orders, total } */
export const costVariance = (period) => request.get(`/fin/cost/${period}/variance`)
export const costVarianceComponents = (period, woId) => request.get(`/fin/cost/${period}/variance/${woId}`)

/** 标准成本 */
export const pageStdCost = (params) => request.get('/fin/std-cost', { params })
export const getStdCost = (id) => request.get(`/fin/std-cost/${id}`)
/** data：{ name, effectivePeriod, copyFromId, remark }，返回版本 ID */
export const addStdCost = (data) => request.post('/fin/std-cost', data)
export const updateStdCost = (id, data) => request.put(`/fin/std-cost/${id}`, data)
export const deleteStdCost = (id) => request.delete(`/fin/std-cost/${id}`)
/** params：{ current, size, keyword, make } */
export const stdCostItems = (id, params) => request.get(`/fin/std-cost/${id}/items`, { params })
export const stdCostBreakdown = (id, materialId) => request.get(`/fin/std-cost/${id}/items/${materialId}/breakdown`)
/** data：{ materialId, price } */
export const saveStdPrice = (id, data) => request.put(`/fin/std-cost/${id}/items`, data)
export const removeStdPrice = (id, materialId) => request.delete(`/fin/std-cost/${id}/items/${materialId}`)
/** 返回单价为 0 的采购件数 */
export const rollupStdCost = (id) => request.post(`/fin/std-cost/${id}/rollup`)
export const releaseStdCost = (id) => request.post(`/fin/std-cost/${id}/release`)
export const unreleaseStdCost = (id) => request.post(`/fin/std-cost/${id}/unrelease`)
export const runCost = (period) => request.post(`/fin/cost/${period}/run`)
export const undoCost = (period) => request.delete(`/fin/cost/${period}`)

/** 期初余额 */
export const accountOpenings = () => request.get('/fin/opening/accounts')
export const saveAccountOpenings = (items) => request.put('/fin/opening/accounts', { items })
/** startPeriod：尚未保存存货期初时按该启用期间推算期初数量 */
export const stockOpening = (startPeriod) => request.get('/fin/opening/stock', { params: { startPeriod } })
export const saveStockOpening = (data) => request.put('/fin/opening/stock', data)
/** 启用向导：各项期初数据的完成情况 */
export const openingSummary = () => request.get('/fin/opening/summary')
/** 期初库存导入：生成启用期间前一天的期初入库单 */
export const importOpeningStock = (startPeriod, data) => request.post('/fin/opening/stock-in/import', data, { params: { startPeriod } })
/** 期初往来导入：kind 为 AR / AP */
export const importOpeningBills = (kind, data) => request.post(`/fin/opening/bills/${kind}/import`, data)

/** 预算 */
export const listBudget = (params) => request.get('/fin/budget', { params })
export const budgetExecution = (params) => request.get('/fin/budget/execution', { params })
export const addBudget = (data) => request.post('/fin/budget', data)
export const updateBudget = (id, data) => request.put(`/fin/budget/${id}`, data)
export const deleteBudget = (id) => request.delete(`/fin/budget/${ids(id)}`)

/** 坏账核销：data { amount, date, reason }，返回凭证 ID */
export const badDebt = (id, data) => request.post(`/fin/bill/ar/${id}/bad-debt`, data)
/** 红字冲销：返回红字凭证 ID */
export const reverseVoucher = (id, date) => request.post(`/fin/voucher/${id}/reverse`, null, { params: { date } })

/** 结账检查：[{ name, level: OK | WARN | ERROR, message }] */
export const closeCheck = (period) => request.get(`/fin/period/${period}/check`)
/** 增值税结转：data { cityRate, eduRate, localRate }，返回 { voucherId, output, input, payable, surcharge } */
export const transferVat = (period, data) => request.post(`/fin/period/${period}/vat`, data)
/** 现金流量表，期间 yyyyMM */
export const cashFlow = (begin, end) => request.get('/fin/report/cash-flow', { params: { begin, end } })

/** 发票：kind = OUTPUT 销项 | INPUT 进项 */
export const pageInvoice = (params) => request.get('/fin/invoice', { params })
export const getInvoice = (id) => request.get(`/fin/invoice/${id}`)
export const invoiceOpenBills = (kind, partnerId) => request.get('/fin/invoice/open-bills', { params: { kind, partnerId } })
export const addInvoice = (data) => request.post('/fin/invoice', data)
export const cancelInvoice = (id) => request.post(`/fin/invoice/${id}/cancel`)
/** 进项认证：period 为空时取消认证 */
export const certifyInvoice = (ids, period) => request.post('/fin/invoice/certify', { ids, period })
/** 未开票收入（OUTPUT）/ 暂估应付（INPUT） */
export const uninvoiced = (kind, asOf) => request.get('/fin/invoice/uninvoiced', { params: { kind, asOf } })

/** 预收 / 预付：kind = RECEIPT | PAYMENT */
export const pageAdvance = (params) => request.get('/fin/advance', { params })
export const getAdvance = (id) => request.get(`/fin/advance/${id}`)
export const addAdvance = (data) => request.post('/fin/advance', data)
/** 冲抵：data { date, items: [{ billId, amount }] } */
export const applyAdvance = (id, data) => request.post(`/fin/advance/${id}/apply`, data)
export const deleteAdvance = (id) => request.delete(`/fin/advance/${id}`)

/** 银行对账 */
export const bankStatements = (params) => request.get('/fin/bank/statement', { params })
export const bankEntries = (params) => request.get('/fin/bank/entries', { params })
/** data { accountId, rows: [{ txnDate, amount, summary, refNo }] }，返回导入条数 */
export const importBankStatements = (data) => request.post('/fin/bank/statement/import', data)
export const autoMatchBank = (params) => request.post('/fin/bank/auto-match', null, { params })
export const matchBank = (id, entryId) => request.post(`/fin/bank/statement/${id}/match/${entryId}`)
export const unmatchBank = (id) => request.post(`/fin/bank/statement/${id}/unmatch`)
export const deleteBankStatements = (id) => request.delete(`/fin/bank/statement/${ids(id)}`)
export const bankReconcile = (params) => request.get('/fin/bank/reconcile', { params })

/** 费用报销 */
export const pageClaim = (params) => request.get('/fin/claim', { params })
export const getClaim = (id) => request.get(`/fin/claim/${id}`)
export const addClaim = (data) => request.post('/fin/claim', data)
export const updateClaim = (id, data) => request.put(`/fin/claim/${id}`, data)
export const deleteClaim = (id) => request.delete(`/fin/claim/${id}`)
/** 返回 { flow, warnings } */
export const submitClaim = (id) => request.post(`/fin/claim/${id}/submit`)
export const payClaim = (id, data) => request.post(`/fin/claim/${id}/pay`, data)

/** 员工借款 */
export const pageLoan = (params) => request.get('/fin/loan', { params })
export const getLoan = (id) => request.get(`/fin/loan/${id}`)
export const addLoan = (data) => request.post('/fin/loan', data)
export const deleteLoan = (id) => request.delete(`/fin/loan/${id}`)
/** 返回是否进入审批流程 */
export const submitLoan = (id) => request.post(`/fin/loan/${id}/submit`)
export const payLoan = (id, data) => request.post(`/fin/loan/${id}/pay`, data)
/** data { accountId, amount, date } */
export const repayLoan = (id, data) => request.post(`/fin/loan/${id}/repay`, data)

/** 经营分析 */
export const grossMargin = (params) => request.get('/fin/analysis/gross-margin', { params })
export const inventoryCheck = (period) => request.get('/fin/analysis/inventory', { params: { period } })

/** 币种与汇率 */
export const currencies = () => request.get('/fin/currency')
/** 按币种编码新增或修改：{ code, name, symbol, status, sort } */
export const saveCurrency = (data) => request.put('/fin/currency', data)
/** 汇率：{ period?, currency? } → [{ id, currency, period, rate }] */
export const exchangeRates = (params) => request.get('/fin/currency/rate', { params })
/** 某日的记账汇率，未维护时为空 */
export const rateOf = (currency, date) => request.get('/fin/currency/rate/of', { params: { currency, date } })
/** 新增或修改某月汇率：{ currency, period, rate } */
export const saveRate = (data) => request.put('/fin/currency/rate', data)
/** 期末调汇：{ voucherId, reverseVoucherId, lines } */
export const revalue = (period) => request.post(`/fin/currency/revalue/${period}`)

// ---------------------------------------------------------------- 资金管理：票据、账户调拨、资金计划
export const pageNote = (params) => request.get('/fin/note', { params })
export const getNote = (id) => request.get(`/fin/note/${id}`)
export const receiveNote = (data) => request.post('/fin/note/receive', data)
export const issueNote = (data) => request.post('/fin/note/issue', data)
/** action：endorse 背书、discount 贴现、collect 托收、dishonor 拒付、pay 兑付 */
export const noteAction = (id, action, data) => request.post(`/fin/note/${id}/${action}`, data)
export const undoNote = (id) => request.post(`/fin/note/${id}/undo`)
export const deleteNote = (id) => request.delete(`/fin/note/${id}`)

export const pageFundTransfer = (params) => request.get('/fin/fund-transfer', { params })
export const addFundTransfer = (data) => request.post('/fin/fund-transfer', data)
export const deleteFundTransfer = (id) => request.delete(`/fin/fund-transfer/${ids(id)}`)

export const fundPlan = (params) => request.get('/fin/fund-plan', { params })
export const fundPlanEntries = (params) => request.get('/fin/fund-plan/entries', { params })
export const pageFundPlanItem = (params) => request.get('/fin/fund-plan/items', { params })
export const addFundPlanItem = (data) => request.post('/fin/fund-plan/items', data)
export const updateFundPlanItem = (id, data) => request.put(`/fin/fund-plan/items/${id}`, data)
export const deleteFundPlanItem = (id) => request.delete(`/fin/fund-plan/items/${ids(id)}`)

/** 供应商对账单 */
export const pageStatement = (params) => request.get('/fin/statement', { params })
export const getStatement = (id) => request.get(`/fin/statement/${id}`)
/** 生成：data { supplierId, beginDate, endDate, remark } */
export const generateStatement = (data) => request.post('/fin/statement', data)
export const sendStatement = (id) => request.post(`/fin/statement/${id}/send`)
export const deleteStatement = (id) => request.delete(`/fin/statement/${id}`)

/** 凭证科目设置：[{ role, group, label, dimension, dictType, defaultCode, dimensionDefaults, accountId, items }] */
export const accountRules = () => request.get('/fin/account-rule')
/** 保存一个用途的设置：{ accountId, items: { 维度取值: 科目ID } }，为空的使用预置科目 */
export const saveAccountRule = (role, data) => request.put(`/fin/account-rule/${role}`, data)

/** 出纳：日记账 { accountId?, begin, end, posted } → [{ type, entryDate, voucherNo, summary, counterAccounts, debit, credit, balance }] */
export const cashJournal = (params) => request.get('/fin/cashier/journal', { params })
/** 资金日报 { begin, end, posted } → { rows, total } */
export const cashDaily = (params) => request.get('/fin/cashier/daily', { params })
/** 未生成凭证的收付款 → { RECEIPT: { count, amount }, PAYMENT: {...} } */
export const cashUnvouchered = () => request.get('/fin/cashier/unvouchered')
