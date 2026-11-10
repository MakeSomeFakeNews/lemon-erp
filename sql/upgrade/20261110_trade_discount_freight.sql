-- ====================================================
-- 整单优惠不改变价格体系（issue #43）、销售毛利扣坏账与运费承担（issue #38）。可重复执行
-- 1. 出入库单记录本单分摊的整单优惠，明细记录优惠前单价和分摊额：明细显示优惠前的单价、金额，优惠只在合计处体现；
--    价格记忆、价格参考改为记录优惠前单价。已有单据按订单明细的分摊额回填，并重算受影响的价格记忆
-- 2. 询价单的供应商报价可填写整单优惠承诺（新表 scm_inquiry_supplier），定标生成采购订单时带过去
-- 3. 采购入库可填写运费（供应商代垫 / 本公司承担），按行分摊计入存货成本，生成运费应付单（来源「采购运费」）
-- 4. 发运单的运费承担方：本公司承担（默认，与原来一致，计入销售费用）/ 客户承担（代垫，生成客户应收单）
-- 销售毛利的坏账核销、运费口径只是查询，不需要改库
-- ====================================================

-- ---------------------------------------------------- 1. 出入库单的整单优惠分摊、采购运费

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'discount_amount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''本单分摊的整单优惠（本位币，含税）：优惠前合计 = 价税合计 + 本项'' AFTER replenish', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''采购运费（仅采购入库，计入存货成本）'' AFTER discount_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_bearer');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_bearer VARCHAR(16) DEFAULT NULL COMMENT ''采购运费付给(字典 scm_freight_bearer) SUPPLIER供应商代垫 COMPANY本公司承担（付承运商）'' AFTER freight', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'carrier_supplier_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN carrier_supplier_id BIGINT DEFAULT NULL COMMENT ''采购运费的承运商（供应商），本公司承担时必填'' AFTER freight_bearer', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_bill_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_bill_id BIGINT DEFAULT NULL COMMENT ''采购运费应付单'' AFTER carrier_supplier_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE scm_trade_doc_item MODIFY COLUMN price DECIMAL(18, 4) NOT NULL COMMENT '含税单价（取订单，已扣除整单优惠的分摊）';

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc_item' AND COLUMN_NAME = 'gross_price');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc_item ADD COLUMN gross_price DECIMAL(18, 4) DEFAULT NULL COMMENT ''优惠前含税单价（整单优惠分摊前，本位币），为空同单价'' AFTER tax_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc_item' AND COLUMN_NAME = 'discount_share');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc_item ADD COLUMN discount_share DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''本行分摊的整单优惠（本位币，含税）'' AFTER gross_price', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc_item' AND COLUMN_NAME = 'freight_share');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc_item ADD COLUMN freight_share DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''本行分摊的采购运费（本位币，计入存货成本）'' AFTER discount_share', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填：订单明细分摊了整单优惠的出入库明细，优惠前单价 = (订单行金额 + 分摊额) / 数量（按单据汇率折算本位币），
-- 分摊额 = 优惠前单价 × 数量 − 行金额（不小于 0）；只处理还没有回填的行（gross_price 为空）
UPDATE scm_trade_doc_item i
JOIN scm_trade_doc d ON d.id = i.doc_id
JOIN scm_order_item oi ON oi.id = i.order_item_id
SET i.gross_price = ROUND((oi.amount + oi.discount_share) / oi.qty * d.exchange_rate, 4)
WHERE i.gross_price IS NULL AND oi.discount_share <> 0 AND oi.qty <> 0;

UPDATE scm_trade_doc_item
SET discount_share = GREATEST(ROUND(qty * gross_price, 2) - amount, 0)
WHERE gross_price IS NOT NULL AND discount_share = 0 AND ROUND(qty * gross_price, 2) > amount;

UPDATE scm_trade_doc d
JOIN (SELECT doc_id, SUM(discount_share) AS share FROM scm_trade_doc_item WHERE discount_share <> 0 GROUP BY doc_id) s ON s.doc_id = d.id
SET d.discount_amount = s.share
WHERE d.discount_amount = 0;

-- 价格记忆按优惠前单价重算：只处理含有整单优惠分摊的往来单位 × 物料（先删除再按全部历史单据重新汇总，口径同 20261106_v1.0.0.sql）
DELETE FROM scm_price_memory
WHERE EXISTS (SELECT 1 FROM scm_trade_doc_item i JOIN scm_trade_doc d ON d.id = i.doc_id
              WHERE i.gross_price IS NOT NULL AND d.kind IN ('SALES_OUT', 'PURCHASE_IN') AND d.tenant_id = scm_price_memory.tenant_id
                AND d.partner_id = scm_price_memory.partner_id AND i.material_id = scm_price_memory.material_id
                AND scm_price_memory.kind = CASE d.kind WHEN 'SALES_OUT' THEN 'SALES' ELSE 'PURCHASE' END);

