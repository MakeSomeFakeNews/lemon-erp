-- ====================================================
-- 实时库存成本（issue #27、#28）：按物料的移动加权平均成本，库存查询显示均价与库存金额，
-- 库存流水记录业务单价、成本单价、成本金额与往来单位，经营历史；其他入库、期初入库可填写成本价；盘点单盘盈可填写成本价；
-- 升级前的历史流水标记为近似成本。可重复执行
-- ====================================================

DROP PROCEDURE IF EXISTS lemon_add_column;
DELIMITER //
CREATE PROCEDURE lemon_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN ddl VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t AND COLUMN_NAME = c) THEN
        SET @s = CONCAT('ALTER TABLE ', t, ' ADD COLUMN ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL lemon_add_column('wms_stock_flow', 'biz_price', 'biz_price DECIMAL(18, 6) DEFAULT NULL COMMENT ''业务单价（不含税，本位币）：采购入库、销售出库等来源单据的单价，没有为空'' AFTER qty');
CALL lemon_add_column('wms_stock_flow', 'cost_price', 'cost_price DECIMAL(18, 6) DEFAULT NULL COMMENT ''成本单价（实时移动加权平均），供应商寄售仓的库存不计价为空'' AFTER biz_price');
CALL lemon_add_column('wms_stock_flow', 'cost_amount', 'cost_amount DECIMAL(18, 2) DEFAULT NULL COMMENT ''成本金额，与数量同号'' AFTER cost_price');
CALL lemon_add_column('wms_stock_flow', 'partner_id', 'partner_id BIGINT DEFAULT NULL COMMENT ''往来单位（供应商 / 客户）'' AFTER cost_amount');
CALL lemon_add_column('wms_stock_flow', 'partner_name', 'partner_name VARCHAR(128) DEFAULT NULL COMMENT ''往来单位名称快照'' AFTER partner_id');
CALL lemon_add_column('wms_stock_doc', 'partner_id', 'partner_id BIGINT DEFAULT NULL COMMENT ''往来单位（业务单据生成时由来源单据传入）'' AFTER reversed');
CALL lemon_add_column('wms_stock_doc', 'partner_name', 'partner_name VARCHAR(128) DEFAULT NULL COMMENT ''往来单位名称快照'' AFTER partner_id');
CALL lemon_add_column('wms_stock_doc_item', 'cost_price', 'cost_price DECIMAL(18, 6) DEFAULT NULL COMMENT ''成本单价：其他入库、期初入库填写的入库成本，业务单据传入的成本；为空按当前移动加权平均价'' AFTER qty');
CALL lemon_add_column('wms_stock_doc_item', 'biz_price', 'biz_price DECIMAL(18, 6) DEFAULT NULL COMMENT ''业务单价（不含税，本位币），业务单据生成时传入'' AFTER cost_price');
CALL lemon_add_column('wms_stock_flow', 'cost_estimated', 'cost_estimated TINYINT NOT NULL DEFAULT 0 COMMENT ''1 升级前的历史流水：成本按当时的成本核算单价 / 参考成本价回填，是近似值'' AFTER partner_name');
CALL lemon_add_column('wms_check_item', 'cost_price', 'cost_price DECIMAL(18, 6) DEFAULT NULL COMMENT ''盘盈的成本单价，为空按确认时的当前均价；盘亏按当前均价'' AFTER remark');
DROP PROCEDURE IF EXISTS lemon_add_column;

CREATE TABLE IF NOT EXISTS wms_material_cost (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '计价数量：全部仓库（不含供应商寄售仓）的库存合计',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '库存金额',
    unit_cost   DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '移动加权平均单价；数量为 0 时保留最后的单价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wms_material_cost (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料实时库存成本（移动加权平均，按物料不分仓库），随库存变动在同一事务中更新';

-- ==================== 历史数据回填（只处理还没有回填的记录） ====================

-- 业务单据生成的出入库单：往来单位取采购入库 / 退货的供应商、销售出库 / 退货的客户
UPDATE wms_stock_doc sd
JOIN scm_trade_doc d ON d.wms_doc_id = sd.id AND d.tenant_id = sd.tenant_id
LEFT JOIN scm_supplier s ON s.id = d.partner_id AND d.kind IN ('PURCHASE_IN', 'PURCHASE_RETURN')
LEFT JOIN scm_customer c ON c.id = d.partner_id AND d.kind IN ('SALES_OUT', 'SALES_RETURN')
SET sd.partner_id = d.partner_id, sd.partner_name = COALESCE(s.name, c.name)
WHERE sd.partner_id IS NULL;

-- 流水的往来单位、业务单价（该单据该物料的不含税金额 / 数量）
UPDATE wms_stock_flow f
JOIN wms_stock_doc sd ON sd.id = f.doc_id AND f.biz_type IN ('IN', 'OUT')
SET f.partner_id = sd.partner_id, f.partner_name = sd.partner_name
WHERE f.partner_id IS NULL AND sd.partner_id IS NOT NULL;

UPDATE wms_stock_flow f
JOIN scm_trade_doc d ON d.wms_doc_id = f.doc_id AND d.tenant_id = f.tenant_id AND f.biz_type IN ('IN', 'OUT')
SET f.biz_price = (SELECT ROUND(SUM(i.amount - i.tax_amount) / NULLIF(SUM(i.qty), 0), 6)
                   FROM scm_trade_doc_item i WHERE i.doc_id = d.id AND i.material_id = f.material_id)
WHERE f.biz_price IS NULL;

-- 流水的成本单价（供应商寄售仓不计价）：采购入库 / 退货取业务单价；其他取流水所在月份及之前最近一期的成本核算单价，
-- 没有时取参考成本价。历史成本是近似值，标记 cost_estimated = 1，经营历史中升级前的行显示「近似」
UPDATE wms_stock_flow f
JOIN wms_warehouse w ON w.id = f.warehouse_id
JOIN wms_material m ON m.id = f.material_id
LEFT JOIN wms_stock_doc sd ON sd.id = f.doc_id AND f.biz_type IN ('IN', 'OUT')
SET f.cost_price = CASE
        WHEN sd.biz_type IN ('PURCHASE', 'PURCHASE_RETURN') AND f.biz_price IS NOT NULL THEN f.biz_price
        ELSE COALESCE((SELECT lc.unit_cost FROM fin_material_cost lc
                       WHERE lc.tenant_id = f.tenant_id AND lc.material_id = f.material_id AND lc.unit_cost > 0
                         AND lc.period <= YEAR(f.create_time) * 100 + MONTH(f.create_time)
                       ORDER BY lc.period DESC LIMIT 1), m.ref_cost, 0)
    END,
    f.cost_estimated = 1
WHERE f.cost_price IS NULL AND w.type <> 'CONSIGN_IN';

UPDATE wms_stock_flow SET cost_amount = ROUND(qty * cost_price, 2)
WHERE cost_amount IS NULL AND cost_price IS NOT NULL;

-- 物料的实时成本：数量 = 当前库存（不含供应商寄售仓），单价 = 最近一期成本核算单价，没有时取参考成本价
INSERT INTO wms_material_cost (tenant_id, material_id, qty, amount, unit_cost, create_time, update_time)
SELECT t.tenant_id, t.material_id, t.qty, ROUND(t.qty * t.unit_cost, 2), t.unit_cost, NOW(), NOW()
FROM (
    SELECT s.tenant_id, s.material_id, SUM(s.qty) AS qty,
           COALESCE((SELECT lc.unit_cost FROM fin_material_cost lc
                     WHERE lc.tenant_id = s.tenant_id AND lc.material_id = s.material_id AND lc.unit_cost > 0
                     ORDER BY lc.period DESC LIMIT 1), MAX(m.ref_cost), 0) AS unit_cost
    FROM wms_stock s
    JOIN wms_warehouse w ON w.id = s.warehouse_id
    JOIN wms_material m ON m.id = s.material_id
    WHERE w.type <> 'CONSIGN_IN'
    GROUP BY s.tenant_id, s.material_id
) t
WHERE NOT EXISTS (SELECT 1 FROM wms_material_cost x WHERE x.tenant_id = t.tenant_id AND x.material_id = t.material_id);

-- 已执行过早期版本本脚本（回填时还没有 cost_estimated 列）的库：回填的流水都早于物料实时成本记录的初始化时间
UPDATE wms_stock_flow f
SET f.cost_estimated = 1
WHERE f.cost_estimated = 0 AND f.cost_price IS NOT NULL
  AND f.create_time < (SELECT MIN(c.create_time) FROM wms_material_cost c WHERE c.tenant_id = f.tenant_id);

-- 物料库存金额与流水成本累计对齐：采购入库按各自的业务单价回填，出库和物料成本按核算单价，两者累计会有差额，
-- 导致经营历史的结余金额与库存查询的库存金额不一致。对还没有升级后业务的物料（流水全部是回填的近似成本），
-- 流水累计数量等于当前库存时，库存金额取流水成本累计，单价随之重算。可重复执行（已对齐的不再变化）
UPDATE wms_material_cost c
JOIN (
    SELECT f.tenant_id, f.material_id, SUM(f.qty) AS qty, SUM(f.cost_amount) AS amount
    FROM wms_stock_flow f
    JOIN wms_warehouse w ON w.id = f.warehouse_id
    WHERE w.type <> 'CONSIGN_IN'
    GROUP BY f.tenant_id, f.material_id
    HAVING SUM(CASE WHEN f.cost_estimated = 1 THEN 0 ELSE 1 END) = 0 AND COUNT(f.cost_amount) = COUNT(*)
) t ON t.tenant_id = c.tenant_id AND t.material_id = c.material_id
SET c.amount = t.amount,
    c.unit_cost = CASE WHEN t.qty > 0 THEN ROUND(t.amount / t.qty, 6) ELSE c.unit_cost END,
    c.update_time = NOW()
WHERE c.qty = t.qty AND c.amount <> t.amount AND t.qty > 0 AND t.amount >= 0;

-- ==================== 菜单 ====================

-- 库存查询的「查看成本金额」按钮：加入包含库存查询的套餐，授权有库存查询的管理员、总经理、仓库管理员、会计
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 3652, 134, '查看成本金额', 'F', NULL, NULL, 'wms:stock:cost', NULL, 1, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 3652);

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, 3652
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 134) p
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 3652);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 3652
FROM sys_role r
WHERE r.code IN ('admin', 'gm', 'warehouse', 'accountant')
  AND EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 134)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 3652);

