-- ====================================================
-- 调拨的同价 / 差价调拨、调拨价格快速计算、调拨费用（issue #104）。可重复执行
-- 1. 调拨单表头：调拨价格（transfer_pricing 同价调拨 / 差价调拨的依据，price_level 价格列，price_factor 系数）、
--    调出成本金额、调拨金额（cost_amount、transfer_amount，确认时生成）、调拨费用（fee、fee_settle 现付 / 月结、
--    fee_account_id 付款账户、fee_carrier_id 承运商，生成的费用支出单 fee_io_id、承运商应付单 fee_bill_id）；
--    明细：调拨价、调拨金额（transfer_price、transfer_amount），调出成本价沿用 cost_price。
--    系统的存货成本按物料全公司计算，调拨价只是内部结算价，不改变存货成本、收发存、成本核算和凭证
-- 2. 已有调拨单按同价调拨回填：明细的调出成本价、调拨价 = 调出仓库流水的成本单价，金额 = 数量 × 单价；表头金额为明细合计；
--    调拨流水没有业务单价的记为成本单价（与新单据一致）
-- 3. 收支类别内置「调拨费用」（TRANSFER_FEE，平台和已有租户各一个，科目取凭证科目设置中的调拨费用，没有时按编码 6601；
--    已有同名支出类别时直接标记为内置）；应收应付来源增加「调拨费用」TRANSFER_FEE
-- ====================================================

-- ---------------------------------------------------- 1. 调拨单字段

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'transfer_pricing');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN transfer_pricing VARCHAR(16) DEFAULT NULL COMMENT ''调拨价格（仅调拨） SAME同价调拨 REF_COST参考成本价 AVG_COST库存均价 MIN_SALE最低售价 PRICE_LEVEL价格列：差价调拨的快速计算依据，调拨价只是内部结算价，不影响存货成本'' AFTER partner_name', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'price_level');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN price_level TINYINT DEFAULT NULL COMMENT ''调拨价格依据为价格列时的价格列：0零售价 1-8批发价'' AFTER transfer_pricing', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'price_factor');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN price_factor DECIMAL(10, 4) DEFAULT NULL COMMENT ''差价调拨的价格系数（调拨价 = 依据价 × 系数），同价调拨为空'' AFTER price_level', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'cost_amount');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN cost_amount DECIMAL(18, 2) DEFAULT NULL COMMENT ''调出成本金额（调拨确认时生成，红字为负数）'' AFTER price_factor', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'transfer_amount');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN transfer_amount DECIMAL(18, 2) DEFAULT NULL COMMENT ''调拨金额（调拨价 × 数量合计，确认时生成）'' AFTER cost_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'fee');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN fee DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''调拨费用（配送费，本公司承担，计入当期费用），0为没有'' AFTER transfer_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'fee_settle');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN fee_settle VARCHAR(16) DEFAULT NULL COMMENT ''调拨费用付给承运商的方式(字典 scm_freight_settle) CASH现付 MONTHLY月结'' AFTER fee', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'fee_account_id');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN fee_account_id BIGINT DEFAULT NULL COMMENT ''现付调拨费用的付款账户（现金 / 银行末级科目）'' AFTER fee_settle', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'fee_carrier_id');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN fee_carrier_id BIGINT DEFAULT NULL COMMENT ''调拨费用的承运商（供应商），月结时必填'' AFTER fee_account_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'fee_io_id');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN fee_io_id BIGINT DEFAULT NULL COMMENT ''现付调拨费用生成的费用支出单'' AFTER fee_carrier_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'fee_bill_id');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN fee_bill_id BIGINT DEFAULT NULL COMMENT ''月结调拨费用生成的承运商应付单'' AFTER fee_io_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'transfer_price');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN transfer_price DECIMAL(18, 6) DEFAULT NULL COMMENT ''调拨价（调入成本价 / 内部结算价，仅调拨）：同价调拨确认时等于调出成本价，差价调拨可填写、不填按调出成本价'' AFTER biz_price', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'transfer_amount');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN transfer_amount DECIMAL(18, 2) DEFAULT NULL COMMENT ''调拨金额 = 数量 × 调拨价（确认时生成）'' AFTER transfer_price', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------- 2. 已有调拨单按同价调拨回填

