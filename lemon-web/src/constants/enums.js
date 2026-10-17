/**
 * 后端枚举常量 —— 由 scripts/gen-enums.mjs 生成，请勿手工修改；后端新增或修改枚举后运行 npm run gen:enums
 *
 * 来源：lemon-common、lemon-framework、lemon-modules 中文件名与枚举同名的顶层 Java 枚举，键和值都是枚举常量名。
 * 命名：Java 简单类名；多个模块有同名枚举时加模块前缀（如 WmsDocStatus、MesDocStatus）。
 * 用法：import { WorkOrderStatus } from '@/constants/enums'；record.status === WorkOrderStatus.DRAFT
 */

/** com.lemon.erp.modules.bi.model.AbcClass */
export const AbcClass = Object.freeze({
  A: 'A', // 累计占比 70% 以内
  B: 'B', // 累计占比 90% 以内
  C: 'C', // 其余
})

/** com.lemon.erp.modules.fin.model.AccountCategory（字典 fin_account_category） */
export const AccountCategory = Object.freeze({
  ASSET: 'ASSET',
  LIABILITY: 'LIABILITY',
  EQUITY: 'EQUITY',
  COST: 'COST',
  PROFIT: 'PROFIT', // 损益类
})

/** com.lemon.erp.modules.fin.model.AccountDirection */
export const AccountDirection = Object.freeze({
  DEBIT: 'DEBIT',
  CREDIT: 'CREDIT',
})

/** com.lemon.erp.modules.fin.model.AccountRole */
export const AccountRole = Object.freeze({
  CASH: 'CASH', // 库存现金
  BANK: 'BANK', // 银行存款
  RECEIVABLE: 'RECEIVABLE',
  PAYABLE: 'PAYABLE',
  ADVANCE_RECEIVED: 'ADVANCE_RECEIVED',
  PREPAYMENT: 'PREPAYMENT',
  NOTES_RECEIVABLE: 'NOTES_RECEIVABLE',
  NOTES_PAYABLE: 'NOTES_PAYABLE',
  OTHER_RECEIVABLE: 'OTHER_RECEIVABLE',
  OTHER_PAYABLE: 'OTHER_PAYABLE',
  INVENTORY: 'INVENTORY', // 默认
  OUTSOURCE_MATERIAL: 'OUTSOURCE_MATERIAL',
  PRODUCTION_COST: 'PRODUCTION_COST',
  MANUFACTURING_OVERHEAD: 'MANUFACTURING_OVERHEAD',
  CONTRACT_COST: 'CONTRACT_COST',
  PENDING_LOSS: 'PENDING_LOSS',
  OTHER_IN_OFFSET: 'OTHER_IN_OFFSET', // 其他入库
  OTHER_OUT_OFFSET: 'OTHER_OUT_OFFSET', // 其他出库、报废出库
  REVENUE: 'REVENUE',
  COST_OF_SALES: 'COST_OF_SALES',
  OTHER_REVENUE: 'OTHER_REVENUE',
  CONTRACT_LIABILITY: 'CONTRACT_LIABILITY',
  SELLING_EXPENSE: 'SELLING_EXPENSE',
  TRAINING_EXPENSE: 'TRAINING_EXPENSE',
  FINANCE_EXPENSE: 'FINANCE_EXPENSE',
  NON_OPERATING_INCOME: 'NON_OPERATING_INCOME',
  NON_OPERATING_EXPENSE: 'NON_OPERATING_EXPENSE',
  INPUT_VAT: 'INPUT_VAT',
  OUTPUT_VAT: 'OUTPUT_VAT',
  UNPAID_VAT: 'UNPAID_VAT',
  CITY_TAX: 'CITY_TAX',
  EDU_SURCHARGE: 'EDU_SURCHARGE',
  LOCAL_EDU_SURCHARGE: 'LOCAL_EDU_SURCHARGE',
  TAX_SURCHARGE: 'TAX_SURCHARGE',
  INCOME_TAX: 'INCOME_TAX',
  FIXED_ASSET: 'FIXED_ASSET',
  DEPRECIATION: 'DEPRECIATION',
  ASSET_DISPOSAL: 'ASSET_DISPOSAL',
  SALARY: 'SALARY',
  SALARY_EXPENSE: 'SALARY_EXPENSE', // 默认
  PROFIT_THIS_YEAR: 'PROFIT_THIS_YEAR',
  PROFIT_DISTRIBUTION: 'PROFIT_DISTRIBUTION',
})

/** com.lemon.erp.modules.bi.model.AlertLevel（字典 bi_alert_level） */
export const AlertLevel = Object.freeze({
  INFO: 'INFO', // 提示
  WARN: 'WARN', // 警告
  DANGER: 'DANGER', // 严重
})

/** com.lemon.erp.modules.bi.model.AlertOperator */
export const AlertOperator = Object.freeze({
  GT: 'GT', // 大于
  GE: 'GE', // 不小于
  LT: 'LT', // 小于
  LE: 'LE', // 不大于
})

/** com.lemon.erp.modules.hr.model.AppraisalStatus（字典 hr_appraisal_status） */
export const AppraisalStatus = Object.freeze({
  SELF: 'SELF', // 待自评
  REVIEW: 'REVIEW', // 待评分
  DONE: 'DONE', // 已完成
})

/** com.lemon.erp.modules.workflow.model.ApprovalAction（字典 wf_task_action） */
export const ApprovalAction = Object.freeze({
  START: 'START', // 发起
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
  TRANSFER: 'TRANSFER',
  CANCEL: 'CANCEL', // 发起人撤回
  TERMINATE: 'TERMINATE', // 管理员终止
  CC: 'CC', // 抄送
  PENDING: 'PENDING', // 待审批
})

/** com.lemon.erp.modules.workflow.model.ApproveMode */
export const ApproveMode = Object.freeze({
  SEQUENTIAL: 'SEQUENTIAL', // 依次审批
  AND: 'AND', // 会签
  OR: 'OR', // 或签
})

/** com.lemon.erp.framework.workflow.ApproveStatus（字典 sys_approve_status） */
export const ApproveStatus = Object.freeze({
  APPROVED: 'APPROVED', // 已生效
  APPROVING: 'APPROVING', // 审批中
  REJECTED: 'REJECTED', // 已拒绝
  CANCELED: 'CANCELED', // 已撤回
  DONE: 'DONE', // 已生效
})

/** com.lemon.erp.modules.scm.model.AsnStatus（字典 scm_asn_status） */
export const AsnStatus = Object.freeze({
  SUBMITTED: 'SUBMITTED',
  RECEIVED: 'RECEIVED',
  CANCELLED: 'CANCELLED',
})

/** com.lemon.erp.modules.wms.model.AssemblyKind（字典 wms_assembly_kind） */
export const AssemblyKind = Object.freeze({
  ASSEMBLE: 'ASSEMBLE', // 组装
  DISASSEMBLE: 'DISASSEMBLE', // 拆卸
})

/** com.lemon.erp.modules.fin.model.AssetCheckResult（字典 fin_asset_check_result） */
export const AssetCheckResult = Object.freeze({
  PENDING: 'PENDING',
  NORMAL: 'NORMAL', // 相符
  MOVED: 'MOVED', // 位置变化
  LOSS: 'LOSS', // 盘亏
  GAIN: 'GAIN', // 盘盈
})

/** com.lemon.erp.modules.fin.model.AssetCheckStatus（字典 fin_asset_check_status） */
export const AssetCheckStatus = Object.freeze({
  CHECKING: 'CHECKING',
  DONE: 'DONE',
})

/** com.lemon.erp.modules.fin.model.AssetMoveType（字典 fin_asset_move_type） */
export const AssetMoveType = Object.freeze({
  ASSIGN: 'ASSIGN', // 领用
  RETURN: 'RETURN', // 归还
  TRANSFER: 'TRANSFER', // 调拨
})

