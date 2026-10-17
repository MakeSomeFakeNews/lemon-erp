// 菜单名称英文翻译：键为 sys_menu.id（与 sql/init.sql 一致），中文直接使用数据库中的名称
export default {
  1: 'System', // 系统管理
  2: 'Monitor', // 系统监控
  3: 'Workflow', // 流程中心
  4: 'Warehouse', // 仓库管理
  5: 'Supply Chain', // 供应链管理
  6: 'Manufacturing', // 生产制造
  7: 'Finance', // 财务管理
  8: 'HR', // 人力资源
  9: 'CRM', // 客户关系
  10: 'Projects', // 项目管理
  11: 'BI', // 商务智能
  12: 'Quality', // 质量管理
  13: 'Supplier Portal', // 供应商门户
  14: 'Dealer Portal', // 经销商门户
  15: 'Mobile', // 移动作业
  100: 'Users', // 用户管理
  101: 'Roles', // 角色管理
  102: 'Menus', // 菜单管理
  103: 'Dictionaries', // 字典管理
  104: 'Departments', // 部门管理
  105: 'Tenants', // 租户管理
  106: 'Tenant Plans', // 租户套餐
  107: 'Numbering Rules', // 编码规则
  108: 'Print Templates', // 打印模板
  109: 'System Parameters', // 参数设置
  110: 'Online Users', // 在线用户
  111: 'Operation Logs', // 操作日志
  112: 'Login Logs', // 登录日志
  113: 'Server Monitor', // 服务监控
  114: 'Cache Monitor', // 缓存监控
  115: 'Scheduled Jobs', // 定时任务
  120: 'Start a Request', // 发起申请
  121: 'My To-dos', // 我的待办
  122: 'My Processed', // 我的已办
  123: 'My Requests', // 我的申请
  124: 'CC to Me', // 抄送我的
  125: 'Process Models', // 流程模型
  126: 'Process Instances', // 流程实例
  130: 'Inbound Orders', // 入库单
  131: 'Outbound Orders', // 出库单
  132: 'Transfer Orders', // 调拨单
  133: 'Stock Counts', // 盘点单
  134: 'Stock Inquiry', // 库存查询
  135: 'Stock Ledger', // 库存流水
  136: 'Batches & Serial Nos.', // 批次与序列号
  137: 'Materials', // 物料管理
  138: 'Warehouses', // 仓库管理
  139: 'Locations', // 库位管理
  140: 'Purchase Orders', // 采购订单
  141: 'Purchase Receipts', // 采购入库
  142: 'Purchase Returns', // 采购退货
  143: 'Sales Orders', // 销售订单
  144: 'Sales Deliveries', // 销售出库
  145: 'Sales Returns', // 销售退货
  146: 'Shipments', // 物流发运
  147: 'Suppliers', // 供应商
  148: 'Supplier Evaluation', // 供应商评估
  149: 'Customers', // 客户
  160: 'Production Plans', // 生产计划
  161: 'MRP Run', // MRP 运算
  162: 'Work Orders', // 生产工单
  163: 'Quality Inspection', // 质量检验
  164: 'Equipment Ledger', // 设备台账
  165: 'Equipment Maintenance', // 设备维护
  166: 'BOM', // 物料清单
  167: 'Work Centers', // 工作中心
  169: 'Kitting Analysis', // 齐套分析
  170: 'Supplementary Issue Requests', // 补料申请
  180: 'Vouchers', // 凭证管理
  181: 'Ledgers', // 账簿查询
  182: 'Financial Statements', // 财务报表
  183: 'Receivables', // 应收管理
  184: 'Payables', // 应付管理
  185: 'Receipts', // 收款单
  186: 'Payments', // 付款单
  187: 'Fixed Assets', // 固定资产
  188: 'Cost Accounting', // 成本核算
  189: 'Budgets', // 预算管理
  190: 'Chart of Accounts', // 会计科目
  191: 'Period Close', // 期末结账
  192: 'Opening Balances', // 期初余额
  193: 'Bills & Notes', // 票据管理
  194: 'Account Transfers', // 账户调拨
  195: 'Cash Plans', // 资金计划
  200: 'Employees', // 员工档案
  201: 'Positions', // 职位管理
  202: 'Recruitment', // 招聘管理
  203: 'Training', // 培训管理
  204: 'Attendance', // 考勤管理
  205: 'Leave & Overtime', // 假勤审批
  206: 'Salary Profiles', // 薪资档案
  207: 'Payroll', // 薪酬核算
  208: 'Performance Reviews', // 绩效考核
  209: 'Employee Self-Service', // 员工自助
  220: 'Customers', // 客户管理
  221: 'Leads', // 线索管理
  222: 'Opportunities', // 销售机会
  223: 'Follow-ups', // 跟进记录
  224: 'Campaigns', // 市场活动
  225: 'Service Tickets', // 服务工单
  226: 'Customer Analysis', // 客户分析
  240: 'Projects', // 项目列表
  241: 'Schedule & Progress', // 计划进度
  242: 'Resource Allocation', // 资源分配
  243: 'Timesheets', // 工时管理
  244: 'Cost Control', // 成本控制
  245: 'Project Accounting', // 项目核算
  260: 'Business Cockpit', // 经营驾驶舱
  261: 'Pivot Analysis', // 多维分析
  262: 'Custom Reports', // 自定义报表
  263: 'Custom Dashboards', // 自定义仪表盘
  264: 'Decision Support', // 决策支持
  265: 'Data Warehouse', // 数据仓库
  266: 'Stock Holds', // 库存冻结
  267: 'Purchase Requisitions', // 请购单
  268: 'RFQ & Comparison', // 询价比价
  269: 'Sales Quotations', // 销售报价
  270: 'Sales Price Lists', // 销售价目表
  271: 'Delivery Notices', // 发货通知
  272: 'Backorders', // 欠交清单
  273: 'Stock Reservations', // 库存预留
  274: 'Replenishment Suggestions', // 补货建议
  275: 'Capacity Scheduling', // 产能排程
  276: 'Invoices', // 发票管理
  277: 'Advances', // 预收预付
  278: 'Bank Reconciliation', // 银行对账
  279: 'Expense Claims', // 费用报销
  280: 'Employee Loans', // 员工借款
  281: 'Business Analysis', // 经营分析
  282: 'Labor Contracts', // 劳动合同
  283: 'Social Insurance Schemes', // 社保方案
  284: 'Special Additional Deductions', // 专项附加扣除
  285: 'Contracts', // 合同管理
  286: 'Currencies & Exchange Rates', // 币种汇率
  287: 'Consignment Settlement', // 寄售结算
  290: 'Inspection Plans', // 检验方案
  291: 'Nonconformance', // 不合格品处理
  292: 'CAPA', // 纠正预防
  293: 'Supplier Claims', // 供应商索赔
  294: 'Quality Analysis', // 质量分析
  295: 'SPC Charts', // SPC 控制图
  296: 'Tendering', // 招标采购
  297: 'Purchase Contracts', // 采购合同
  298: 'Advance Shipping Notices', // 送货通知
  299: 'Supplier Statements', // 供应商对账
  300: 'My Orders', // 我的订单
  301: 'Shipping Notices', // 送货通知
  302: 'Statements', // 对账单
  303: 'Tenders & Bids', // 招标投标
  304: 'Quality Claims', // 质量索赔
  305: 'Engineering Changes', // 工程变更
  306: 'Technical Documents', // 技术文档
  307: 'Asset Counts', // 资产盘点
  308: 'Low-value Consumables', // 低值易耗品
  309: 'Standard Cost', // 标准成本
  310: 'Equipment OEE', // 设备 OEE
  311: 'Molds & Fixtures', // 模具工装
  312: 'Promotions', // 促销活动
  313: 'Sales Rebates', // 销售返利
  314: 'Channel Inventory', // 渠道库存
  315: 'Wave Picking', // 波次拣货
  320: 'Place Orders', // 我要订货
  321: 'Stock & Sales Reports', // 库存销量上报
  330: 'Scan to Receive', // 扫码收货
  331: 'Put-away', // 上架
  332: 'Picking', // 拣货
  333: 'Stock Count', // 盘点
  340: 'Voucher Account Settings', // 凭证科目设置
  341: 'Cashier', // 出纳管理
  342: 'Variant Attributes', // 规格属性
  343: 'Assembly & Disassembly', // 组装拆卸
  344: 'Sales Commissions', // 销售提成
  345: 'Custom Fields', // 自定义字段
  346: 'Business Parameters', // 业务参数
  347: 'Inventory Movement Summary', // 收发存汇总
  348: 'Partner Statements', // 往来对账单
  349: 'Data Screens', // 数据大屏
  350: 'Integrations', // 第三方集成
  351: 'Production Board', // 生产看板
  352: 'Business Dictionaries', // 业务字典
  901: 'Purchasing', // 采购
  902: 'Sales', // 销售
  903: 'Business Partners', // 往来单位
  904: 'Sourcing', // 采购寻源
  911: 'Stock Operations', // 库存作业
  912: 'Stock Inquiry', // 库存查询
  913: 'Stock Control', // 库存控制
  914: 'Master Data', // 基础资料
  921: 'Production Planning', // 生产计划
  922: 'Shop Floor', // 车间执行
  923: 'Engineering & Equipment', // 工艺与设备
  931: 'General Ledger', // 总账
  932: 'Receivables & Payables', // 应收应付
  933: 'Treasury & Expenses', // 资金与费用
  934: 'Assets & Costs', // 资产与成本
  935: 'Budget & Analysis', // 预算与分析
  936: 'Closing & Settings', // 期末与设置
  941: 'Customer Management', // 客户经营
  942: 'Service & Marketing', // 服务与营销
  951: 'Employee Management', // 员工管理
  952: 'Recruitment & Training', // 招聘培训
  953: 'Attendance & Performance', // 考勤绩效
  954: 'Compensation & Benefits', // 薪酬福利
  961: 'My Tasks', // 我的事务
  962: 'Process Management', // 流程管理
  963: 'Inspection', // 检验管理
  964: 'Nonconformance', // 不良处理
  965: 'Quality Analysis', // 质量分析
  1000: 'Add User', // 用户新增
  1001: 'Edit User', // 用户修改
  1002: 'Delete User', // 用户删除
  1003: 'Reset Password', // 重置密码
  1010: 'Add Role', // 角色新增
  1011: 'Edit Role', // 角色修改
  1012: 'Delete Role', // 角色删除
  1013: 'Data Permission', // 数据权限
  1014: 'Assign Users', // 分配用户
  1020: 'Add Menu', // 菜单新增
  1021: 'Edit Menu', // 菜单修改
  1022: 'Delete Menu', // 菜单删除
  1030: 'Add Dictionary', // 字典新增
  1031: 'Edit Dictionary', // 字典修改
  1032: 'Delete Dictionary', // 字典删除
  1040: 'Add Department', // 部门新增
  1041: 'Edit Department', // 部门修改
  1042: 'Delete Department', // 部门删除
  1050: 'Add Tenant', // 租户新增
  1051: 'Edit Tenant', // 租户修改
  1052: 'Delete Tenant', // 租户删除
  1060: 'Add Plan', // 套餐新增
  1061: 'Edit Plan', // 套餐修改
  1062: 'Delete Plan', // 套餐删除
  1070: 'Edit Rule', // 规则修改
  1080: 'Edit Template', // 模板修改
  1090: 'Edit Parameter', // 参数修改
  1100: 'Force Logout', // 强制退出
  1110: 'Delete Log', // 日志删除
  1120: 'Delete Log', // 日志删除
  1140: 'Clear Cache', // 清除缓存
  1150: 'Edit Job', // 任务修改
  1151: 'Run Now', // 立即执行
  1250: 'Add Model', // 模型新增
  1251: 'Edit Model', // 模型修改
  1252: 'Deploy Model', // 模型发布
  1253: 'Delete Model', // 模型删除
  1260: 'Terminate Process', // 终止流程
  1300: 'Add Inbound Order', // 入库单新增
  1301: 'Edit Inbound Order', // 入库单修改
  1302: 'Delete Inbound Order', // 入库单删除
  1303: 'Confirm Inbound', // 入库确认
  1304: 'Reverse Inbound', // 入库冲销
  1310: 'Add Outbound Order', // 出库单新增
  1311: 'Edit Outbound Order', // 出库单修改
  1312: 'Delete Outbound Order', // 出库单删除
  1313: 'Confirm Outbound', // 出库确认
  1314: 'Reverse Outbound', // 出库冲销
  1320: 'Add Transfer Order', // 调拨单新增
  1321: 'Edit Transfer Order', // 调拨单修改
  1322: 'Delete Transfer Order', // 调拨单删除
  1323: 'Confirm Transfer', // 调拨确认
  1324: 'Reverse Transfer', // 调拨冲销
  1325: 'Receive Transfer', // 调拨收货
  1330: 'Add Stock Count', // 盘点单新增
  1331: 'Edit Stock Count', // 盘点单修改
  1332: 'Delete Stock Count', // 盘点单删除
  1333: 'Confirm Stock Count', // 盘点确认
  1370: 'Add Material', // 物料新增
  1371: 'Edit Material', // 物料修改
  1372: 'Delete Material', // 物料删除
  1373: 'Planning Attributes', // 生产计划属性
  1380: 'Add Warehouse', // 仓库新增
  1381: 'Edit Warehouse', // 仓库修改
  1382: 'Delete Warehouse', // 仓库删除
  1390: 'Add Location', // 库位新增
  1391: 'Edit Location', // 库位修改
  1392: 'Delete Location', // 库位删除
  1400: 'Add Purchase Order', // 采购订单新增
  1401: 'Edit Purchase Order', // 采购订单修改
  1402: 'Delete Purchase Order', // 采购订单删除
  1403: 'Approve Purchase Order', // 采购订单审核
  1404: 'Close Purchase Order', // 采购订单关闭
  1405: 'Change Purchase Order', // 采购订单变更
  1410: 'Purchase Receipt', // 采购入库
  1420: 'Purchase Return', // 采购退货
  1430: 'Add Sales Order', // 销售订单新增
  1431: 'Edit Sales Order', // 销售订单修改
  1432: 'Delete Sales Order', // 销售订单删除
  1433: 'Approve Sales Order', // 销售订单审核
  1434: 'Close Sales Order', // 销售订单关闭
  1435: 'Reserve Stock', // 预留库存
  1436: 'Change Sales Order', // 销售订单变更
  1440: 'Sales Delivery', // 销售出库
  1450: 'Sales Return', // 销售退货
  1460: 'Add Shipment', // 发运单新增
  1461: 'Edit Shipment', // 发运单修改
  1462: 'Delete Shipment', // 发运单删除
  1463: 'Ship & Track', // 发货与跟踪
  1470: 'Add Supplier', // 供应商新增
  1471: 'Edit Supplier', // 供应商修改
  1472: 'Delete Supplier', // 供应商删除
  1473: 'Portal Accounts', // 门户账号
  1480: 'Add Evaluation', // 评估新增
  1481: 'Delete Evaluation', // 评估删除
  1490: 'Add Customer', // 客户新增
  1491: 'Edit Customer', // 客户修改
  1492: 'Delete Customer', // 客户删除
  1493: 'Portal Accounts', // 门户账号
  1600: 'Add Plan', // 计划新增
  1601: 'Edit Plan', // 计划修改
  1602: 'Delete Plan', // 计划删除
  1603: 'Confirm Plan', // 计划确认
  1610: 'Run MRP', // MRP 运算
  1611: 'Release MRP', // MRP 下达
  1620: 'Add Work Order', // 工单新增
  1621: 'Edit Work Order', // 工单修改
  1622: 'Delete Work Order', // 工单删除
  1623: 'Release & Close', // 下达与关闭
  1624: 'Material Issue & Return', // 领料退料
  1625: 'Production Report', // 报工
  1626: 'Completion Receipt', // 完工入库
  1627: 'Change Qty & Split', // 改量与拆分
  1630: 'Add Inspection', // 检验新增
  1631: 'Edit Inspection', // 检验修改
  1632: 'Delete Inspection', // 检验删除
  1640: 'Add Equipment', // 设备新增
  1641: 'Edit Equipment', // 设备修改
  1642: 'Delete Equipment', // 设备删除
  1650: 'Add Maintenance Order', // 维护单新增
  1651: 'Delete Maintenance Order', // 维护单删除
  1652: 'Process Maintenance', // 维护处理
  1660: 'Add BOM', // BOM 新增
  1661: 'Edit BOM', // BOM 修改
  1662: 'Delete BOM', // BOM 删除
  1670: 'Add Work Center', // 工作中心新增
  1671: 'Edit Work Center', // 工作中心修改
  1672: 'Delete Work Center', // 工作中心删除
  1690: 'Order Shortages', // 缺料下单
  1691: 'Switch Substitute', // 替代料切换
  1700: 'Request Supplementary Issue', // 补料申请
  1701: 'Issue Supplementary Material', // 补料发料
  1800: 'Add Voucher', // 凭证新增
  1801: 'Edit Voucher', // 凭证修改
  1802: 'Delete Voucher', // 凭证删除
  1803: 'Approve Voucher', // 凭证审核
  1804: 'Post Voucher', // 凭证记账
  1805: 'Reversal', // 红字冲销
  1830: 'Add Receivable', // 应收单新增
  1831: 'Delete Receivable', // 应收单删除
  1832: 'Generate AR Voucher', // 应收生成凭证
  1834: 'Write Off Bad Debt', // 坏账核销
  1840: 'Add Payable', // 应付单新增
  1841: 'Delete Payable', // 应付单删除
  1842: 'Generate AP Voucher', // 应付生成凭证
  1850: 'Add Receipt', // 收款单新增
  1851: 'Delete Receipt', // 收款单删除
  1852: 'Generate Receipt Voucher', // 收款生成凭证
  1860: 'Add Payment', // 付款单新增
  1861: 'Delete Payment', // 付款单删除
  1862: 'Generate Payment Voucher', // 付款生成凭证
  1870: 'Add Asset', // 资产新增
  1871: 'Edit Asset', // 资产修改
  1872: 'Delete Asset', // 资产删除
  1873: 'Run Depreciation', // 计提折旧
  1874: 'Assign & Transfer', // 领用调拨
  1880: 'Run Cost Accounting', // 成本核算
  1890: 'Prepare Budget', // 预算编制
  1900: 'Add Account', // 科目新增
  1901: 'Edit Account', // 科目修改
  1902: 'Delete Account', // 科目删除
  1910: 'Close Period', // 结账
  1912: 'VAT Carry-forward', // 增值税结转
  1920: 'Edit Opening Balances', // 期初修改
  1930: 'Note Operations', // 票据操作
  1940: 'Record Transfer', // 调拨登记
  1950: 'Maintain Plan Items', // 计划项维护
  2000: 'Add Employee', // 员工新增
  2001: 'Edit Employee', // 员工修改
  2002: 'Delete Employee', // 员工删除
  2003: 'Personnel Changes', // 人事异动
  2010: 'Add Position', // 职位新增
  2011: 'Edit Position', // 职位修改
  2012: 'Delete Position', // 职位删除
  2020: 'Add Requisition', // 需求新增
  2021: 'Edit Requisition', // 需求修改
  2022: 'Delete Requisition', // 需求删除
  2023: 'Approve Requisition', // 需求审核
  2024: 'Manage Candidates', // 应聘者管理
  2025: 'Hire & Onboard', // 录用入职
  2030: 'Add Training', // 培训新增
  2031: 'Edit Training', // 培训修改
  2032: 'Delete Training', // 培训删除
  2040: 'Edit Attendance', // 考勤补录
  2041: 'Shifts & Holidays', // 班次与节假日
  2050: 'Approve Leave & Overtime', // 假勤审批
  2060: 'Maintain Salary Profiles', // 薪资档案维护
  2070: 'Generate Payroll', // 生成工资表
  2071: 'Adjust Payroll', // 工资调整
  2072: 'Confirm Payroll', // 工资确认
  2073: 'Pay Salaries', // 工资发放
  2074: 'Delete Payroll', // 工资表删除
  2080: 'Add Review', // 考核新增
  2081: 'Edit Review', // 考核修改
  2082: 'Delete Review', // 考核删除
  2083: 'Score on Behalf', // 代为评分
  2200: 'Add Customer', // 客户新增
  2201: 'Edit Customer', // 客户修改
  2202: 'Assign Customers', // 分配客户
  2203: 'Claim from Public Pool', // 领取公海客户
  2210: 'Add Lead', // 线索新增
  2211: 'Edit Lead', // 线索修改
  2212: 'Delete Lead', // 线索删除
  2213: 'Convert Lead', // 线索转化
  2220: 'Add Opportunity', // 商机新增
  2221: 'Edit Opportunity', // 商机修改
  2222: 'Delete Opportunity', // 商机删除
  2223: 'Create Sales Order', // 生成销售订单
  2230: 'Log Follow-up', // 写跟进
  2231: 'Delete Follow-up', // 跟进删除
  2240: 'Add Campaign', // 活动新增
  2241: 'Edit Campaign', // 活动修改
  2242: 'Delete Campaign', // 活动删除
  2250: 'Accept Ticket', // 工单受理
  2251: 'Handle Ticket', // 工单处理
  2252: 'Delete Ticket', // 工单删除
  2400: 'Add Project', // 项目新增
  2401: 'Edit Project', // 项目修改
  2402: 'Delete Project', // 项目删除
  2410: 'Maintain Tasks', // 任务维护
  2420: 'Maintain Members', // 成员维护
  2430: 'Approve Timesheets', // 工时审批
  2440: 'Record Expenses', // 费用录入
  2441: 'Delete Expense', // 费用删除
  2442: 'Prepare Budget', // 预算编制
  2443: 'Project Material Issue', // 项目领料
  2450: 'Maintain Billing Plans', // 收款计划维护
  2451: 'Invoice to Receivable', // 开票生成应收
  2459: 'Project Month-end Close', // 项目月结
  2620: 'Add Report', // 报表新增
  2621: 'Edit Report', // 报表修改
  2622: 'Delete Report', // 报表删除
  2630: 'Add Dashboard', // 仪表盘新增
  2631: 'Edit Dashboard', // 仪表盘修改
  2632: 'Delete Dashboard', // 仪表盘删除
  2640: 'Maintain Alert Rules', // 预警规则维护
  2650: 'Sync Now', // 立即同步
  2660: 'Release Hold', // 冻结放行
  2670: 'Add Requisition', // 请购单新增
  2671: 'Edit Requisition', // 请购单修改
  2672: 'Delete Requisition', // 请购单删除
  2673: 'Create Purchase Order', // 生成采购订单
  2680: 'Add RFQ', // 询价单新增
  2681: 'Edit RFQ', // 询价单修改
  2682: 'Delete RFQ', // 询价单删除
  2690: 'Add Quotation', // 报价单新增
  2691: 'Edit Quotation', // 报价单修改
  2692: 'Delete Quotation', // 报价单删除
  2693: 'Convert to Sales Order', // 转销售订单
  2700: 'Maintain Price Lists', // 价目表维护
  2710: 'Add Delivery Notice', // 发货通知新增
  2711: 'Pick', // 拣货
  2713: 'Pack', // 装箱
  2730: 'Release Reservation', // 释放预留
  2750: 'Scheduling & Calendar', // 排程与日历
  2760: 'Register Invoice', // 发票登记
  2770: 'Record Advances', // 预收预付登记
  2780: 'Import & Match', // 导入与勾对
  2790: 'Add Expense Claim', // 报销新增
  2791: 'Pay Expense Claim', // 报销付款
  2800: 'Add Loan', // 借款新增
  2801: 'Loan Payment & Repayment', // 借款付款与还款
  2820: 'Maintain Contracts', // 合同维护
  2830: 'Maintain Schemes', // 方案维护
  2840: 'Maintain Deductions', // 扣除维护
  2850: 'Add Contract', // 合同新增
  2851: 'Edit Contract', // 合同修改
  2852: 'Delete Contract', // 合同删除
  2860: 'Rates & FX Revaluation', // 汇率维护与期末调汇
  2870: 'Consignment Settlement', // 寄售结算
  2900: 'Add Plan', // 方案新增
  2901: 'Edit Plan', // 方案修改
  2902: 'Delete Plan', // 方案删除
  2910: 'Register Nonconformance', // 不合格登记
  2911: 'Review & Disposition', // 评审处置
  2912: 'Delete Nonconformance', // 不合格删除
  2920: 'Register CAPA', // 纠正预防登记
  2921: 'Fill in 8D', // 8D 填写
  2922: 'Verify Effectiveness', // 效果验证
  2923: 'Delete CAPA', // 纠正预防删除
  2930: 'Register Claim', // 索赔登记
  2931: 'Confirm Claim', // 索赔确认
  2932: 'Delete Claim', // 索赔删除
  2960: 'Add Tender', // 招标新增
  2961: 'Edit Tender', // 招标修改
  2962: 'Delete Tender', // 招标删除
  2963: 'Open & Award Bids', // 开标定标
  2970: 'Add Contract', // 合同新增
  2971: 'Edit Contract', // 合同修改
  2972: 'Delete Contract', // 合同删除
  2973: 'Submit Contract', // 合同提交
  2974: 'Close Contract', // 合同关闭
  2980: 'Receive ASN', // 送货收货
  2990: 'Generate Statement', // 对账单生成
  2991: 'Send Statement', // 对账单发送
  2992: 'Delete Statement', // 对账单删除
  3000: 'Confirm Order', // 订单确认
  3010: 'Create Shipping Notice', // 发起送货
  3020: 'Confirm Statement', // 对账确认
  3030: 'Submit Bid', // 提交投标
  3050: 'Add Change', // 变更新增
  3051: 'Edit Change', // 变更修改
  3052: 'Delete Change', // 变更删除
  3053: 'Submit Change', // 变更提交
  3060: 'Add Document', // 文档新增
  3061: 'Edit Document', // 文档修改
  3062: 'Delete Document', // 文档删除
  3063: 'Release Document', // 文档发布
  3070: 'Add Count', // 盘点新增
  3071: 'Enter Count Results', // 盘点录入
  3072: 'Delete Count', // 盘点删除
  3080: 'Add Requisition', // 领用新增
  3081: 'Issue Consumables', // 领用出库
  3082: 'Delete Requisition', // 领用删除
  3090: 'Maintain Standard Cost', // 标准成本维护
  3100: 'Record Downtime', // 停机登记
  3110: 'Add Mold', // 模具新增
  3111: 'Edit Mold', // 模具修改
  3112: 'Delete Mold', // 模具删除
  3113: 'Issue & Return', // 领用归还
  3120: 'Maintain Promotions', // 促销维护
  3130: 'Maintain Rebate Agreements', // 返利协议维护
  3131: 'Settle Rebates', // 返利结算
  3150: 'Maintain Waves', // 波次维护
  3151: 'Complete Picking', // 完成拣货
  3200: 'Place Order', // 下单
  3210: 'Submit Report', // 上报
  3400: 'Edit Account Settings', // 科目设置修改
  3420: 'Maintain Variant Attributes', // 规格属性维护
  3430: 'Maintain Assembly Orders', // 组装拆卸维护
  3431: 'Confirm Assembly', // 组装拆卸确认
  3440: 'Maintain Commission Rules', // 提成规则维护
  3441: 'Calculate & Confirm Commissions', // 提成计算与确认
  3450: 'Maintain Custom Fields', // 自定义字段维护
  3460: 'Edit Business Parameters', // 业务参数修改
  3490: 'Manage Display Links', // 投屏链接管理
  3500: 'Edit Integration Settings', // 集成配置修改
  3501: 'Sync & Bind Contacts', // 通讯录同步与绑定
  3520: 'Maintain Business Dictionaries', // 业务字典维护
}