UPDATE wms_stock_doc SET transfer_pricing = 'SAME' WHERE kind = 'TRANSFER' AND transfer_pricing IS NULL;

UPDATE wms_stock_doc_item i
JOIN wms_stock_doc d ON d.id = i.doc_id
JOIN (SELECT doc_id, material_id, warehouse_id, MAX(cost_price) AS cost_price
      FROM wms_stock_flow
      WHERE biz_type = 'TRANSFER' AND cost_price IS NOT NULL
      GROUP BY doc_id, material_id, warehouse_id) f
  ON f.doc_id = d.id AND f.material_id = i.material_id AND f.warehouse_id = d.warehouse_id
SET i.cost_price = f.cost_price, i.transfer_price = f.cost_price, i.transfer_amount = ROUND(i.qty * f.cost_price, 2)
WHERE d.kind = 'TRANSFER' AND d.status IN ('TRANSIT', 'DONE') AND i.transfer_price IS NULL;

UPDATE wms_stock_doc d
JOIN (SELECT doc_id, SUM(ROUND(qty * cost_price, 2)) AS cost_amount, SUM(transfer_amount) AS transfer_amount
      FROM wms_stock_doc_item WHERE cost_price IS NOT NULL GROUP BY doc_id) x ON x.doc_id = d.id
SET d.cost_amount = x.cost_amount, d.transfer_amount = x.transfer_amount
WHERE d.kind = 'TRANSFER' AND d.status IN ('TRANSIT', 'DONE') AND d.cost_amount IS NULL;

UPDATE wms_stock_flow SET biz_price = cost_price WHERE biz_type = 'TRANSFER' AND biz_price IS NULL AND cost_price IS NOT NULL;

-- ---------------------------------------------------- 3. 内置收支类别「调拨费用」、应收应付来源

ALTER TABLE fin_io_category MODIFY COLUMN code VARCHAR(32) DEFAULT NULL COMMENT '内置类别标识（程序使用，不能删除、不能改方向）：SALES_FREIGHT销售运费 FREIGHT_ADVANCE代垫运费 TRANSFER_FEE调拨费用';

-- 已有同名的支出类别直接标记为内置类别，保留其科目
UPDATE fin_io_category c
SET c.code = 'TRANSFER_FEE'
WHERE c.direction = 'OUT' AND c.code IS NULL AND c.name = '调拨费用'
  AND NOT EXISTS (SELECT 1 FROM (SELECT tenant_id FROM fin_io_category WHERE code = 'TRANSFER_FEE') x WHERE x.tenant_id = c.tenant_id);

INSERT INTO fin_io_category (tenant_id, name, code, direction, account_id, sort, status, remark, create_time, update_time)
SELECT t.id, '调拨费用', 'TRANSFER_FEE', 'OUT',
       COALESCE((SELECT r.account_id FROM fin_account_rule r WHERE r.tenant_id = t.id AND r.role = 'TRANSFER_FEE' AND r.dim_value = ''),
                (SELECT a.id FROM fin_account a
                 WHERE a.tenant_id = t.id AND a.code = '6601'
                   AND NOT EXISTS (SELECT 1 FROM fin_account c WHERE c.tenant_id = t.id AND c.parent_id = a.id))),
       12, 1, '调拨单的配送费现付给承运商（issue #104）', NOW(), NOW()
FROM sys_tenant t
WHERE NOT EXISTS (SELECT 1 FROM fin_io_category x WHERE x.tenant_id = t.id AND x.code = 'TRANSFER_FEE');

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, 'fin_bill_source', '调拨费用', 'TRANSFER_FEE', 'cyan', 19, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = 'fin_bill_source' AND x.dict_value = 'TRANSFER_FEE' AND x.tenant_id = 1);
