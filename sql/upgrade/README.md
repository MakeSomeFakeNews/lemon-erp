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