/** com.lemon.erp.modules.fin.model.AssetStatus（字典 fin_asset_status） */
export const AssetStatus = Object.freeze({
  IN_USE: 'IN_USE',
  IDLE: 'IDLE',
  DISPOSED: 'DISPOSED',
})

/** com.lemon.erp.modules.workflow.model.AssigneeType */
export const AssigneeType = Object.freeze({
  USER: 'USER',
  ROLE: 'ROLE',
  DEPT_LEADER: 'DEPT_LEADER', // 发起人的部门主管
  INITIATOR: 'INITIATOR',
})

/** com.lemon.erp.modules.hr.model.AttendanceSource */
export const AttendanceSource = Object.freeze({
  CLOCK: 'CLOCK', // 员工打卡
  MANUAL: 'MANUAL', // 人事补录
})

/** com.lemon.erp.modules.hr.model.AttendanceStatus（字典 hr_attendance_status） */
export const AttendanceStatus = Object.freeze({
  NORMAL: 'NORMAL', // 正常
  LATE: 'LATE', // 迟到
  EARLY: 'EARLY', // 早退
  LATE_EARLY: 'LATE_EARLY', // 迟到早退
  MISSING: 'MISSING', // 缺卡
})

/** com.lemon.erp.modules.fin.model.AuxItem */
export const AuxItem = Object.freeze({
  DEPT: 'DEPT',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  PROJECT: 'PROJECT',
  MATERIAL: 'MATERIAL',
})

/** com.lemon.erp.modules.scm.model.AwardTarget */
export const AwardTarget = Object.freeze({
  CONTRACT: 'CONTRACT', // 采购合同
  ORDER: 'ORDER', // 采购订单
  NONE: 'NONE', // 只定标
})

/** com.lemon.erp.modules.bi.model.BiAlertMetric */
export const BiAlertMetric = Object.freeze({
  AR_OVERDUE: 'AR_OVERDUE',
  STOCK_BELOW_SAFETY: 'STOCK_BELOW_SAFETY',
  SLOW_MOVING: 'SLOW_MOVING',
  GROSS_MARGIN: 'GROSS_MARGIN',
  TICKET_OVERDUE: 'TICKET_OVERDUE',
  TASK_OVERDUE: 'TASK_OVERDUE',
  WO_LATE: 'WO_LATE',
  BUDGET_OVERRUN: 'BUDGET_OVERRUN',
})

/** com.lemon.erp.modules.bi.model.BiCube */
export const BiCube = Object.freeze({
  SALES: 'SALES',
  PURCHASE: 'PURCHASE',
  INVENTORY: 'INVENTORY',
  PRODUCTION: 'PRODUCTION',
  FINANCE: 'FINANCE',
  HR: 'HR',
  CRM: 'CRM',
  PROJECT: 'PROJECT',
})

/** com.lemon.erp.modules.fin.model.BillKind */
export const BillKind = Object.freeze({
  AR: 'AR', // 应收
  AP: 'AP', // 应付
})

/** com.lemon.erp.modules.fin.model.BillSource（字典 fin_bill_source） */
export const BillSource = Object.freeze({
  SALES_OUT: 'SALES_OUT', // 销售出库
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  PURCHASE_IN: 'PURCHASE_IN', // 采购入库
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  MANUAL: 'MANUAL', // 期初 / 手工
  PROJECT: 'PROJECT', // 项目开票
  SHIPMENT: 'SHIPMENT', // 运费
  TICKET: 'TICKET', // 服务收费
  ASSET: 'ASSET', // 固定资产
  NOTE: 'NOTE', // 票据拒付
  QUALITY_CLAIM: 'QUALITY_CLAIM', // 质量索赔
  REBATE: 'REBATE', // 销售返利
})

/** com.lemon.erp.modules.fin.model.BillStatus（字典 fin_bill_status） */
export const BillStatus = Object.freeze({
  UNSETTLED: 'UNSETTLED', // 未核销
  PARTIAL: 'PARTIAL', // 部分核销
  SETTLED: 'SETTLED', // 已核销
})

/** com.lemon.erp.modules.pm.model.BillingStatus（字典 pm_billing_status） */
export const BillingStatus = Object.freeze({
  PLANNED: 'PLANNED', // 待开票
  BILLED: 'BILLED', // 已开票
})

/** com.lemon.erp.modules.mes.model.BoardLineState */
export const BoardLineState = Object.freeze({
  RUNNING: 'RUNNING', // 今天有报工
  IDLE: 'IDLE', // 有待生产的工序
  REST: 'REST', // 今天不排班
  FREE: 'FREE', // 没有待生产的工序
})

/** com.lemon.erp.modules.mes.model.BoardOpState */
export const BoardOpState = Object.freeze({
  DONE: 'DONE',
  DOING: 'DOING',
  WAITING: 'WAITING',
})

/** com.lemon.erp.modules.mes.model.BoardOrderState */
export const BoardOrderState = Object.freeze({
  RUNNING: 'RUNNING', // 今天有报工
  IDLE: 'IDLE', // 已开始但今天没有报工
  WAITING: 'WAITING', // 已下达
  TO_STOCK: 'TO_STOCK', // 工序全部完成
})

/** com.lemon.erp.modules.fin.model.BudgetControl（字典 fin_budget_control） */
export const BudgetControl = Object.freeze({
  NONE: 'NONE', // 不控制
  WARN: 'WARN', // 超预算提示
  BLOCK: 'BLOCK', // 超预算禁止
})

/** com.lemon.erp.modules.crm.model.CampaignStatus（字典 crm_campaign_status） */
export const CampaignStatus = Object.freeze({
  PLANNED: 'PLANNED', // 计划中
  ACTIVE: 'ACTIVE', // 进行中
  FINISHED: 'FINISHED', // 已结束
  CANCELED: 'CANCELED', // 已取消
})

/** com.lemon.erp.modules.hr.model.CandidateStage（字典 hr_candidate_stage） */
export const CandidateStage = Object.freeze({
  NEW: 'NEW', // 新简历
  INTERVIEW: 'INTERVIEW', // 面试中
  OFFER: 'OFFER', // 已发 Offer
  HIRED: 'HIRED', // 已入职
  REJECTED: 'REJECTED', // 已淘汰
})

/** com.lemon.erp.modules.mes.model.CapaSource（字典 qms_capa_source） */
export const CapaSource = Object.freeze({
  NCR: 'NCR',
  COMPLAINT: 'COMPLAINT',
  INTERNAL: 'INTERNAL',
  AUDIT: 'AUDIT',
})

