// 字典标签英文翻译：dict_type -> { dict_value: 英文 }（与 sql/init.sql 一致），中文直接使用数据库中的标签
export default {
  // 通用状态
  sys_common_status: {
    '1': 'Enabled', // 启用
    '0': 'Disabled', // 停用
  },
  // 显示状态
  sys_show_hide: {
    '1': 'Visible', // 显示
    '0': 'Hidden', // 隐藏
  },
  // 菜单类型
  sys_menu_type: {
    'M': 'Directory', // 目录
    'C': 'Menu', // 菜单
    'F': 'Button', // 按钮
  },
  // 操作类型
  sys_oper_type: {
    '0': 'Other', // 其他
    '1': 'Create', // 新增
    '2': 'Update', // 修改
    '3': 'Delete', // 删除
    '4': 'Grant', // 授权
    '5': 'Export', // 导出
    '6': 'Force Logout', // 强退
    '7': 'Clear', // 清空
    '8': 'Import', // 导入
  },
  // 成功失败
  sys_success_fail: {
    '1': 'Success', // 成功
    '0': 'Failed', // 失败
  },
  // 数据权限
  sys_data_scope: {
    '1': 'All Data', // 全部数据
    '2': 'Custom', // 自定义数据
    '3': 'Own Department', // 本部门数据
    '4': 'Own Department & Below', // 本部门及以下数据
    '5': 'Self Only', // 仅本人数据
  },
  // 流程分类
  wf_category: {
    'PURCHASE': 'Purchasing', // 采购
    'SALES': 'Sales', // 销售
    'INVENTORY': 'Inventory', // 库存
    'FINANCE': 'Finance', // 财务
    'OTHER': 'Other', // 其他
    'PRODUCTION': 'Production', // 生产
    'HR': 'HR', // 人事
    'PROJECT': 'Project', // 项目
    'ADMIN': 'Administration', // 行政
    'QUALITY': 'Quality', // 质量
  },
  // 流程状态
  wf_status: {
    'RUNNING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已通过
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤销
  },
  // 审批动作
  wf_task_action: {
    'APPROVE': 'Approve', // 同意
    'REJECT': 'Reject', // 拒绝
    'TRANSFER': 'Transfer', // 转交
  },
  // 物料分类
  wms_material_category: {
    'RAW': 'Raw Material', // 原材料
    'SEMI': 'Semi-finished', // 半成品
    'FINISHED': 'Finished Goods', // 成品
    'PACKING': 'Packaging', // 包装材料
    'OTHER': 'Other', // 其他
    'LOW_VALUE': 'Low-value Consumable', // 低值易耗品
  },
  // 入库类型
  wms_in_type: {
    'PURCHASE': 'Purchase Receipt', // 采购入库
    'PRODUCTION': 'Production Receipt', // 生产入库
    'SALES_RETURN': 'Sales Return', // 销售退货
    'OTHER': 'Other Inbound', // 其他入库
    'PRODUCTION_RETURN': 'Production Return', // 生产退料
    'OUTSOURCE_RETURN': 'Outsourcing Receipt', // 委外收回
    'SCRAP_IN': 'Scrap Receipt', // 报废入库
    'CONSIGN_IN': 'Consignment Inbound', // 寄售入库
    'OPENING': 'Opening Inbound', // 期初入库
    'ASSEMBLY': 'Assembly/Disassembly', // 组装拆卸
  },
  // 出库类型
  wms_out_type: {
    'SALES': 'Sales Delivery', // 销售出库
    'PRODUCTION': 'Material Issue', // 生产领料
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'OTHER': 'Other Outbound', // 其他出库
    'PROJECT': 'Project Issue', // 项目领料
    'OUTSOURCE': 'Outsourcing Issue', // 委外发料
    'SERVICE': 'Service Issue', // 服务领用
    'SCRAP': 'Scrap Outbound', // 报废出库
    'CONSIGN': 'Consignment Consumption', // 寄售消耗
    'CONSUME': 'Department Issue', // 部门领用
    'ASSEMBLY': 'Assembly/Disassembly', // 组装拆卸
  },
  // 单据状态
  wms_doc_status: {
    'DRAFT': 'Draft', // 草稿
    'DONE': 'Completed', // 已完成
    'TRANSIT': 'In Transit', // 在途
    'APPROVING': 'In Approval', // 审批中
  },
  // 库存业务类型
  wms_biz_type: {
    'IN': 'Inbound', // 入库
    'OUT': 'Outbound', // 出库
    'TRANSFER': 'Transfer', // 调拨
    'CHECK': 'Stock Count', // 盘点
  },
  // 序列号状态
  wms_serial_status: {
    'IN_STOCK': 'In Stock', // 在库
    'OUT': 'Shipped Out', // 已出库
  },
  // 供应商等级
  scm_supplier_level: {
    'A': 'A - Excellent', // A 优秀
    'B': 'B - Good', // B 良好
    'C': 'C - Qualified', // C 合格
    'D': 'D - Needs Improvement', // D 待改进
  },
  // 客户等级
  scm_customer_level: {
    'VIP': 'Key Account', // 重点客户
    'NORMAL': 'Regular', // 普通客户
    'POTENTIAL': 'Prospect', // 潜在客户
  },
  // 订单状态
  scm_order_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已审核
    'PARTIAL': 'Partially Completed', // 部分完成
    'COMPLETED': 'Completed', // 已完成
    'CLOSED': 'Closed', // 已关闭
  },
  // 出入库种类
  scm_trade_kind: {
    'PURCHASE_IN': 'Purchase Receipt', // 采购入库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'SALES_OUT': 'Sales Delivery', // 销售出库
    'SALES_RETURN': 'Sales Return', // 销售退货
  },
  // 发运状态
  scm_shipment_status: {
    'PENDING': 'To Ship', // 待发运
    'SHIPPED': 'In Transit', // 运输中
    'SIGNED': 'Delivered', // 已签收
    'EXCEPTION': 'Exception', // 异常
  },
  // 承运商
  scm_carrier: {
    'SF': 'SF Express', // 顺丰速运
    'JD': 'JD Logistics', // 京东物流
    'ZTO': 'ZTO Express', // 中通快递
    'DEPPON': 'Deppon Logistics', // 德邦物流
    'SELF': 'Pickup / Self-delivery', // 自提 / 自送
  },
  // 订单来源
  scm_order_source: {
    'MANUAL': 'Manual', // 手工
    'MRP': 'MRP', // MRP
    'KIT': 'Production Shortage', // 生产缺料
    'CRM': 'Opportunity', // 商机
    'REQUISITION': 'Requisition', // 请购单
    'INQUIRY': 'RFQ', // 询价单
    'QUOTATION': 'Sales Quotation', // 销售报价
    'CONSIGN': 'Consignment Settlement', // 寄售结算
    'TENDER': 'Tender', // 招标
    'DEALER': 'Dealer Order', // 经销商订货
    'IMPORT': 'External Import', // 外部导入
  },
  // 生产计划状态
  mes_plan_status: {
    'DRAFT': 'Draft', // 草稿
    'CONFIRMED': 'Confirmed', // 已确认
    'CLOSED': 'Closed', // 已关闭
  },
  // 工单状态
  mes_wo_status: {
    'DRAFT': 'Draft', // 草稿
    'RELEASED': 'Released', // 已下达
    'STARTED': 'In Production', // 生产中
    'COMPLETED': 'Completed', // 已完工
    'CLOSED': 'Closed', // 已关闭
  },
  // 供应方式
  mes_supply_type: {
    'MAKE': 'Make', // 自制
    'BUY': 'Buy', // 采购
    'OUTSOURCE': 'Outsource', // 委外
    'PHANTOM': 'Phantom', // 虚拟件
  },
  // 工单单据
  mes_wo_doc_kind: {
    'ISSUE': 'Material Issue', // 领料
    'RETURN': 'Material Return', // 退料
    'FINISH': 'Completion Receipt', // 完工入库
    'SUPPLEMENT': 'Supplementary Issue', // 补料
    'SCRAP': 'Scrap Receipt', // 报废入库
  },
  // 检验类型
  mes_qc_type: {
    'IQC': 'Incoming Inspection', // 来料检验
    'IPQC': 'In-process Inspection', // 过程检验
    'FQC': 'Final Inspection', // 成品检验
    'FAI': 'First Article Inspection', // 首件检验
  },
  // 检验结论
  mes_qc_result: {
    'PASS': 'Pass', // 合格
    'FAIL': 'Fail', // 不合格
    'CONCESSION': 'Accepted on Concession', // 让步接收
  },
  // 不良类型
  mes_defect_type: {
    'APPEARANCE': 'Cosmetic Defect', // 外观不良
    'DIMENSION': 'Dimensional Deviation', // 尺寸偏差
    'FUNCTION': 'Functional Failure', // 功能失效
    'SOLDER': 'Soldering Defect', // 焊接不良
    'MATERIAL': 'Incoming Material Defect', // 来料不良
    'OTHER': 'Other', // 其他
  },
  // 设备状态
  mes_equipment_status: {
    'RUNNING': 'Running', // 运行
    'IDLE': 'Idle', // 闲置
    'REPAIR': 'Under Repair', // 维修中
    'SCRAPPED': 'Scrapped', // 报废
  },
  // 维护类型
  mes_maintain_type: {
    'REPAIR': 'Breakdown Repair', // 故障维修
    'MAINTAIN': 'Preventive Maintenance', // 定期保养
  },
  // 维护状态
  mes_maintain_status: {
    'PENDING': 'Pending', // 待处理
    'IN_PROGRESS': 'In Progress', // 处理中
    'DONE': 'Completed', // 已完成
  },
  // 补料原因
  mes_supplement_reason: {
    'SCRAP': 'Production Scrap', // 生产报废
    'LOSS': 'Excess Loss', // 损耗超标
    'DEFECT': 'Incoming Material Defect', // 来料不良
    'OTHER': 'Other', // 其他
  },
  // 补料状态
  mes_supplement_status: {
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'To Issue', // 待发料
    'DONE': 'Issued', // 已发料
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤销
  },
  // 齐套状态
  mes_kit_status: {
    'COMPLETE': 'Fully Kitted', // 齐套
    'PARTIAL': 'Partial Shortage', // 部分缺料
    'SHORT': 'Shortage', // 缺料
    'ISSUED': 'Fully Issued', // 已领齐
  },
  // 科目类别
  fin_account_category: {
    'ASSET': 'Assets', // 资产
    'LIABILITY': 'Liabilities', // 负债
    'EQUITY': 'Owners\' Equity', // 所有者权益
    'COST': 'Costs', // 成本
    'PROFIT': 'Profit & Loss', // 损益
  },
  // 凭证状态
  fin_voucher_status: {
    'DRAFT': 'Unapproved', // 未审核
    'AUDITED': 'Approved', // 已审核
    'POSTED': 'Posted', // 已记账
    'APPROVING': 'In Approval', // 审批中
  },
  // 凭证来源
  fin_voucher_source: {
    'MANUAL': 'Manual', // 手工
    'AR': 'Receivable', // 应收
    'AP': 'Payable', // 应付
    'RECEIPT': 'Receipt', // 收款
    'PAYMENT': 'Payment', // 付款
    'DEPRECIATION': 'Depreciation', // 折旧
    'ASSET': 'Asset Disposal', // 资产处置
    'COST': 'Cost Carry-forward', // 成本结转
    'CLOSE': 'P&L Carry-forward', // 结转损益
    'SALARY': 'Payroll', // 工资
    'PROJECT': 'Project', // 项目
    'EXPENSE': 'Expense', // 费用
    'ADVANCE': 'Advances', // 预收预付
    'BAD_DEBT': 'Bad Debt', // 坏账
    'REVERSAL': 'Reversal', // 红字冲销
    'TAX': 'Taxes', // 税费
    'CLAIM': 'Expense Claim', // 报销
    'LOAN': 'Loan', // 借款
    'FX': 'Exchange Gain/Loss', // 汇兑
    'NOTE': 'Notes', // 票据
    'TRANSFER': 'Account Transfer', // 账户调拨
  },
  // 核销状态
  fin_bill_status: {
    'UNSETTLED': 'Unsettled', // 未核销
    'PARTIAL': 'Partially Settled', // 部分核销
    'SETTLED': 'Settled', // 已核销
  },
  // 应收应付来源
  fin_bill_source: {
    'SALES_OUT': 'Sales Delivery', // 销售出库
    'SALES_RETURN': 'Sales Return', // 销售退货
    'PURCHASE_IN': 'Purchase Receipt', // 采购入库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'MANUAL': 'Opening / Manual', // 期初 / 手工
    'PROJECT': 'Project Billing', // 项目开票
    'SHIPMENT': 'Freight', // 运费
    'TICKET': 'Service Charge', // 服务收费
    'ASSET': 'Fixed Asset', // 固定资产
    'NOTE': 'Dishonored Note', // 票据拒付
    'QUALITY_CLAIM': 'Quality Claim', // 质量索赔
    'REBATE': 'Sales Rebate', // 销售返利
  },
  // 结算方式
  fin_settle_method: {
    'TRANSFER': 'Bank Transfer', // 银行转账
    'CASH': 'Cash', // 现金
    'DRAFT': 'Acceptance Bill', // 承兑汇票
    'ONLINE': 'Alipay / WeChat Pay', // 支付宝 / 微信
    'NOTE': 'Note', // 票据
  },
  // 资产类别
  fin_asset_category: {
    'BUILDING': 'Buildings', // 房屋建筑物
    'MACHINE': 'Machinery', // 机器设备
    'VEHICLE': 'Vehicles', // 运输工具
    'ELECTRONIC': 'Electronic Equipment', // 电子设备
    'FURNITURE': 'Office Furniture', // 办公家具
  },
  // 资产状态
  fin_asset_status: {
    'IN_USE': 'In Use', // 在用
    'IDLE': 'Idle', // 闲置
    'DISPOSED': 'Disposed', // 已处置
  },
  // 职位类别
  hr_position_category: {
    'MANAGEMENT': 'Management', // 管理
    'TECH': 'Technical', // 技术
    'PRODUCTION': 'Production', // 生产
    'SALES': 'Sales', // 销售
    'FUNCTION': 'Functional', // 职能
  },
  // 学历
  hr_education: {
    'DOCTOR': 'Doctorate', // 博士
    'MASTER': 'Master\'s', // 硕士
    'BACHELOR': 'Bachelor\'s', // 本科
    'COLLEGE': 'Associate', // 大专
    'HIGH': 'High School or Below', // 高中及以下
  },
  // 用工类型
  hr_emp_type: {
    'FULL_TIME': 'Full-time', // 全职
    'PART_TIME': 'Part-time', // 兼职
    'INTERN': 'Intern', // 实习
    'OUTSOURCE': 'Agency Worker', // 劳务派遣
  },
  // 员工状态
  hr_emp_status: {
    'PROBATION': 'Probation', // 试用期
    'REGULAR': 'Regular', // 正式
    'LEFT': 'Resigned', // 离职
  },
  // 异动类型
  hr_change_type: {
    'ENTRY': 'Onboarding', // 入职
    'REGULAR': 'Confirmation', // 转正
    'TRANSFER': 'Transfer', // 调岗
    'SALARY': 'Salary Adjustment', // 调薪
    'LEAVE': 'Resignation', // 离职
  },
  // 招聘需求状态
  hr_recruit_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'OPEN': 'Recruiting', // 招聘中
    'CLOSED': 'Closed', // 已关闭
  },
  // 应聘阶段
  hr_candidate_stage: {
    'NEW': 'New', // 新简历
    'INTERVIEW': 'Interviewing', // 面试中
    'OFFER': 'Offer Sent', // 已发 Offer
    'HIRED': 'Hired', // 已入职
    'REJECTED': 'Rejected', // 已淘汰
  },
  // 简历来源
  hr_candidate_source: {
    'WEBSITE': 'Job Board', // 招聘网站
    'REFERRAL': 'Employee Referral', // 内部推荐
    'HUNTER': 'Headhunter', // 猎头
    'CAMPUS': 'Campus Recruiting', // 校园招聘
    'OTHER': 'Other', // 其他
  },
  // 面试结果
  hr_interview_result: {
    'PENDING': 'Pending', // 待面试
    'PASS': 'Passed', // 通过
    'FAIL': 'Failed', // 未通过
  },
  // 培训类别
  hr_training_category: {
    'ONBOARDING': 'Onboarding', // 入职培训
    'SKILL': 'Skills', // 技能培训
    'SAFETY': 'Safety', // 安全培训
    'QUALITY': 'Quality', // 质量培训
    'MANAGEMENT': 'Management', // 管理培训
  },
  // 培训状态
  hr_training_status: {
    'PLANNED': 'Planned', // 计划中
    'ONGOING': 'In Progress', // 进行中
    'FINISHED': 'Finished', // 已结束
    'CANCELED': 'Canceled', // 已取消
  },
  // 考勤状态
  hr_attendance_status: {
    'NORMAL': 'Normal', // 正常
    'LATE': 'Late', // 迟到
    'EARLY': 'Left Early', // 早退
    'LATE_EARLY': 'Late & Left Early', // 迟到早退
    'MISSING': 'Missed Punch', // 缺卡
  },
  // 假期类型
  hr_leave_type: {
    'ANNUAL': 'Annual Leave', // 年假
    'PERSONAL': 'Personal Leave', // 事假
    'SICK': 'Sick Leave', // 病假
    'MARRIAGE': 'Marriage Leave', // 婚假
    'MATERNITY': 'Maternity / Paternity Leave', // 产假 / 陪产假
    'COMPENSATORY': 'Comp Time Off', // 调休
  },
  // 假勤状态
  hr_leave_status: {
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已通过
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤销
  },
  // 工资表状态
  hr_payroll_status: {
    'DRAFT': 'Draft', // 草稿
    'CONFIRMED': 'Confirmed', // 已确认
    'PAID': 'Paid', // 已发放
    'APPROVING': 'In Approval', // 审批中
  },
  // 考核状态
  hr_perf_status: {
    'DRAFT': 'Draft', // 草稿
    'SCORING': 'In Review', // 考核中
    'FINISHED': 'Archived', // 已归档
  },
  // 绩效等级
  hr_perf_grade: {
    'S': 'S - Outstanding', // S 卓越
    'A': 'A - Excellent', // A 优秀
    'B': 'B - Good', // B 良好
    'C': 'C - Qualified', // C 合格
    'D': 'D - Needs Improvement', // D 待改进
  },
  // 考核表状态
  hr_appraisal_status: {
    'SELF': 'Self-assessment Pending', // 待自评
    'REVIEW': 'Scoring Pending', // 待评分
    'DONE': 'Completed', // 已完成
  },
  // 节假日类型
  hr_holiday_type: {
    'HOLIDAY': 'Public Holiday', // 法定节假日
    'WORKDAY': 'Make-up Workday', // 调休上班
  },
  // 假勤种类
  hr_leave_kind: {
    'LEAVE': 'Leave', // 请假
    'OVERTIME': 'Overtime', // 加班
  },
  // 客户行业
  crm_industry: {
    'MANUFACTURING': 'Manufacturing', // 制造业
    'ELECTRONICS': 'Electronics & Appliances', // 电子电器
    'IT': 'Information Technology', // 信息技术
    'RETAIL': 'Retail & Wholesale', // 零售批发
    'ENERGY': 'Energy & Power', // 能源电力
    'LOGISTICS': 'Transportation & Logistics', // 交通物流
    'MEDICAL': 'Healthcare', // 医疗健康
    'GOVERNMENT': 'Government & Public Sector', // 政府与公共事业
    'OTHER': 'Other', // 其他
  },
  // 客户来源
  crm_source: {
    'WEBSITE': 'Website Inquiry', // 官网咨询
    'EXHIBITION': 'Trade Show', // 展会
    'PHONE': 'Telemarketing', // 电话营销
    'REFERRAL': 'Customer Referral', // 客户转介绍
    'ADVERTISING': 'Advertising', // 广告投放
    'OTHER': 'Other', // 其他
  },
  // 客户阶段
  crm_customer_stage: {
    'POTENTIAL': 'Prospect', // 潜在客户
    'INTENT': 'Interested', // 意向客户
    'DEAL': 'Closed Deal', // 成交客户
    'LOST': 'Churned', // 流失客户
  },
  // 线索状态
  crm_lead_status: {
    'NEW': 'New', // 待跟进
    'FOLLOWING': 'Following Up', // 跟进中
    'CONVERTED': 'Converted', // 已转化
    'INVALID': 'Invalid', // 无效
  },
  // 跟进方式
  crm_follow_method: {
    'PHONE': 'Phone', // 电话
    'VISIT': 'Visit', // 拜访
    'WECHAT': 'WeChat', // 微信
    'EMAIL': 'Email', // 邮件
    'MEETING': 'Meeting', // 会议
  },
  // 销售阶段
  crm_opp_stage: {
    'INITIAL': 'Initial Contact', // 初步接洽
    'NEEDS': 'Needs Analysis', // 需求确认
    'PROPOSAL': 'Proposal / Quote', // 方案报价
    'NEGOTIATION': 'Negotiation', // 商务谈判
    'WON': 'Won', // 赢单
    'LOST': 'Lost', // 输单
  },
  // 活动类型
  crm_campaign_type: {
    'EXHIBITION': 'Trade Show', // 展会
    'ONLINE': 'Online Promotion', // 线上推广
    'SEMINAR': 'Seminar', // 研讨会
    'EMAIL': 'Email Marketing', // 邮件营销
    'PROMOTION': 'Promotion', // 促销活动
  },
  // 活动状态
  crm_campaign_status: {
    'PLANNED': 'Planned', // 计划中
    'ACTIVE': 'In Progress', // 进行中
    'FINISHED': 'Finished', // 已结束
    'CANCELED': 'Canceled', // 已取消
  },
  // 工单类型
  crm_ticket_type: {
    'CONSULT': 'Inquiry', // 咨询
    'COMPLAINT': 'Complaint', // 投诉
    'REPAIR': 'Repair', // 报修
    'RETURN': 'Return / Exchange', // 退换货
    'SUGGESTION': 'Suggestion', // 建议
  },
  // 工单优先级
  crm_ticket_priority: {
    'URGENT': 'Urgent', // 紧急
    'HIGH': 'High', // 高
    'MEDIUM': 'Medium', // 中
    'LOW': 'Low', // 低
  },
  // 工单状态
  crm_ticket_status: {
    'OPEN': 'Open', // 待处理
    'PROCESSING': 'In Progress', // 处理中
    'RESOLVED': 'Resolved', // 已解决
    'CLOSED': 'Closed', // 已关闭
  },
  // 受理渠道
  crm_ticket_channel: {
    'PHONE': 'Phone', // 电话
    'EMAIL': 'Email', // 邮件
    'ONLINE': 'Live Chat', // 在线客服
    'ONSITE': 'On-site', // 上门
  },
  // 项目类型
  pm_project_type: {
    'RD': 'R&D', // 研发项目
    'DELIVERY': 'Delivery & Implementation', // 交付实施
    'ENGINEERING': 'Engineering', // 工程项目
    'INTERNAL': 'Internal Improvement', // 内部改进
  },
  // 项目状态
  pm_project_status: {
    'PLANNING': 'Planning', // 规划中
    'ACTIVE': 'In Progress', // 进行中
    'SUSPENDED': 'Suspended', // 已暂停
    'FINISHED': 'Completed', // 已完成
    'CLOSED': 'Closed', // 已关闭
    'APPROVING': 'Pending Initiation Approval', // 立项审批中
  },
  // 项目角色
  pm_member_role: {
    'MANAGER': 'Project Manager', // 项目经理
    'DEVELOPER': 'Developer', // 开发
    'TESTER': 'Tester', // 测试
    'IMPLEMENTER': 'Implementer', // 实施
    'ENGINEER': 'Engineer', // 工程师
    'CONSULTANT': 'Consultant', // 顾问
  },
  // 任务状态
  pm_task_status: {
    'TODO': 'Not Started', // 未开始
    'DOING': 'In Progress', // 进行中
    'DONE': 'Completed', // 已完成
  },
  // 工时状态
  pm_timesheet_status: {
    'SUBMITTED': 'Pending Approval', // 待审批
    'APPROVED': 'Approved', // 已通过
    'REJECTED': 'Rejected', // 已驳回
  },
  // 成本类型
  pm_cost_type: {
    'LABOR': 'Labor', // 人工
    'MATERIAL': 'Material', // 材料
    'TRAVEL': 'Travel', // 差旅
    'OUTSOURCE': 'Outsourcing', // 外包
    'EQUIPMENT': 'Equipment', // 设备
    'OTHER': 'Other', // 其他
  },
  // 收款状态
  pm_billing_status: {
    'PLANNED': 'To Invoice', // 待开票
    'BILLED': 'Invoiced', // 已开票
  },
  // 优先级
  pm_priority: {
    'HIGH': 'High', // 高
    'MEDIUM': 'Medium', // 中
    'LOW': 'Low', // 低
  },
  // 同步状态
  bi_etl_status: {
    'RUNNING': 'Running', // 运行中
    'SUCCESS': 'Success', // 成功
    'FAILED': 'Failed', // 失败
  },
  // 预警级别
  bi_alert_level: {
    'INFO': 'Info', // 提示
    'WARN': 'Warning', // 警告
    'DANGER': 'Critical', // 严重
  },
  // 加班补偿方式
  hr_overtime_comp: {
    'PAY': 'Overtime Pay', // 加班费
    'TO_LEAVE': 'Comp Time Off', // 转调休
  },
  // 消息类别
  sys_message_category: {
    'APPROVAL': 'Approval', // 审批
    'REMIND': 'Reminder', // 提醒
    'ALERT': 'Alert', // 预警
    'SYSTEM': 'System', // 系统
  },
  // 审批状态
  sys_approve_status: {
    'APPROVED': 'Effective', // 已生效
    'APPROVING': 'In Approval', // 审批中
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤回
    'DONE': 'Effective', // 已生效
  },
  // 冻结状态
  wms_hold_status: {
    'PENDING': 'Pending Inspection', // 待检
    'REJECTED': 'Rejected', // 不合格
    'RELEASED': 'Released', // 已放行
    'RETURNED': 'Returned', // 已退货
  },
  // 请购单状态
  scm_requisition_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已批准
    'ORDERED': 'Ordered', // 已下单
    'CLOSED': 'Closed', // 已关闭
  },
  // 询价单状态
  scm_inquiry_status: {
    'QUOTING': 'Quoting', // 询价中
    'FINISHED': 'Awarded', // 已定标
    'CLOSED': 'Closed', // 已关闭
  },
  // 报价单状态
  scm_quotation_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已批准
    'ORDERED': 'Won', // 已成交
    'LOST': 'Expired', // 已失效
  },
  // 发货通知状态
  scm_delivery_status: {
    'PENDING': 'To Pick', // 待拣货
    'PICKED': 'Picked', // 已拣货
    'SHIPPED': 'Shipped', // 已出库
    'CANCELLED': 'Canceled', // 已取消
  },
  // 仓库类型
  wms_warehouse_type: {
    'NORMAL': 'General', // 普通仓
    'RAW': 'Raw Materials', // 原料仓
    'FINISHED': 'Finished Goods', // 成品仓
    'DEFECT': 'Defective Goods', // 不良品仓
    'CONSIGN_OUT': 'Customer Consignment', // 客户寄售仓
    'CONSIGN_IN': 'Supplier Consignment', // 供应商寄售仓
  },
  // 工单类型
  mes_wo_type: {
    'NORMAL': 'Standard', // 普通
    'REWORK': 'Rework', // 返工
  },
  // 发票类型
  fin_invoice_type: {
    'SPECIAL': 'Special VAT Invoice', // 增值税专用发票
    'NORMAL': 'General VAT Invoice', // 普通发票
    'E_SPECIAL': 'Digital Invoice (Special)', // 数电票（专票）
    'E_NORMAL': 'Digital Invoice (General)', // 数电票（普票）
  },
  // 报销状态
  fin_claim_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'To Pay', // 待付款
    'PAID': 'Paid', // 已付款
  },
  // 借款状态
  fin_loan_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'To Pay', // 待付款
    'PAID': 'Outstanding', // 未还清
    'CLEARED': 'Repaid', // 已还清
  },
  // 预算控制
  fin_budget_control: {
    'NONE': 'No Control', // 不控制
    'WARN': 'Warn on Overrun', // 超预算提示
    'BLOCK': 'Block on Overrun', // 超预算禁止
  },
  // 合同类型
  hr_contract_type: {
    'FIXED': 'Fixed-term', // 固定期限
    'OPEN': 'Open-ended', // 无固定期限
    'TASK': 'Task-based', // 以完成一定工作任务为期限
    'INTERN': 'Internship Agreement', // 实习协议
    'LABOR': 'Service Agreement', // 劳务协议
  },
  // 合同状态
  hr_contract_status: {
    'ACTIVE': 'Active', // 生效中
    'EXPIRED': 'Expired', // 已到期
    'RENEWED': 'Renewed', // 已续签
    'TERMINATED': 'Terminated', // 已终止
  },
  // 专项附加扣除
  hr_deduction_type: {
    'CHILD_EDU': 'Children\'s Education', // 子女教育
    'CONTINUING_EDU': 'Continuing Education', // 继续教育
    'HOUSING_LOAN': 'Mortgage Interest', // 住房贷款利息
    'HOUSING_RENT': 'Housing Rent', // 住房租金
    'ELDERLY': 'Elderly Support', // 赡养老人
    'INFANT': 'Infant Care', // 婴幼儿照护
  },
  // 合同状态
  crm_contract_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'ACTIVE': 'In Execution', // 执行中
    'COMPLETED': 'Completed', // 已完成
    'TERMINATED': 'Terminated', // 已终止
  },
  // 变更单状态
  scm_change_status: {
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Effective', // 已生效
    'REJECTED': 'Rejected', // 已驳回
  },
  // 票据类型
  fin_note_type: {
    'BANK': 'Bank Acceptance Bill', // 银行承兑汇票
    'COMMERCIAL': 'Commercial Acceptance Bill', // 商业承兑汇票
  },
  // 票据状态
  fin_note_status: {
    'HELD': 'Held', // 持有
    'ENDORSING': 'Endorsement in Approval', // 背书审批中
    'ENDORSED': 'Endorsed', // 已背书
    'DISCOUNTED': 'Discounted', // 已贴现
    'COLLECTED': 'Collected', // 已托收
    'DISHONORED': 'Dishonored', // 已拒付
    'ISSUING': 'Issuance in Approval', // 开票审批中
    'ISSUED': 'Unpaid', // 未兑付
    'PAID': 'Paid', // 已兑付
    'CANCELLED': 'Voided', // 已作废
  },
  // 票据操作
  fin_note_action: {
    'RECEIVE': 'Receive', // 收票
    'ENDORSE': 'Endorse', // 背书转让
    'DISCOUNT': 'Discount', // 贴现
    'COLLECT': 'Collect', // 托收
    'DISHONOR': 'Dishonor', // 拒付
    'ISSUE': 'Issue', // 开票
    'PAY': 'Pay', // 兑付
  },
  // 不合格来源
  qms_ncr_source: {
    'INSPECTION': 'Failed Inspection', // 检验不合格
    'PRODUCTION': 'Found in Production', // 生产发现
    'CUSTOMER': 'Customer Complaint', // 客户投诉
    'OTHER': 'Other', // 其他
  },
  // 不合格处置
  qms_disposition: {
    'RETURN': 'Return', // 退货
    'REWORK': 'Rework', // 返工
    'SCRAP': 'Scrap', // 报废
    'CONCESSION': 'Accept on Concession', // 让步接收
    'SORT': 'Sort', // 挑选
  },
  // 责任方
  qms_responsibility: {
    'SUPPLIER': 'Supplier', // 供应商
    'PRODUCTION': 'Production', // 生产
    'DESIGN': 'Design', // 设计
    'OTHER': 'Other', // 其他
  },
  // 不合格处理状态
  qms_ncr_status: {
    'OPEN': 'Pending Review', // 待评审
    'APPROVING': 'In Approval', // 审批中
    'DONE': 'Dispositioned', // 已处置
    'CANCELLED': 'Canceled', // 已取消
  },
  // 纠正预防来源
  qms_capa_source: {
    'NCR': 'Nonconformance', // 不合格品
    'COMPLAINT': 'Customer Complaint', // 客户投诉
    'INTERNAL': 'Internal Finding', // 内部发现
    'AUDIT': 'Audit Finding', // 审核发现
  },
  // 纠正预防状态
  qms_capa_status: {
    'OPEN': 'In Progress', // 处理中
    'VERIFYING': 'Pending Verification', // 待验证
    'CLOSED': 'Closed', // 已关闭
  },
  // 索赔状态
  qms_claim_status: {
    'DRAFT': 'Pending Confirmation', // 待确认
    'CONFIRMED': 'Confirmed', // 已确认
    'CANCELLED': 'Canceled', // 已取消
  },
  // 抽样方式
  qms_sampling_mode: {
    'FULL': '100% Inspection', // 全检
    'FIXED': 'Fixed Quantity', // 固定数量
    'AQL': 'AQL Sampling', // AQL 抽样
  },
  // 采购合同类型
  scm_contract_type: {
    'FRAMEWORK': 'Framework Agreement', // 框架协议
    'FIXED': 'Fixed-quantity Contract', // 固定数量合同
  },
  // 采购合同状态
  scm_contract_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'ACTIVE': 'Active', // 生效
    'CLOSED': 'Closed', // 已关闭
  },
  // 招标状态
  scm_tender_status: {
    'DRAFT': 'Draft', // 草稿
    'BIDDING': 'Bidding', // 投标中
    'OPENED': 'Bids Opened', // 已开标
    'AWARDED': 'Awarded', // 已定标
    'CANCELLED': 'Canceled', // 已取消
  },
  // 送货通知状态
  scm_asn_status: {
    'SUBMITTED': 'To Receive', // 待收货
    'RECEIVED': 'Received', // 已收货
    'CANCELLED': 'Canceled', // 已取消
  },
  // 供应商确认
  scm_supplier_confirm: {
    'ACCEPTED': 'Accepted', // 已接受
    'REJECTED': 'Rejected', // 已拒绝
  },
  // 对账单状态
  fin_statement_status: {
    'DRAFT': 'Draft', // 草稿
    'SENT': 'Pending Confirmation', // 待确认
    'CONFIRMED': 'Confirmed', // 已确认
    'DISPUTED': 'Disputed', // 有异议
  },
  // 工程变更原因
  mes_ecn_reason: {
    'DESIGN': 'Design Improvement', // 设计改进
    'COST': 'Cost Reduction', // 降本
    'QUALITY': 'Quality Issue', // 质量问题
    'CUSTOMER': 'Customer Request', // 客户要求
    'SUBSTITUTE': 'Material Substitution', // 物料替代
    'OTHER': 'Other', // 其他
  },
  // 工程变更状态
  mes_ecn_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Pending Effective', // 待生效
    'EFFECTIVE': 'Effective', // 已生效
    'CANCELLED': 'Canceled', // 已取消
  },
  // 技术文档类型
  mes_doc_type: {
    'DRAWING': 'Drawing', // 图纸
    'SPEC': 'Specification', // 规格书
    'SOP': 'Work Instruction (SOP)', // 作业指导书
    'PROCESS': 'Process Document', // 工艺文件
    'INSPECTION': 'Inspection Standard', // 检验标准
    'OTHER': 'Other', // 其他
  },
  // 技术文档状态
  mes_doc_status: {
    'DRAFT': 'Draft', // 草稿
    'RELEASED': 'Released', // 已发布
    'OBSOLETE': 'Obsolete', // 已作废
  },
  // 资产变动类型
  fin_asset_move_type: {
    'ASSIGN': 'Assign', // 领用
    'RETURN': 'Return', // 归还
    'TRANSFER': 'Transfer', // 调拨
  },
  // 资产盘点状态
  fin_asset_check_status: {
    'CHECKING': 'Counting', // 盘点中
    'DONE': 'Completed', // 已完成
  },
  // 资产盘点结果
  fin_asset_check_result: {
    'PENDING': 'Not Counted', // 待盘
    'NORMAL': 'Matched', // 相符
    'MOVED': 'Location Changed', // 位置变化
    'LOSS': 'Shortage', // 盘亏
    'GAIN': 'Surplus', // 盘盈
  },
  // 易耗品领用状态
  fin_consumable_status: {
    'DRAFT': 'Draft', // 草稿
    'CONFIRMED': 'Issued', // 已出库
  },
  // 标准成本版本状态
  fin_std_cost_status: {
    'DRAFT': 'Draft', // 草稿
    'RELEASED': 'Released', // 已发布
  },
  // 停机原因
  mes_stop_reason: {
    'BREAKDOWN': 'Breakdown', // 故障
    'CHANGEOVER': 'Changeover', // 换型调机
    'MATERIAL': 'Waiting for Material', // 待料
    'MAINTAIN': 'Planned Maintenance', // 计划保养
    'NO_ORDER': 'No Production Plan', // 无生产计划
    'OTHER': 'Other', // 其他
  },
  // 模具类型
  mes_mold_type: {
    'MOLD': 'Mold', // 模具
    'FIXTURE': 'Fixture', // 工装夹具
    'GAUGE': 'Gauge', // 检具
  },
  // 模具状态
  mes_mold_status: {
    'AVAILABLE': 'In Stock', // 在库
    'IN_USE': 'In Use', // 使用中
    'REPAIR': 'Under Repair', // 维修中
    'SCRAPPED': 'Scrapped', // 报废
  },
  // 模具履历
  mes_mold_action: {
    'ISSUE': 'Issue', // 领用
    'RETURN': 'Return', // 归还
    'USE': 'Use', // 使用
    'REPAIR': 'Sent for Repair', // 送修
    'REPAIRED': 'Repaired', // 修复
    'SCRAP': 'Scrap', // 报废
  },
  // 促销类型
  scm_promo_type: {
    'DISCOUNT': 'Discount', // 折扣
    'REDUCE': 'Spend & Save', // 满减
    'GIFT': 'Buy & Get Free', // 买赠
  },
  // 返利状态
  scm_rebate_status: {
    'ACTIVE': 'Active', // 执行中
    'SETTLED': 'Settled', // 已结算
  },
  // 返利兑付方式
  scm_rebate_settle: {
    'CREDIT_NOTE': 'Credit Note', // 红字应收
    'NEXT_ORDER': 'Deduct from Next Order', // 下次订单抵扣
  },
  // 销售渠道
  scm_channel: {
    'DIRECT': 'Direct', // 直销
    'DEALER': 'Dealer', // 经销商
  },
  // 库位类型
  wms_location_type: {
    'STORAGE': 'Storage', // 存储位
    'PICK': 'Picking', // 拣货位
    'RECEIVE': 'Receiving Staging', // 收货暂存
    'SHIP': 'Shipping Staging', // 发货暂存
    'DEFECT': 'Defective', // 不良品位
  },
  // 波次状态
  scm_wave_status: {
    'PICKING': 'Picking', // 拣货中
    'DONE': 'Completed', // 已完成
    'CANCELLED': 'Canceled', // 已取消
  },
  // 存货计价方法
  wms_cost_method: {
    'WEIGHTED': 'Monthly Weighted Average', // 月末一次加权平均
    'MOVING': 'Moving Average', // 移动加权平均
    'FIFO': 'FIFO', // 先进先出
  },
  // 组装拆卸种类
  wms_assembly_kind: {
    'ASSEMBLE': 'Assembly', // 组装
    'DISASSEMBLE': 'Disassembly', // 拆卸
  },
  // 提成依据
  fin_commission_basis: {
    'SALES': 'Sales Amount', // 销售额
    'PROFIT': 'Gross Profit', // 毛利
    'RECEIPT': 'Collections', // 回款
  },
  // 价格控制方式
  scm_price_control_mode: {
    'WARN': 'Warn', // 提示
    'BLOCK': 'Block Saving', // 禁止保存
  },
  // 自定义字段类型
  sys_custom_field_type: {
    'TEXT': 'Text', // 文本
    'TEXTAREA': 'Multi-line Text', // 多行文本
    'NUMBER': 'Number', // 数字
    'DATE': 'Date', // 日期
    'SELECT': 'Dropdown', // 下拉选择
    'BOOL': 'Yes/No', // 是否
  },
  // 集成平台
  sys_im_platform: {
    'DINGTALK': 'DingTalk', // 钉钉
    'WECOM': 'WeCom', // 企业微信
    'FEISHU': 'Feishu (Lark)', // 飞书
  },
}