-- ==================== 生产成本：完工入库的工费、工单提前关闭、完工后退料 ====================
-- 完工入库的实时成本 = 材料（工单净领料成本分摊）+ 人工 + 制造费用（报工工时 × 工作中心费率，没有报工工时的工序按标准工时），
-- 或标准成本（按料工费拆分）；关闭工单时未分摊的成本追加到已完工产品，完工后退料冲减已完工产品的成本

DROP PROCEDURE IF EXISTS lemon_add_column;
DELIMITER //
CREATE PROCEDURE lemon_add_column(IN t VARCHAR(64), IN c VARCHAR(64), IN ddl VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t AND COLUMN_NAME = c) THEN
        SET @s = CONCAT('ALTER TABLE ', t, ' ADD COLUMN ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL lemon_add_column('mes_wo_doc', 'cost_basis', 'cost_basis VARCHAR(16) DEFAULT NULL COMMENT ''完工入库的成本口径 STANDARD标准成本 ACTUAL工单实际成本 AVERAGE当前均价（工单没有已计价的领料和工费）'' AFTER wms_doc_no');
CALL lemon_add_column('mes_wo_doc', 'material_cost', 'material_cost DECIMAL(18, 2) DEFAULT NULL COMMENT ''转入产品的材料成本：完工入库为本次分摊的净领料成本；退料为冲减已完工产品的成本（负数）'' AFTER cost_basis');
CALL lemon_add_column('mes_wo_doc', 'labor_cost', 'labor_cost DECIMAL(18, 2) DEFAULT NULL COMMENT ''完工入库的直接人工'' AFTER material_cost');
CALL lemon_add_column('mes_wo_doc', 'overhead_cost', 'overhead_cost DECIMAL(18, 2) DEFAULT NULL COMMENT ''完工入库的制造费用'' AFTER labor_cost');
CALL lemon_add_column('mes_wo_doc', 'stock_cost_adjust', 'stock_cost_adjust DECIMAL(18, 2) DEFAULT NULL COMMENT ''退料冲减已完工产品成本中计入库存的部分（负数），其余对应已出库的产品'' AFTER overhead_cost');
CALL lemon_add_column('mes_work_order', 'close_cost', 'close_cost DECIMAL(18, 2) DEFAULT NULL COMMENT ''关闭时尚未转入产品的成本（净领料成本 + 工费 − 已转入完工产品）'' AFTER close_time');
CALL lemon_add_column('mes_work_order', 'close_stock_cost', 'close_stock_cost DECIMAL(18, 2) DEFAULT NULL COMMENT ''其中追加到已完工产品库存的部分，其余（没有完工、产品已出库）计入当期成本'' AFTER close_cost');
DROP PROCEDURE IF EXISTS lemon_add_column;

-- 升级前的完工入库：材料成本取该入库单产品流水的成本金额（回填的近似成本），工费为 0
UPDATE mes_wo_doc d
JOIN mes_work_order o ON o.id = d.wo_id
SET d.material_cost = COALESCE((SELECT SUM(f.cost_amount) FROM wms_stock_flow f
                                WHERE f.biz_type = 'IN' AND f.doc_id = d.wms_doc_id AND f.material_id = o.material_id), 0),
    d.labor_cost    = 0,
    d.overhead_cost = 0
WHERE d.kind = 'FINISH' AND d.material_cost IS NULL AND d.wms_doc_id IS NOT NULL;