/** com.lemon.erp.modules.mes.model.CapaStatus（字典 qms_capa_status） */
export const CapaStatus = Object.freeze({
  OPEN: 'OPEN', // 处理中
  VERIFYING: 'VERIFYING', // 待效果验证
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.framework.setting.CaptchaMode */
export const CaptchaMode = Object.freeze({
  OFF: 'OFF',
  ALWAYS: 'ALWAYS',
  AFTER_FAILURE: 'AFTER_FAILURE',
})

/** com.lemon.erp.framework.setting.CaptchaType */
export const CaptchaType = Object.freeze({
  MATH: 'MATH',
  CHAR: 'CHAR',
})

/** com.lemon.erp.modules.hr.model.ChangeType（字典 hr_change_type） */
export const ChangeType = Object.freeze({
  ENTRY: 'ENTRY', // 入职
  REGULAR: 'REGULAR', // 转正
  TRANSFER: 'TRANSFER', // 调岗
  SALARY: 'SALARY', // 调薪
  LEAVE: 'LEAVE', // 离职
})

/** com.lemon.erp.modules.fin.model.CheckLevel */
export const CheckLevel = Object.freeze({
  OK: 'OK', // 通过
  WARN: 'WARN', // 提示
  ERROR: 'ERROR', // 错误
})

/** com.lemon.erp.modules.mes.model.ClaimStatus（字典 qms_claim_status） */
export const ClaimStatus = Object.freeze({
  DRAFT: 'DRAFT', // 待确认
  CONFIRMED: 'CONFIRMED', // 已确认
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.crm.model.CollectStatus */
export const CollectStatus = Object.freeze({
  RECEIVED: 'RECEIVED', // 已回款
  PARTIAL: 'PARTIAL', // 部分回款
  OVERDUE: 'OVERDUE', // 逾期
  PENDING: 'PENDING', // 未到期
})

/** com.lemon.erp.modules.fin.model.CommissionBasis（字典 fin_commission_basis） */
export const CommissionBasis = Object.freeze({
  SALES: 'SALES', // 销售额
  PROFIT: 'PROFIT', // 毛利
  RECEIPT: 'RECEIPT', // 回款
})

/** com.lemon.erp.modules.fin.model.CommissionStatus */
export const CommissionStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED',
})

/** com.lemon.erp.modules.workflow.model.ConditionOperator */
export const ConditionOperator = Object.freeze({
  EQ: 'EQ',
  NE: 'NE',
  GT: 'GT',
  GE: 'GE',
  LT: 'LT',
  LE: 'LE',
  IN: 'IN', // 属于其中之一
  CONTAINS: 'CONTAINS', // 文本包含
})

/** com.lemon.erp.modules.fin.model.ConsumableStatus（字典 fin_consumable_status） */
export const ConsumableStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED', // 已生成部门领用出库单
})

/** com.lemon.erp.modules.scm.model.ContractStatus（字典 scm_contract_status） */
export const ContractStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING',
  ACTIVE: 'ACTIVE', // 生效
  CLOSED: 'CLOSED',
})

/** com.lemon.erp.modules.scm.model.ContractType（字典 scm_contract_type） */
export const ContractType = Object.freeze({
  FRAMEWORK: 'FRAMEWORK', // 框架协议
  FIXED: 'FIXED', // 固定数量合同
})

/** com.lemon.erp.modules.wms.model.CostMethod（字典 wms_cost_method） */
export const CostMethod = Object.freeze({
  WEIGHTED: 'WEIGHTED', // 月末一次加权平均
  MOVING: 'MOVING', // 移动加权平均
  FIFO: 'FIFO', // 先进先出
})

/** com.lemon.erp.modules.fin.model.CostRunKind */
export const CostRunKind = Object.freeze({
  RUN: 'RUN', // 月度成本核算
  OPENING: 'OPENING', // 存货期初
})

/** com.lemon.erp.modules.pm.model.CostSource */
export const CostSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工录入
  ISSUE: 'ISSUE', // 项目领料生成
})

/** com.lemon.erp.modules.pm.model.CostType（字典 pm_cost_type） */
export const CostType = Object.freeze({
  LABOR: 'LABOR', // 人工
  MATERIAL: 'MATERIAL', // 材料
  TRAVEL: 'TRAVEL', // 差旅
  OUTSOURCE: 'OUTSOURCE', // 外包
  EQUIPMENT: 'EQUIPMENT', // 设备
  OTHER: 'OTHER', // 其他
})

/** com.lemon.erp.modules.scm.model.CreditControl */
export const CreditControl = Object.freeze({
  ALL: 'ALL', // 审核和出库时都控制
  SHIPMENT: 'SHIPMENT', // 只在出库时控制
  NONE: 'NONE', // 不控制
})

/** com.lemon.erp.modules.system.model.CustomFieldType（字典 sys_custom_field_type） */
export const CustomFieldType = Object.freeze({
  TEXT: 'TEXT',
  TEXTAREA: 'TEXTAREA',
  NUMBER: 'NUMBER', // 数字
  DATE: 'DATE', // 日期
  SELECT: 'SELECT', // 下拉
  BOOL: 'BOOL', // 是否
})

/** com.lemon.erp.modules.crm.model.CustomerStage（字典 crm_customer_stage） */
export const CustomerStage = Object.freeze({
  POTENTIAL: 'POTENTIAL', // 潜在客户
  INTENT: 'INTENT', // 意向客户
  DEAL: 'DEAL', // 成交客户
  LOST: 'LOST', // 流失客户
})

/** com.lemon.erp.modules.dashboard.model.DashboardBillKind */
export const DashboardBillKind = Object.freeze({
  AR: 'AR', // 应收
  AP: 'AP', // 应付
})

/** com.lemon.erp.modules.dashboard.model.DashboardOrderKind */
export const DashboardOrderKind = Object.freeze({
  SALES: 'SALES', // 销售订单
  PURCHASE: 'PURCHASE', // 采购订单
})

/** com.lemon.erp.framework.security.datascope.DataScopeType（字典 sys_data_scope） */
export const DataScopeType = Object.freeze({
  ALL: 'ALL',
  CUSTOM: 'CUSTOM',
  DEPT: 'DEPT',
  DEPT_AND_CHILD: 'DEPT_AND_CHILD',
  SELF: 'SELF',
})

/** com.lemon.erp.modules.bi.model.DateFilter */
export const DateFilter = Object.freeze({
  DATE: 'DATE', // 按业务日期
  PERIOD: 'PERIOD', // 按月份
  SNAPSHOT: 'SNAPSHOT', // 按快照日期
})

/** com.lemon.erp.modules.bi.model.DateRange */
export const DateRange = Object.freeze({
  THIS_MONTH: 'THIS_MONTH', // 本月
  THIS_QUARTER: 'THIS_QUARTER', // 本季度
  THIS_YEAR: 'THIS_YEAR', // 本年
  LAST_12M: 'LAST_12M', // 最近 12 个月
  ALL: 'ALL', // 全部
})

/** com.lemon.erp.modules.hr.model.DeductionType（字典 hr_deduction_type） */
export const DeductionType = Object.freeze({
  CHILD_EDU: 'CHILD_EDU', // 子女教育
  CONTINUING_EDU: 'CONTINUING_EDU', // 继续教育
  HOUSING_LOAN: 'HOUSING_LOAN', // 住房贷款利息
  HOUSING_RENT: 'HOUSING_RENT', // 住房租金
  ELDERLY: 'ELDERLY', // 赡养老人
  INFANT: 'INFANT', // 婴幼儿照护
})

/** com.lemon.erp.modules.mes.model.DeliveryRisk */
export const DeliveryRisk = Object.freeze({
  ON_TIME: 'ON_TIME', // 预计完工不晚于交期
  AT_RISK: 'AT_RISK', // 预计完工晚于交期
  OVERDUE: 'OVERDUE', // 交期已过还没有完工
  UNKNOWN: 'UNKNOWN', // 产能不足
})

/** com.lemon.erp.modules.scm.model.DeliveryStatus（字典 scm_delivery_status） */
export const DeliveryStatus = Object.freeze({
  PENDING: 'PENDING',
  PICKED: 'PICKED',
  SHIPPED: 'SHIPPED',
  CANCELLED: 'CANCELLED',
})

/** com.lemon.erp.modules.system.model.DictKind */
export const DictKind = Object.freeze({
  SYSTEM: 'SYSTEM', // 系统字典
  BUSINESS: 'BUSINESS', // 业务字典
})

/** com.lemon.erp.modules.mes.model.Disposition（字典 qms_disposition） */
export const Disposition = Object.freeze({
  RETURN: 'RETURN', // 退货
  REWORK: 'REWORK', // 返工
  SCRAP: 'SCRAP', // 报废
  CONCESSION: 'CONCESSION', // 让步接收
  SORT: 'SORT', // 挑选
})

