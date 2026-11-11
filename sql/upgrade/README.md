# 数据库升级脚本

`sql/init.sql` 始终是最新版本的完整建表与初始数据脚本，**全新安装只执行 init.sql**，不需要执行这里的脚本。

已经在使用的数据库升级到新版本时，按文件名（日期）顺序执行**部署之后**新增的脚本。所有脚本都可以重复执行，已经执行过的部分会自动跳过，不确定是否执行过时直接再执行一次即可。

| 脚本 | 说明 |
| --- | --- |
| `20261106_v1.0.0.sql` | 开源首版（2026-10-16）升级到 v1.0.0。这段时间内任一版本的代码部署的数据库都可以直接执行 |
| `20261108_order_discount.sql` | 整单优惠 / 抹零：销售 / 采购订单、销售报价单增加整单优惠金额（`whole_discount`）；订单明细增加已出入库金额、已退款金额、整单优惠分摊额（`done_amount`、`return_amount`、`discount_share`、`done_share`，分批出入库的尾差、订单变更重新分摊用，已出入库金额按历史出入库单回填），订单变更单增加变更前后的整单优惠；订单关闭时的尾差调整（`scm_order.close_adjust`、`scm_order_item.close_adjust` / `close_adjust_net`，应收应付来源字典增加 `ORDER_ADJUST`「订单尾差调整」） |
| `20261108_price_region.sql` | 客户区域改为业务字典 `scm_region`（客户、CRM 客户、线索的地区下拉选择，进销存报表按字典名称显示）：已有的区域文本与字典名称相同的换成键值，其余不同取值按租户写入字典后换成键值 |
| `20261108_settlement.sql` | 应收应付：收付款核销折让 / 抹零（`discount_amount`、`discount`，外币折让的原币 `discount_fc`）、多收转预收预付、外币预收预付（`fin_advance.currency` / `exchange_rate` / `amount_fc` / `used_fc`、`fin_advance_apply.amount_fc`，冲抵的单据一方本位币 `fin_advance_apply.bill_amount`（资产状况按截至日倒推应收应付用，历史数据回填））、冲账单（`fin_offset`，菜单「冲账单」、字典 `fin_offset_item`）、应付坏账按钮 |
| `20261108_stock_cost.sql` | 实时库存成本：物料移动加权平均成本（`wms_material_cost`）、库存流水的业务单价 / 成本单价 / 成本金额 / 往来单位、出入库明细的成本价，按历史单据和成本核算单价回填并标记为近似成本（`cost_estimated`），回填后物料库存金额与流水成本累计对齐（经营历史结余与库存查询金额一致）；盘点明细的盘盈成本价（`wms_check_item.cost_price`）；库存查询「查看成本金额」按钮（`wms:stock:cost`）；生产成本：完工入库的成本口径与料工费（`mes_wo_doc.cost_basis` / `material_cost` / `labor_cost` / `overhead_cost`，历史完工入库回填流水成本为材料）、完工后退料冲减产品成本（`mes_wo_doc.stock_cost_adjust`）、工单关闭转出成本（`mes_work_order.close_cost` / `close_stock_cost`） |
| `20261109_gm_order_close.sql` | 总经理角色增加「采购订单关闭」「销售订单关闭」按钮权限（各租户按角色编码 gm），请购单的可见范围调整不需要改库 |
| `20261109_role_perms.sql` | 内置角色权限补齐（各租户按角色编码，非平台租户以套餐为上限）：质量工程师补齐员工自助、费用报销、借款申请；销售员增加服务工单处理、市场活动维护；新增菜单「项目 → 我的工时」（`pm:timesheet:mine`）并授权普通员工；新增内置角色「销售经理」（`sales_mgr`，本部门及以下，复制销售员权限并加客户分析、价目表和商品价格维护）；缩短总经理备注 |
| `20261109_system_bugs.sql` | 缺陷修复：在途调拨收货日期（`wms_stock_doc.receive_date`，历史单据取收货时间的日期）；工作中心所属车间（`mes_work_center.dept_id`，工单的数据权限部门取第一道工序工作中心的车间）；金额为 0 的应收应付单改为已核销（不生成凭证） |
| `20261109_notice.sql` | 通知公告：新表 `sys_notice`、`sys_notice_target`（发布范围）、`sys_notice_recipient`（接收人与阅读记录）；字典公告状态 `sys_notice_status`、发布范围 `sys_notice_scope`（系统字典）、公告类型 `sys_notice_type`（业务字典，平台和已有租户各一份），站内消息类别增加「公告」（`NOTICE`）；菜单「系统管理 → 通知公告」（`sys:notice:*`），包含系统管理的套餐及其租户管理员加入，人事专员全部权限、普通用户只读（各租户按角色编码，非平台租户以套餐为上限） |
| `20261109_ecn_approver.sql` | 工程变更记录批准人（`mes_ecn.approve_by`），详情的「批准」显示批准人和时间；已批准的历史变更单没有批准人 |
| `20261110_ecn_flow.sql` | 工程变更审批的内置示例流程（`mes_ecn`，未发布）增加条件分支：变更原因为客户要求、质量问题或用料变更 ≥ 5 项时总经理审批（流程变量新增 `itemChanges` 用料变更项数、`opChanges` 工序变更项数）；只更新从未发布、从未修改过的模型，各租户按角色编码对应本租户的岗位角色。项目月结迟审批工时入账、合同已回款口径、分批预留合并不需要改库 |
| `20261110_other_income_rebate.sql` | 现金银行与返利：其他收支（新表 `fin_io_category` 收支类别、`fin_io_doc` / `fin_io_doc_item` 其他收入单与费用支出单；管理费用下增加业务招待费、水电费、租赁费、快递费明细科目；平台和已有租户各复制一份内置收支类别，科目按编码关联）；账户调拨的手续费承担方（`fin_fund_transfer.fee_bearer`，已有数据为转出账户）；返利协议的种类、计算方式和供应商（`scm_rebate.kind` / `calc_mode` / `supplier_id`，已有数据为销售返利、阶梯比例）、返利项目（新表 `scm_rebate_item`），应收应付来源「销售返利」改名为「返利」；字典收支方向、收支单状态、关联单位类型、手续费承担方、返利种类、返利计算方式，凭证来源增加「其他收支」；菜单「财务 → 资金与费用 → 其他收支」（`fin:io:*`）和「供应链 → 采购 → 采购返利」（`scm:purchaseRebate:*`），随套餐加入，内置角色按编码授权（平台以外的租户以套餐为上限） |
| `20261110_trade_discount_freight.sql` | 整单优惠不改变价格体系（issue #43）：出入库单记录本单分摊的整单优惠（`scm_trade_doc.discount_amount`），明细记录优惠前单价和分摊额（`scm_trade_doc_item.gross_price`、`discount_share`），按订单明细的分摊额回填已有单据，并按优惠前单价重算受影响的价格记忆；询价单的供应商整单优惠承诺（新表 `scm_inquiry_supplier`）；采购入库运费（`scm_trade_doc.freight`、`freight_bearer`、`carrier_supplier_id`、`freight_bill_id`，明细 `freight_share`，应收应付来源「采购运费」`PURCHASE_FREIGHT`）；发运单运费承担方（`scm_shipment.freight_bearer`，默认本公司承担，与原来一致；`freight_ar_bill_id` 代垫运费应收单）；系统字典「运费承担」`scm_freight_bearer`。销售毛利扣坏账、运费的口径（issue #38）只是查询，不需要改库 |
| `20261110_mes_issue49.sql` | 生产制造（issue #49）：工单报工报废数量（`mes_work_order.scrap_qty`，按已有报工的各工序报废数回填），完工入库 + 报废达到工单数量时工单完工，在制产出按 工单数量 − 完工 − 报废 计算（已有完工入库且 完工 + 报废已达到数量的在制工单改为已完工，完工时间取最后一次完工入库或报工的时间）；工单计划完工日期索引；菜单「生产制造 → 生产统计」（人员计件统计、工序产量统计、工单完成情况、产品产量汇总、用料差异分析，`mes:stat:*`），包含生产工单的套餐及其租户管理员随套餐加入，内置角色按编码授权（平台以外的租户以套餐为上限）。业务参数「完工入库领料校验」（`mes.finish.kitCheck`，默认提示）不需要改库 |
| `20261110_arap_issue50.sql` | 应收应付的综合问题（issue #50）：收付款中冲抵预收预付（`fin_settlement.advance_offset`、`fin_settlement_item.advance_offset`，新表 `fin_settlement_advance` 登记收付款选用的预收预付，`fin_advance_apply.settlement_id` 关联在收付款中冲抵的记录）；收付款草稿（`fin_settlement.status` 增加 `DRAFT`，字典「审批状态」增加「草稿」，是否启用由业务参数「收付款先存草稿」控制，默认关闭）；按钮「收款单确认」「付款单确认」（`fin:receipt:confirm` / `fin:payment:confirm`），随套餐加入，已有收款单 / 付款单新增权限的角色同时授权。往来汇总、来源单据穿透联查不需要改库 |
| `20261111_fund_transfer_fields.sql` | 账户调拨（issue #66）：经办人、票据号码（`fin_fund_transfer.handler_id`、`ticket_no`，可空）；手续费承担方改称「手续费支付账户」（`fee_bearer` 注释，系统字典 `fin_fee_bearer` 改名，取值名称改为「转出账户支付」「转入账户支付」，只改仍为原名称的）。手续费单独生成凭证分录（出纳日记账中作为支付账户的一笔支出）、收支汇总单列调拨手续费只是程序变化，已有调拨的凭证保持原样 |
| `20261111_purchase_discount_freight.sql` | 采购入库的订单优惠、运费、承运商（issue #68）：引用订单的采购入库可修改本单订单优惠，记录与按订单分摊之差（`scm_trade_doc.discount_adjust`，已有单据为 0）；采购运费的入账方式（`scm_trade_doc.freight_cost`，已有带运费的入库单为计入存货成本 `INVENTORY`）；供应商的物流公司标识和营运路线（`scm_supplier.logistics` / `routes`），已被用作承运商（采购入库承运商、发运单运费结算供应商）的供应商标记为物流公司。业务参数「本公司承担的采购运费计入」（`scm.purchaseFreight.cost`，默认存货成本）、凭证科目「采购运费（计入当期费用时）」使用默认值，不需要改库 |
| `20261111_trade_draft.sql` | 进销存单据草稿与审核（issue #68）：新表 `scm_trade_draft` / `scm_trade_draft_item`（采购入库 / 退货、销售出库 / 退货的草稿，审核后生成出入库单并删除）；出入库单的审核人、审核时间（`scm_trade_doc.post_by`、`post_time`，已有单据取制单人、创建时间）；菜单「采购入库审核」等 4 个审核按钮（`scm:{purchaseIn,purchaseReturn,salesOut,salesReturn}:post`），包含对应页面的套餐加入，有新增按钮的角色和能查看页面的会计角色同时授权。是否启用由业务参数「进销存单据先存草稿」（`scm.tradeDoc.draft`，默认关闭）控制 |
| `20261111_trade_freight.sql` | 销售出库、销售退货、采购退货的运费（issue #72、#73）：出入库单和草稿的付给承运商方式、现付付款账户（`scm_trade_doc` / `scm_trade_draft` 的 `freight_settle`、`freight_account_id`），出入库单生成的往来单位代垫运费单据、费用支出单（`scm_trade_doc.freight_partner_bill_id`、`freight_io_id`），已有单据为空；其他收支单的来源单据（`fin_io_doc.source_type` / `source_id` / `source_no`，索引 `idx_io_doc_source`）；收支类别的内置标识（`fin_io_category.code`，唯一键 `uk_io_category_code`），平台和已有租户各增加内置类别「销售运费」「代垫运费」（科目取该租户凭证科目设置中的销售运费 / 代垫运费，没有设置按编码 6601 / 1221；已有同名支出类别时直接标记为内置）；应收应付单、供应商对账单明细的来源字段加长到 32（`fin_bill.source_type`、`fin_supplier_statement_line.source_type`）；系统字典「运费结算方式」`scm_freight_settle`，应收应付来源增加「销售运费」`SALES_FREIGHT`、「销售退货运费」`SALES_RETURN_FREIGHT`、「采购退货运费」`PURCHASE_RETURN_FREIGHT` |
| `20261111_wms_material_category.sql` | 物料（商品）多级分类：新表 `wms_material_category`（每个租户一份，物料等仍按分类编码关联），物料表增加分类索引；各租户的业务字典 `wms_material_category` 迁为一级分类（编码 = 原键值、名称 = 原标签，排序、状态、内置标记沿用，已有物料和检验方案、促销、提成规则、凭证科目设置中的分类不变；物料上有、字典中已删除的编码补为一级分类，缺少的内置分类从平台补上），然后删除该业务字典（分类改在「物料管理」左侧的分类树中维护）；按钮「物料分类维护」（`wms:material:category`），包含物料管理的套餐加入，有物料修改权限的角色同时授权。升级后清空「字典数据」缓存（`dict:*`） |