INSERT INTO scm_price_memory (tenant_id, kind, partner_id, material_id, last_date, last_price, min_date, min_price, max_date, max_price,
                              create_time, update_time)
SELECT g.tenant_id, g.kind, g.partner_id, g.material_id, g.last_date,
       (SELECT COALESCE(i2.gross_price, i2.price) FROM scm_trade_doc_item i2 JOIN scm_trade_doc d2 ON d2.id = i2.doc_id
        WHERE d2.tenant_id = g.tenant_id AND d2.kind = g.doc_kind AND d2.partner_id = g.partner_id AND i2.material_id = g.material_id
          AND COALESCE(i2.gross_price, i2.price) > 0 AND d2.biz_date = g.last_date
        ORDER BY d2.id DESC, i2.id DESC LIMIT 1),
       (SELECT MIN(d2.biz_date) FROM scm_trade_doc_item i2 JOIN scm_trade_doc d2 ON d2.id = i2.doc_id
        WHERE d2.tenant_id = g.tenant_id AND d2.kind = g.doc_kind AND d2.partner_id = g.partner_id AND i2.material_id = g.material_id
          AND COALESCE(i2.gross_price, i2.price) = g.min_price),
       g.min_price,
       (SELECT MIN(d2.biz_date) FROM scm_trade_doc_item i2 JOIN scm_trade_doc d2 ON d2.id = i2.doc_id
        WHERE d2.tenant_id = g.tenant_id AND d2.kind = g.doc_kind AND d2.partner_id = g.partner_id AND i2.material_id = g.material_id
          AND COALESCE(i2.gross_price, i2.price) = g.max_price),
       g.max_price, NOW(), NOW()
FROM (
    SELECT d.tenant_id, d.kind AS doc_kind, CASE d.kind WHEN 'SALES_OUT' THEN 'SALES' ELSE 'PURCHASE' END AS kind, d.partner_id,
           i.material_id, MAX(d.biz_date) AS last_date, MIN(COALESCE(i.gross_price, i.price)) AS min_price,
           MAX(COALESCE(i.gross_price, i.price)) AS max_price
    FROM scm_trade_doc_item i
    JOIN scm_trade_doc d ON d.id = i.doc_id
    WHERE d.kind IN ('SALES_OUT', 'PURCHASE_IN') AND COALESCE(i.gross_price, i.price) > 0
    GROUP BY d.tenant_id, d.kind, d.partner_id, i.material_id
) g
WHERE NOT EXISTS (SELECT 1 FROM scm_price_memory x
                  WHERE x.tenant_id = g.tenant_id AND x.kind = g.kind AND x.partner_id = g.partner_id AND x.material_id = g.material_id);

-- ---------------------------------------------------- 2. 询价单的供应商整单优惠承诺

CREATE TABLE IF NOT EXISTS scm_inquiry_supplier (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inquiry_id     BIGINT         NOT NULL COMMENT '询价单',
    supplier_id    BIGINT         NOT NULL COMMENT '供应商',
    whole_discount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '整单优惠承诺（含税，按该供应商全部报价行的合计）',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inquiry_supplier (inquiry_id, supplier_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '询价单的供应商报价汇总（整单优惠承诺）';

-- ---------------------------------------------------- 4. 发运单的运费承担方

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_shipment' AND COLUMN_NAME = 'freight_bearer');
SET @s = IF(@c = 0, 'ALTER TABLE scm_shipment ADD COLUMN freight_bearer VARCHAR(16) NOT NULL DEFAULT ''COMPANY'' COMMENT ''运费承担(字典 scm_freight_bearer) COMPANY本公司承担（销售费用） CUSTOMER客户承担（代垫，计入应收）'' AFTER freight', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_shipment' AND COLUMN_NAME = 'freight_ar_bill_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_shipment ADD COLUMN freight_ar_bill_id BIGINT DEFAULT NULL COMMENT ''代垫运费应收单（客户承担时）'' AFTER freight_bill_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------- 字典：运费承担（系统字典）、应收应付来源「采购运费」（ID 已被占用时按自增 ID 插入）

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 196, '运费承担', 'scm_freight_bearer', 'SYSTEM', 1, '发运单、采购入库的运费由谁承担 / 付给谁', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_freight_bearer' OR x.id = 196);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '运费承担', 'scm_freight_bearer', 'SYSTEM', 1, '发运单、采购入库的运费由谁承担 / 付给谁', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_freight_bearer');

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'scm_freight_bearer' AS dict_type, '本公司承担' AS dict_label, 'COMPANY' AS dict_value, 'default' AS tag_color, 1 AS sort
    UNION ALL SELECT 'scm_freight_bearer', '客户承担（代垫）', 'CUSTOMER', 'blue', 2
    UNION ALL SELECT 'scm_freight_bearer', '供应商代垫', 'SUPPLIER', 'orange', 3
    UNION ALL SELECT 'fin_bill_source', '采购运费', 'PURCHASE_FREIGHT', 'cyan', 15
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value AND x.tenant_id = 1);