/** com.lemon.erp.modules.mes.model.EcnStatus（字典 mes_ecn_status） */
export const EcnStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING',
  APPROVED: 'APPROVED', // 已批准
  EFFECTIVE: 'EFFECTIVE',
  CANCELLED: 'CANCELLED',
})

/** com.lemon.erp.modules.hr.model.EmployeeStatus（字典 hr_emp_status） */
export const EmployeeStatus = Object.freeze({
  PROBATION: 'PROBATION', // 试用期
  REGULAR: 'REGULAR', // 正式
  LEFT: 'LEFT', // 离职
})

/** com.lemon.erp.modules.workflow.model.EmptyAction */
export const EmptyAction = Object.freeze({
  AUTO_PASS: 'AUTO_PASS', // 自动通过
  TO_ADMIN: 'TO_ADMIN', // 转交给超级管理员审批
})

/** com.lemon.erp.modules.mes.model.EquipmentStatus（字典 mes_equipment_status） */
export const EquipmentStatus = Object.freeze({
  RUNNING: 'RUNNING',
  IDLE: 'IDLE',
  REPAIR: 'REPAIR',
  SCRAPPED: 'SCRAPPED',
})

/** com.lemon.erp.modules.bi.model.EtlStatus（字典 bi_etl_status） */
export const EtlStatus = Object.freeze({
  RUNNING: 'RUNNING', // 运行中
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
})

/** com.lemon.erp.modules.bi.model.EtlTrigger */
export const EtlTrigger = Object.freeze({
  MANUAL: 'MANUAL', // 手动
  SCHEDULE: 'SCHEDULE', // 定时
})

/** com.lemon.erp.modules.fin.model.ExpenseClaimStatus（字典 fin_claim_status） */
export const ExpenseClaimStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 待付款
  PAID: 'PAID', // 已付款
})

/** com.lemon.erp.framework.workflow.FlowStatus（字典 wf_status） */
export const FlowStatus = Object.freeze({
  RUNNING: 'RUNNING', // 审批中
  APPROVED: 'APPROVED', // 审批通过
  REJECTED: 'REJECTED', // 审批拒绝
  CANCELED: 'CANCELED', // 发起人撤回或管理员终止
})

/** com.lemon.erp.modules.crm.model.FollowupTarget */
export const FollowupTarget = Object.freeze({
  CUSTOMER: 'CUSTOMER', // 客户
  LEAD: 'LEAD', // 线索
  OPPORTUNITY: 'OPPORTUNITY', // 商机
})

/** com.lemon.erp.modules.workflow.model.FormFieldType */
export const FormFieldType = Object.freeze({
  INPUT: 'INPUT',
  TEXTAREA: 'TEXTAREA',
  NUMBER: 'NUMBER', // 整数保存为 Long
  DATE: 'DATE', // yyyy-MM-dd
  SELECT: 'SELECT',
})

/** com.lemon.erp.modules.fin.model.FundDirection */
export const FundDirection = Object.freeze({
  IN: 'IN',
  OUT: 'OUT',
})

/** com.lemon.erp.modules.fin.model.FundSource */
export const FundSource = Object.freeze({
  AR: 'AR', // 未核销的应收单
  AP: 'AP', // 未核销的应付单
  NOTE_IN: 'NOTE_IN', // 持有中的应收票据到期
  NOTE_OUT: 'NOTE_OUT', // 未兑付的应付票据到期
  CLAIM: 'CLAIM', // 已审批待付款的报销
  PLAN: 'PLAN', // 手工计划项
})

/** com.lemon.erp.modules.wms.model.HoldStatus（字典 wms_hold_status） */
export const HoldStatus = Object.freeze({
  PENDING: 'PENDING', // 待检
  REJECTED: 'REJECTED', // 不合格
  RELEASED: 'RELEASED', // 已放行
  RETURNED: 'RETURNED', // 已退货
})

/** com.lemon.erp.modules.hr.model.HolidayType（字典 hr_holiday_type） */
export const HolidayType = Object.freeze({
  HOLIDAY: 'HOLIDAY', // 法定节假日
  WORKDAY: 'WORKDAY', // 调休上班
})

/** com.lemon.erp.modules.system.im.ImPlatform（字典 sys_im_platform） */
export const ImPlatform = Object.freeze({
  DINGTALK: 'DINGTALK',
  WECOM: 'WECOM',
  FEISHU: 'FEISHU',
})

/** com.lemon.erp.modules.scm.model.InquiryStatus（字典 scm_inquiry_status） */
export const InquiryStatus = Object.freeze({
  QUOTING: 'QUOTING', // 询价中
  FINISHED: 'FINISHED', // 已定标
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.mes.model.InspectionLevel */
export const InspectionLevel = Object.freeze({
  S1: 'S1',
  S2: 'S2',
  S3: 'S3',
  S4: 'S4',
  I: 'I',
  II: 'II',
  III: 'III',
})

/** com.lemon.erp.modules.mes.model.InspectionStatus */
export const InspectionStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  CONFIRMED: 'CONFIRMED', // 已确认
})

/** com.lemon.erp.modules.hr.model.InterviewMethod */
export const InterviewMethod = Object.freeze({
  ONSITE: 'ONSITE', // 现场面试
  VIDEO: 'VIDEO', // 视频面试
  PHONE: 'PHONE', // 电话面试
})

/** com.lemon.erp.modules.hr.model.InterviewResult（字典 hr_interview_result） */
export const InterviewResult = Object.freeze({
  PENDING: 'PENDING', // 待面试
  PASS: 'PASS', // 通过
  FAIL: 'FAIL', // 未通过
})

/** com.lemon.erp.modules.fin.model.InvoiceKind */
export const InvoiceKind = Object.freeze({
  OUTPUT: 'OUTPUT', // 销项发票
  INPUT: 'INPUT', // 进项发票
})

/** com.lemon.erp.modules.fin.model.InvoiceSource */
export const InvoiceSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工登记
  XML: 'XML', // 导入电子发票 XML
  EXCEL: 'EXCEL', // Excel 批量导入
})

/** com.lemon.erp.modules.fin.model.InvoiceStatus */
export const InvoiceStatus = Object.freeze({
  VALID: 'VALID', // 有效
  VOID: 'VOID', // 已作废
})

/** com.lemon.erp.modules.fin.model.InvoiceType（字典 fin_invoice_type） */
export const InvoiceType = Object.freeze({
  SPECIAL: 'SPECIAL', // 增值税专用发票
  NORMAL: 'NORMAL', // 普通发票
  E_SPECIAL: 'E_SPECIAL', // 数电票
  E_NORMAL: 'E_NORMAL', // 数电票
})

/** com.lemon.erp.modules.mes.model.ItemMethod */
export const ItemMethod = Object.freeze({
  QUALITATIVE: 'QUALITATIVE', // 定性
  QUANTITATIVE: 'QUANTITATIVE', // 定量
})

/** com.lemon.erp.modules.system.model.JobRunStatus */
export const JobRunStatus = Object.freeze({
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
})

/** com.lemon.erp.modules.system.model.JobTrigger */
export const JobTrigger = Object.freeze({
  SCHEDULE: 'SCHEDULE', // 按 Cron 表达式定时触发
  MANUAL: 'MANUAL', // 手动立即执行
})

/** com.lemon.erp.modules.mes.model.KitStatus（字典 mes_kit_status） */
export const KitStatus = Object.freeze({
  COMPLETE: 'COMPLETE', // 齐套
  PARTIAL: 'PARTIAL', // 部分缺料
  SHORT: 'SHORT', // 缺料
  ISSUED: 'ISSUED', // 已领齐
})