之后的变更按 `YYYYMMDD_名称.sql`（变更当天的日期）新增，并在上表中登记。

## 升级步骤

```bash
# 1. 备份数据库
mysqldump -uroot -p lemon_erp > lemon_erp_backup.sql

# 2. 按日期顺序执行新增的升级脚本
mysql -uroot -p lemon_erp < sql/upgrade/20261106_v1.0.0.sql

# Docker 部署时
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" lemon_erp' < ../sql/upgrade/20261106_v1.0.0.sql
```

3. 部署新版本的后端和前端并重启；
4. 在「系统监控 → 缓存监控」中清空「用户权限」「字典数据」「系统参数」缓存（或删除 Redis 中 `login:perms:*`、`dict:*` 开头的键和 `settings:all`），使新的菜单、权限、字典立即生效。

Flowable 的 `ACT_*` 表不需要处理，后端启动时自动升级。

## 编写约定

- 可重复执行：建表用 `CREATE TABLE IF NOT EXISTS`；加列、加索引前查询 `information_schema` 判断是否已存在；插入数据用 `INSERT ... SELECT ... WHERE NOT EXISTS`；数据换算要能判断是否已经换算过
- 不依赖自增 ID：按编码、路由地址、权限标识、角色编码关联
- 多租户：业务字典等租户数据写平台租户（`tenant_id` = 1），并给已有租户各复制一份
- 同时修改 `sql/init.sql`，保证「旧版本 init.sql + 升级脚本」与「新版本 init.sql」得到相同的表结构和初始数据