/** com.lemon.erp.modules.hr.model.LaborContractStatus（字典 hr_contract_status） */
export const LaborContractStatus = Object.freeze({
  ACTIVE: 'ACTIVE', // 生效中
  EXPIRED: 'EXPIRED', // 已到期
  RENEWED: 'RENEWED', // 已续签
  TERMINATED: 'TERMINATED', // 已终止
})

/** com.lemon.erp.modules.crm.model.LeadStatus（字典 crm_lead_status） */
export const LeadStatus = Object.freeze({
  NEW: 'NEW', // 待跟进
  FOLLOWING: 'FOLLOWING', // 跟进中
  CONVERTED: 'CONVERTED', // 已转化
  INVALID: 'INVALID', // 无效
})

/** com.lemon.erp.modules.hr.model.LeaveKind（字典 hr_leave_kind） */
export const LeaveKind = Object.freeze({
  LEAVE: 'LEAVE', // 请假
  OVERTIME: 'OVERTIME', // 加班
})

/** com.lemon.erp.modules.hr.model.LeaveStatus（字典 hr_leave_status） */
export const LeaveStatus = Object.freeze({
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已通过
  REJECTED: 'REJECTED', // 已拒绝
  CANCELED: 'CANCELED', // 已撤销
})

/** com.lemon.erp.modules.fin.model.LedgerRowType */
export const LedgerRowType = Object.freeze({
  OPEN: 'OPEN', // 期初余额
  ENTRY: 'ENTRY', // 凭证分录
  DAY: 'DAY', // 本日合计
  TOTAL: 'TOTAL', // 本期合计
})

/** com.lemon.erp.modules.fin.model.LoanRepayMethod */
export const LoanRepayMethod = Object.freeze({
  CASH: 'CASH', // 现金 / 银行还款
  CLAIM: 'CLAIM', // 报销冲抵
})

/** com.lemon.erp.modules.fin.model.LoanStatus（字典 fin_loan_status） */
export const LoanStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 待付款
  PAID: 'PAID', // 未还清
  CLEARED: 'CLEARED', // 已还清
})

/** com.lemon.erp.modules.wms.model.LocationType（字典 wms_location_type） */
export const LocationType = Object.freeze({
  STORAGE: 'STORAGE',
  PICK: 'PICK',
  RECEIVE: 'RECEIVE',
  SHIP: 'SHIP',
  DEFECT: 'DEFECT',
})

/** com.lemon.erp.modules.mes.model.MaintenanceStatus（字典 mes_maintain_status） */
export const MaintenanceStatus = Object.freeze({
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  DONE: 'DONE',
})

/** com.lemon.erp.modules.mes.model.MaintenanceType（字典 mes_maintain_type） */
export const MaintenanceType = Object.freeze({
  REPAIR: 'REPAIR', // 故障维修
  MAINTAIN: 'MAINTAIN', // 定期保养
})

/** com.lemon.erp.modules.fin.model.MarginGroup */
export const MarginGroup = Object.freeze({
  MATERIAL: 'MATERIAL',
  CUSTOMER: 'CUSTOMER',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.bi.model.MeasureFormat */
export const MeasureFormat = Object.freeze({
  MONEY: 'MONEY', // 金额
  QTY: 'QTY', // 数量
  COUNT: 'COUNT', // 计数
  PERCENT: 'PERCENT', // 百分比
  PRICE: 'PRICE', // 单价
})

/** com.lemon.erp.modules.system.model.MenuType（字典 sys_menu_type） */
export const MenuType = Object.freeze({
  M: 'M', // 目录
  C: 'C', // 菜单
  F: 'F', // 按钮
})

/** com.lemon.erp.modules.mes.model.DocStatus（字典 mes_doc_status） */
export const MesDocStatus = Object.freeze({
  DRAFT: 'DRAFT',
  RELEASED: 'RELEASED',
  OBSOLETE: 'OBSOLETE', // 发布了新版本后作废
})

/** com.lemon.erp.framework.message.MessageCategory（字典 sys_message_category） */
export const MessageCategory = Object.freeze({
  APPROVAL: 'APPROVAL', // 审批
  REMIND: 'REMIND', // 提醒
  ALERT: 'ALERT', // 预警
  SYSTEM: 'SYSTEM', // 系统
})

/** com.lemon.erp.modules.mes.model.MoldAction（字典 mes_mold_action） */
export const MoldAction = Object.freeze({
  ISSUE: 'ISSUE',
  RETURN: 'RETURN',
  USE: 'USE',
  REPAIR: 'REPAIR',
  REPAIRED: 'REPAIRED',
  SCRAP: 'SCRAP',
})

/** com.lemon.erp.modules.mes.model.MoldStatus（字典 mes_mold_status） */
export const MoldStatus = Object.freeze({
  AVAILABLE: 'AVAILABLE',
  IN_USE: 'IN_USE',
  REPAIR: 'REPAIR',
  SCRAPPED: 'SCRAPPED',
})

/** com.lemon.erp.modules.mes.model.MrpResultStatus */
export const MrpResultStatus = Object.freeze({
  NEW: 'NEW', // 待处理
  RELEASED: 'RELEASED', // 已下达
  NONE: 'NONE', // 无需处理
  EXPIRED: 'EXPIRED', // 已失效
})

/** com.lemon.erp.modules.mes.model.NcrSource（字典 qms_ncr_source） */
export const NcrSource = Object.freeze({
  INSPECTION: 'INSPECTION', // 检验不合格
  PRODUCTION: 'PRODUCTION', // 生产发现
  CUSTOMER: 'CUSTOMER', // 客户投诉
  OTHER: 'OTHER', // 其他
})

/** com.lemon.erp.modules.mes.model.NcrStatus（字典 qms_ncr_status） */
export const NcrStatus = Object.freeze({
  OPEN: 'OPEN', // 待评审
  APPROVING: 'APPROVING', // 审批中
  DONE: 'DONE', // 已处置
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.workflow.model.NodeStatus */
export const NodeStatus = Object.freeze({
  DONE: 'DONE', // 已完成
  RUNNING: 'RUNNING', // 进行中
  REJECTED: 'REJECTED', // 已拒绝
})

/** com.lemon.erp.modules.workflow.model.NodeType */
export const NodeType = Object.freeze({
  START: 'START', // 发起人
  APPROVER: 'APPROVER', // 审批人
  CC: 'CC', // 抄送人
  CONDITION: 'CONDITION', // 条件分支
  PARALLEL: 'PARALLEL', // 并行分支
})

/** com.lemon.erp.modules.fin.model.NoteAction（字典 fin_note_action） */
export const NoteAction = Object.freeze({
  RECEIVE: 'RECEIVE', // 收票
  ENDORSE: 'ENDORSE', // 背书转让
  DISCOUNT: 'DISCOUNT',
  COLLECT: 'COLLECT',
  DISHONOR: 'DISHONOR',
  ISSUE: 'ISSUE', // 开具应付票据
  PAY: 'PAY',
})

/** com.lemon.erp.modules.fin.model.NoteKind */
export const NoteKind = Object.freeze({
  RECEIVABLE: 'RECEIVABLE', // 应收票据
  PAYABLE: 'PAYABLE', // 应付票据
})

/** com.lemon.erp.modules.fin.model.NoteStatus（字典 fin_note_status） */
export const NoteStatus = Object.freeze({
  HELD: 'HELD', // 应收票据
  ENDORSING: 'ENDORSING', // 应收票据
  ENDORSED: 'ENDORSED',
  DISCOUNTED: 'DISCOUNTED',
  COLLECTED: 'COLLECTED',
  DISHONORED: 'DISHONORED',
  ISSUING: 'ISSUING', // 应付票据
  ISSUED: 'ISSUED', // 应付票据
  PAID: 'PAID',
  CANCELLED: 'CANCELLED', // 应付票据
})

/** com.lemon.erp.modules.fin.model.NoteType（字典 fin_note_type） */
export const NoteType = Object.freeze({
  BANK: 'BANK', // 银行承兑汇票
  COMMERCIAL: 'COMMERCIAL', // 商业承兑汇票
})

/** com.lemon.erp.framework.log.OperType（字典 sys_oper_type） */
export const OperType = Object.freeze({
  OTHER: 'OTHER',
  INSERT: 'INSERT',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE',
  GRANT: 'GRANT',
  EXPORT: 'EXPORT',
  FORCE: 'FORCE',
  CLEAN: 'CLEAN',
  IMPORT: 'IMPORT',
})

/** com.lemon.erp.modules.crm.model.OpportunityStage（字典 crm_opp_stage） */
export const OpportunityStage = Object.freeze({
  INITIAL: 'INITIAL', // 初步接洽
  NEEDS: 'NEEDS', // 需求确认
  PROPOSAL: 'PROPOSAL', // 方案报价
  NEGOTIATION: 'NEGOTIATION', // 商务谈判
  WON: 'WON', // 赢单
  LOST: 'LOST', // 输单
})

/** com.lemon.erp.modules.scm.model.OrderChangeAction */
export const OrderChangeAction = Object.freeze({
  ADD: 'ADD', // 新增明细
  MODIFY: 'MODIFY', // 修改数量 / 单价
  REMOVE: 'REMOVE', // 删除明细
})

/** com.lemon.erp.modules.scm.model.OrderChangeStatus（字典 scm_change_status） */
export const OrderChangeStatus = Object.freeze({
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已生效
  REJECTED: 'REJECTED', // 已驳回
})

/** com.lemon.erp.modules.scm.model.OrderKind */
export const OrderKind = Object.freeze({
  PURCHASE: 'PURCHASE',
  SALES: 'SALES',
})

/** com.lemon.erp.modules.scm.model.OrderSource（字典 scm_order_source） */
export const OrderSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工
  MRP: 'MRP', // MRP
  KIT: 'KIT', // 生产缺料
  CRM: 'CRM', // 商机
  REQUISITION: 'REQUISITION', // 请购单
  INQUIRY: 'INQUIRY', // 询价单
  QUOTATION: 'QUOTATION', // 销售报价
  CONSIGN: 'CONSIGN', // 寄售结算
  TENDER: 'TENDER', // 招标
  DEALER: 'DEALER', // 经销商订货
  IMPORT: 'IMPORT', // 外部导入
})

/** com.lemon.erp.modules.scm.model.OrderStatus（字典 scm_order_status） */
export const OrderStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已审核
  PARTIAL: 'PARTIAL', // 部分入库 / 出库
  COMPLETED: 'COMPLETED', // 已完成
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.hr.model.OvertimeComp（字典 hr_overtime_comp） */
export const OvertimeComp = Object.freeze({
  PAY: 'PAY', // 加班费
  TO_LEAVE: 'TO_LEAVE', // 转调休
})

/** com.lemon.erp.modules.fin.model.PartnerLedgerType */
export const PartnerLedgerType = Object.freeze({
  BILL: 'BILL',
  SETTLEMENT: 'SETTLEMENT',
  ADVANCE: 'ADVANCE',
})

/** com.lemon.erp.framework.setting.PasswordComplexity */
export const PasswordComplexity = Object.freeze({
  LETTER_DIGIT: 'LETTER_DIGIT',
  MIXED_CASE_DIGIT: 'MIXED_CASE_DIGIT',
  LETTER_DIGIT_SYMBOL: 'LETTER_DIGIT_SYMBOL',
})

/** com.lemon.erp.modules.hr.model.PayrollStatus（字典 hr_payroll_status） */
export const PayrollStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  CONFIRMED: 'CONFIRMED', // 已确认
  PAID: 'PAID', // 已发放
  APPROVING: 'APPROVING', // 审批中
})

/** com.lemon.erp.modules.hr.model.PerfGrade（字典 hr_perf_grade） */
export const PerfGrade = Object.freeze({
  S: 'S', // S 卓越
  A: 'A', // A 优秀
  B: 'B', // B 良好
  C: 'C', // C 合格
  D: 'D', // D 待改进
})

/** com.lemon.erp.modules.hr.model.PerfPeriodType */
export const PerfPeriodType = Object.freeze({
  MONTH: 'MONTH', // 月度
  QUARTER: 'QUARTER', // 季度
  YEAR: 'YEAR', // 年度
})

/** com.lemon.erp.modules.hr.model.PerfStatus（字典 hr_perf_status） */
export const PerfStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  SCORING: 'SCORING', // 考核中
  FINISHED: 'FINISHED', // 已归档
})

/** com.lemon.erp.modules.fin.model.PeriodStatus */
export const PeriodStatus = Object.freeze({
  OPEN: 'OPEN', // 未结账
  CLOSED: 'CLOSED', // 已结账
})

/** com.lemon.erp.modules.mes.model.PlanSource */
export const PlanSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工新建
  SALES: 'SALES', // 由销售订单生成
})

/** com.lemon.erp.modules.mes.model.PlanStatus（字典 mes_plan_status） */
export const PlanStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED',
  CLOSED: 'CLOSED',
})

/** com.lemon.erp.modules.fin.model.PlanUnit */
export const PlanUnit = Object.freeze({
  WEEK: 'WEEK',
  MONTH: 'MONTH',
})

/** com.lemon.erp.framework.portal.PortalKind */
export const PortalKind = Object.freeze({
  SUPPLIER: 'SUPPLIER',
  DEALER: 'DEALER',
})

/** com.lemon.erp.modules.scm.model.PriceControlMode（字典 scm_price_control_mode） */
export const PriceControlMode = Object.freeze({
  WARN: 'WARN', // 提示
  BLOCK: 'BLOCK', // 禁止保存
})

/** com.lemon.erp.modules.pm.model.ProjectStatus（字典 pm_project_status） */
export const ProjectStatus = Object.freeze({
  PLANNING: 'PLANNING', // 规划中
  ACTIVE: 'ACTIVE', // 进行中
  SUSPENDED: 'SUSPENDED', // 已暂停
  FINISHED: 'FINISHED', // 已完成
  CLOSED: 'CLOSED', // 已关闭
  APPROVING: 'APPROVING', // 立项审批中
})

/** com.lemon.erp.modules.scm.model.PromoType（字典 scm_promo_type） */
export const PromoType = Object.freeze({
  DISCOUNT: 'DISCOUNT',
  REDUCE: 'REDUCE',
  GIFT: 'GIFT',
})

/** com.lemon.erp.modules.mes.model.QcResult（字典 mes_qc_result） */
export const QcResult = Object.freeze({
  PASS: 'PASS', // 合格
  FAIL: 'FAIL', // 不合格
  CONCESSION: 'CONCESSION', // 让步接收
})

/** com.lemon.erp.modules.mes.model.QcType（字典 mes_qc_type） */
export const QcType = Object.freeze({
  IQC: 'IQC', // 来料检验
  IPQC: 'IPQC', // 过程检验
  FQC: 'FQC', // 成品检验
  FAI: 'FAI', // 首件检验
})

/** com.lemon.erp.modules.mes.model.QualitySummaryBy */
export const QualitySummaryBy = Object.freeze({
  TYPE: 'TYPE', // 按检验类型
  SUPPLIER: 'SUPPLIER', // 按供应商
})

/** com.lemon.erp.modules.scm.model.QuotationStatus（字典 scm_quotation_status） */
export const QuotationStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已批准
  ORDERED: 'ORDERED', // 已成交
  LOST: 'LOST', // 已失效
})

/** com.lemon.erp.modules.scm.model.RebateSettle（字典 scm_rebate_settle） */
export const RebateSettle = Object.freeze({
  CREDIT_NOTE: 'CREDIT_NOTE',
  NEXT_ORDER: 'NEXT_ORDER',
})

/** com.lemon.erp.modules.scm.model.RebateStatus（字典 scm_rebate_status） */
export const RebateStatus = Object.freeze({
  ACTIVE: 'ACTIVE',
  SETTLED: 'SETTLED',
})

/** com.lemon.erp.modules.hr.model.RecruitStatus（字典 hr_recruit_status） */
export const RecruitStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  OPEN: 'OPEN', // 招聘中
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.framework.ref.RefType */
export const RefType = Object.freeze({
  MATERIAL: 'MATERIAL',
  WAREHOUSE: 'WAREHOUSE',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  USER: 'USER',
  DEPT: 'DEPT',
})

/** com.lemon.erp.modules.scm.model.RequisitionStatus（字典 scm_requisition_status） */
export const RequisitionStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已批准
  ORDERED: 'ORDERED', // 已下单
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.wms.model.ReservationSource */
export const ReservationSource = Object.freeze({
  SALES: 'SALES', // 销售订单
})

/** com.lemon.erp.modules.scm.model.SalesChannel（字典 scm_channel） */
export const SalesChannel = Object.freeze({
  DIRECT: 'DIRECT',
  DEALER: 'DEALER',
})

/** com.lemon.erp.modules.crm.model.SalesContractStatus（字典 crm_contract_status） */
export const SalesContractStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  ACTIVE: 'ACTIVE', // 执行中
  COMPLETED: 'COMPLETED', // 已完成
  TERMINATED: 'TERMINATED', // 已终止
})

/** com.lemon.erp.modules.mes.model.SamplingMode（字典 qms_sampling_mode） */
export const SamplingMode = Object.freeze({
  FULL: 'FULL', // 全检
  FIXED: 'FIXED', // 固定数量
  AQL: 'AQL', // 按 GB/T 2828.1 正常检验一次抽样
})

/** com.lemon.erp.modules.bi.model.ScreenKey */
export const ScreenKey = Object.freeze({
  FACTORY: 'FACTORY',
  PROGRESS: 'PROGRESS',
  WAREHOUSE: 'WAREHOUSE',
  SALES: 'SALES',
  QUALITY: 'QUALITY',
})

/** com.lemon.erp.modules.wms.model.SerialStatus（字典 wms_serial_status） */
export const SerialStatus = Object.freeze({
  IN_STOCK: 'IN_STOCK',
  OUT: 'OUT',
})

/** com.lemon.erp.framework.setting.Setting */
export const Setting = Object.freeze({
  SYSTEM_NAME: 'SYSTEM_NAME',
  SHOW_DEMO_ACCOUNT: 'SHOW_DEMO_ACCOUNT',
  WATERMARK: 'WATERMARK',
  CAPTCHA_MODE: 'CAPTCHA_MODE',
  CAPTCHA_TYPE: 'CAPTCHA_TYPE',
  LOGIN_MAX_RETRY: 'LOGIN_MAX_RETRY',
  LOGIN_LOCK_MINUTES: 'LOGIN_LOCK_MINUTES',
  SESSION_EXPIRE_HOURS: 'SESSION_EXPIRE_HOURS',
  SESSION_SINGLE: 'SESSION_SINGLE',
  PASSWORD_MIN_LENGTH: 'PASSWORD_MIN_LENGTH',
  PASSWORD_COMPLEXITY: 'PASSWORD_COMPLEXITY',
  PASSWORD_EXPIRE_DAYS: 'PASSWORD_EXPIRE_DAYS',
  USER_INITIAL_PASSWORD: 'USER_INITIAL_PASSWORD',
  ATTACHMENT_MAX_MB: 'ATTACHMENT_MAX_MB',
})

/** com.lemon.erp.framework.setting.SettingType */
export const SettingType = Object.freeze({
  TEXT: 'TEXT',
  NUMBER: 'NUMBER',
  BOOL: 'BOOL',
  SELECT: 'SELECT',
  DECIMAL: 'DECIMAL', // 小数
  TABLE: 'TABLE', // 表格
})

/** com.lemon.erp.modules.fin.model.SettleKind */
export const SettleKind = Object.freeze({
  RECEIPT: 'RECEIPT', // 收款
  PAYMENT: 'PAYMENT', // 付款
})

/** com.lemon.erp.modules.scm.model.ShipmentStatus（字典 scm_shipment_status） */
export const ShipmentStatus = Object.freeze({
  PENDING: 'PENDING', // 待发运
  SHIPPED: 'SHIPPED', // 运输中
  SIGNED: 'SIGNED', // 已签收
  EXCEPTION: 'EXCEPTION', // 异常
})

/** com.lemon.erp.modules.mes.model.SpcChart */
export const SpcChart = Object.freeze({
  I_MR: 'I_MR', // 单值-移动极差图
  XBAR_R: 'XBAR_R', // 均值-极差图
  XBAR_S: 'XBAR_S', // 均值-标准差图
})

/** com.lemon.erp.modules.fin.model.StatementStatus（字典 fin_statement_status） */
export const StatementStatus = Object.freeze({
  DRAFT: 'DRAFT',
  SENT: 'SENT', // 已发送
  CONFIRMED: 'CONFIRMED',
  DISPUTED: 'DISPUTED', // 供应商有异议
})

/** com.lemon.erp.modules.fin.model.StdCostStatus（字典 fin_std_cost_status） */
export const StdCostStatus = Object.freeze({
  DRAFT: 'DRAFT',
  RELEASED: 'RELEASED',
})

/** com.lemon.erp.modules.wms.model.StockBizType（字典 wms_biz_type） */
export const StockBizType = Object.freeze({
  IN: 'IN',
  OUT: 'OUT',
  TRANSFER: 'TRANSFER',
  CHECK: 'CHECK',
})

/** com.lemon.erp.modules.wms.model.StockInType（字典 wms_in_type） */
export const StockInType = Object.freeze({
  PURCHASE: 'PURCHASE', // 采购入库
  PRODUCTION: 'PRODUCTION', // 生产入库
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  OTHER: 'OTHER', // 其他入库
  PRODUCTION_RETURN: 'PRODUCTION_RETURN', // 生产退料
  OUTSOURCE_RETURN: 'OUTSOURCE_RETURN', // 委外收回
  SCRAP_IN: 'SCRAP_IN', // 报废入库
  CONSIGN_IN: 'CONSIGN_IN', // 寄售入库
  OPENING: 'OPENING', // 期初入库
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
})

/** com.lemon.erp.modules.wms.model.StockOutType（字典 wms_out_type） */
export const StockOutType = Object.freeze({
  SALES: 'SALES', // 销售出库
  PRODUCTION: 'PRODUCTION', // 生产领料
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  OTHER: 'OTHER', // 其他出库
  PROJECT: 'PROJECT', // 项目领料
  OUTSOURCE: 'OUTSOURCE', // 委外发料
  SERVICE: 'SERVICE', // 服务领用
  SCRAP: 'SCRAP', // 报废出库
  CONSIGN: 'CONSIGN', // 寄售消耗
  CONSUME: 'CONSUME', // 部门领用
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
})

/** com.lemon.erp.modules.mes.model.StopReason（字典 mes_stop_reason） */
export const StopReason = Object.freeze({
  BREAKDOWN: 'BREAKDOWN',
  CHANGEOVER: 'CHANGEOVER',
  MATERIAL: 'MATERIAL',
  MAINTAIN: 'MAINTAIN',
  NO_ORDER: 'NO_ORDER',
  OTHER: 'OTHER',
})

/** com.lemon.erp.modules.mes.model.SupplementStatus（字典 mes_supplement_status） */
export const SupplementStatus = Object.freeze({
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 待发料
  DONE: 'DONE', // 已发料
  REJECTED: 'REJECTED', // 已拒绝
  CANCELED: 'CANCELED', // 已撤销
})

/** com.lemon.erp.modules.scm.model.SupplierConfirm（字典 scm_supplier_confirm） */
export const SupplierConfirm = Object.freeze({
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
})

/** com.lemon.erp.modules.scm.model.SupplierLevel（字典 scm_supplier_level） */
export const SupplierLevel = Object.freeze({
  A: 'A', // A 优秀
  B: 'B', // B 良好
  C: 'C', // C 合格
  D: 'D', // D 待改进
})

/** com.lemon.erp.modules.mes.model.SupplyType（字典 mes_supply_type） */
export const SupplyType = Object.freeze({
  MAKE: 'MAKE', // 自制
  BUY: 'BUY', // 采购
  OUTSOURCE: 'OUTSOURCE', // 委外
  PHANTOM: 'PHANTOM', // 虚拟件
})

/** com.lemon.erp.modules.pm.model.TaskStatus（字典 pm_task_status） */
export const TaskStatus = Object.freeze({
  TODO: 'TODO', // 未开始
  DOING: 'DOING', // 进行中
  DONE: 'DONE', // 已完成
})

/** com.lemon.erp.modules.scm.model.TenderStatus（字典 scm_tender_status） */
export const TenderStatus = Object.freeze({
  DRAFT: 'DRAFT',
  BIDDING: 'BIDDING', // 已发布
  OPENED: 'OPENED', // 已开标
  AWARDED: 'AWARDED',
  CANCELLED: 'CANCELLED',
})

/** com.lemon.erp.modules.crm.model.TicketAction */
export const TicketAction = Object.freeze({
  CREATE: 'CREATE', // 受理
  ASSIGN: 'ASSIGN', // 指派
  PROCESS: 'PROCESS', // 处理
  RESOLVE: 'RESOLVE', // 解决
  CLOSE: 'CLOSE', // 关闭
  REOPEN: 'REOPEN', // 重新打开
})

/** com.lemon.erp.modules.crm.model.TicketPriority（字典 crm_ticket_priority） */
export const TicketPriority = Object.freeze({
  URGENT: 'URGENT', // 紧急
  HIGH: 'HIGH', // 高
  MEDIUM: 'MEDIUM', // 中
  LOW: 'LOW', // 低
})

/** com.lemon.erp.modules.crm.model.TicketStatus（字典 crm_ticket_status） */
export const TicketStatus = Object.freeze({
  OPEN: 'OPEN', // 待处理
  PROCESSING: 'PROCESSING', // 处理中
  RESOLVED: 'RESOLVED', // 已解决
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.pm.model.TimesheetStatus（字典 pm_timesheet_status） */
export const TimesheetStatus = Object.freeze({
  SUBMITTED: 'SUBMITTED', // 待审批
  APPROVED: 'APPROVED', // 已通过
  REJECTED: 'REJECTED', // 已驳回
})

/** com.lemon.erp.modules.scm.model.TradeKind（字典 scm_trade_kind） */
export const TradeKind = Object.freeze({
  PURCHASE_IN: 'PURCHASE_IN',
  PURCHASE_RETURN: 'PURCHASE_RETURN',
  SALES_OUT: 'SALES_OUT',
  SALES_RETURN: 'SALES_RETURN',
})

/** com.lemon.erp.modules.hr.model.TrainingStatus（字典 hr_training_status） */
export const TrainingStatus = Object.freeze({
  PLANNED: 'PLANNED', // 计划中
  ONGOING: 'ONGOING', // 进行中
  FINISHED: 'FINISHED', // 已结束
  CANCELED: 'CANCELED', // 已取消
})

/** com.lemon.erp.modules.fin.model.VoucherSource（字典 fin_voucher_source） */
export const VoucherSource = Object.freeze({
  MANUAL: 'MANUAL',
  AR: 'AR', // 应收单
  AP: 'AP', // 应付单
  RECEIPT: 'RECEIPT', // 收款单
  PAYMENT: 'PAYMENT', // 付款单
  DEPRECIATION: 'DEPRECIATION', // 折旧计提
  ASSET: 'ASSET', // 资产处置
  COST: 'COST', // 成本结转
  CLOSE: 'CLOSE', // 期末结转损益
  SALARY: 'SALARY', // 工资计提、发放
  PROJECT: 'PROJECT',
  EXPENSE: 'EXPENSE',
  ADVANCE: 'ADVANCE', // 预收预付及冲抵
  BAD_DEBT: 'BAD_DEBT', // 坏账核销
  REVERSAL: 'REVERSAL', // 红字冲销
  TAX: 'TAX', // 增值税结转、附加税计提
  CLAIM: 'CLAIM', // 费用报销
  LOAN: 'LOAN', // 员工借款、还款
  FX: 'FX', // 期末调汇
  NOTE: 'NOTE', // 票据贴现、托收、拒付、兑付
  TRANSFER: 'TRANSFER', // 账户调拨
})

/** com.lemon.erp.modules.fin.model.VoucherStatus（字典 fin_voucher_status） */
export const VoucherStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING',
  AUDITED: 'AUDITED',
  POSTED: 'POSTED',
})

/** com.lemon.erp.modules.wms.model.WarehouseType（字典 wms_warehouse_type） */
export const WarehouseType = Object.freeze({
  NORMAL: 'NORMAL', // 普通仓
  RAW: 'RAW', // 原料仓
  FINISHED: 'FINISHED', // 成品仓
  DEFECT: 'DEFECT', // 不良品仓
  CONSIGN_OUT: 'CONSIGN_OUT', // 客户寄售仓
  CONSIGN_IN: 'CONSIGN_IN', // 供应商寄售仓
})

/** com.lemon.erp.modules.scm.model.WaveStatus（字典 scm_wave_status） */
export const WaveStatus = Object.freeze({
  PICKING: 'PICKING',
  DONE: 'DONE',
  CANCELLED: 'CANCELLED',
})

/** com.lemon.erp.modules.wms.model.DocStatus（字典 wms_doc_status） */
export const WmsDocStatus = Object.freeze({
  DRAFT: 'DRAFT',
  TRANSIT: 'TRANSIT', // 在途
  APPROVING: 'APPROVING', // 审批中
  DONE: 'DONE',
})

/** com.lemon.erp.modules.mes.model.WoDocKind（字典 mes_wo_doc_kind） */
export const WoDocKind = Object.freeze({
  ISSUE: 'ISSUE', // 领料
  RETURN: 'RETURN', // 退料
  FINISH: 'FINISH', // 完工入库
  SUPPLEMENT: 'SUPPLEMENT', // 补料
  SCRAP: 'SCRAP', // 报废入库
})

/** com.lemon.erp.modules.mes.model.WorkOrderSource */
export const WorkOrderSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工新建
  MRP: 'MRP', // MRP 下达
  PLAN: 'PLAN', // 生产计划转工单
  SPLIT: 'SPLIT', // 从其他工单拆分
  REWORK: 'REWORK', // 返工
  KIT: 'KIT', // 齐套分析按缺料生成
})

/** com.lemon.erp.modules.mes.model.WorkOrderStatus（字典 mes_wo_status） */
export const WorkOrderStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  RELEASED: 'RELEASED', // 已下达
  STARTED: 'STARTED', // 生产中
  COMPLETED: 'COMPLETED', // 已完工
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.mes.model.WorkOrderType（字典 mes_wo_type） */
export const WorkOrderType = Object.freeze({
  NORMAL: 'NORMAL', // 普通
  REWORK: 'REWORK', // 返工
})
