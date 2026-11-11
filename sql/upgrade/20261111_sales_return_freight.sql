-- ====================================================
-- 销售出库、销售退货、采购退货的运费（issue #72、#73）。可重复执行
-- 1. 出入库单、草稿的运费字段用于全部四种单据：付给承运商的方式（scm_trade_doc / scm_trade_draft 的 freight_settle：CASH 现付、MONTHLY 月结）、
--    现付的付款账户（freight_account_id）；出入库单另记录往来单位的代垫运费单据（freight_partner_bill_id）和现付生成的费用支出单（freight_io_id）。
--    已有单据只有采购入库有运费，新字段为空
-- 2. 其他收支单记录来源单据（fin_io_doc.source_type / source_id / source_no），现付的运费由出入库单生成费用支出单，可互相联查；已有单据为手工录入
-- 3. 收支类别的内置标识（fin_io_category.code），平台和已有租户各增加内置类别「销售运费」「代垫运费」（科目取该租户凭证科目设置中的销售运费 /
--    代垫运费，没有设置时按编码 6601 / 1221）；已有同名的支出类别时直接标记为内置类别，不再新增
-- 4. 系统字典「运费结算方式」scm_freight_settle；应收应付来源增加「销售运费」「销售退货运费」「采购退货运费」，
--    应收应付单、供应商对账单明细的来源字段加长到 32（fin_bill.source_type、fin_supplier_statement_line.source_type）
-- ====================================================

-- ---------------------------------------------------- 1. 出入库单、草稿的运费字段

ALTER TABLE scm_trade_doc MODIFY COLUMN freight DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费，0为没有';
ALTER TABLE scm_trade_doc MODIFY COLUMN freight_bearer VARCHAR(16) DEFAULT NULL COMMENT '运费承担(字典 scm_freight_bearer)：采购入库 SUPPLIER供应商代垫 COMPANY本公司承担；销售出库 COMPANY本公司承担 CUSTOMER代垫运费（客户承担）；销售退货 CUSTOMER客户代垫；采购退货 SUPPLIER代垫运费（供应商承担）';
ALTER TABLE scm_trade_doc MODIFY COLUMN carrier_supplier_id BIGINT DEFAULT NULL COMMENT '运费的承运商（供应商）：采购入库本公司承担、月结时必填';
ALTER TABLE scm_trade_doc MODIFY COLUMN freight_bill_id BIGINT DEFAULT NULL COMMENT '运费应付单（采购入库运费应付、月结的承运商应付）';

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_settle');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_settle VARCHAR(16) DEFAULT NULL COMMENT ''付给承运商的方式(字典 scm_freight_settle，销售出库、采购退货) CASH现付 MONTHLY月结'' AFTER freight_bill_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_account_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_account_id BIGINT DEFAULT NULL COMMENT ''现付运费的付款账户（现金 / 银行末级科目）'' AFTER freight_settle', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_partner_bill_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_partner_bill_id BIGINT DEFAULT NULL COMMENT ''往来单位的代垫运费单据：销售出库代垫运费应收、销售退货红字应收、采购退货红字应付'' AFTER freight_account_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_io_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_io_id BIGINT DEFAULT NULL COMMENT ''现付运费生成的费用支出单'' AFTER freight_partner_bill_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE scm_trade_draft MODIFY COLUMN freight DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '运费';
ALTER TABLE scm_trade_draft MODIFY COLUMN freight_bearer VARCHAR(16) DEFAULT NULL COMMENT '运费承担(字典 scm_freight_bearer)，含义同出入库单';
ALTER TABLE scm_trade_draft MODIFY COLUMN carrier_supplier_id BIGINT DEFAULT NULL COMMENT '运费的承运商（供应商）';

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_draft' AND COLUMN_NAME = 'freight_settle');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_draft ADD COLUMN freight_settle VARCHAR(16) DEFAULT NULL COMMENT ''付给承运商的方式(字典 scm_freight_settle) CASH现付 MONTHLY月结'' AFTER carrier_supplier_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_draft' AND COLUMN_NAME = 'freight_account_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_draft ADD COLUMN freight_account_id BIGINT DEFAULT NULL COMMENT ''现付运费的付款账户'' AFTER freight_settle', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------- 2. 其他收支单的来源单据

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_io_doc' AND COLUMN_NAME = 'source_type');
SET @s = IF(@c = 0, 'ALTER TABLE fin_io_doc ADD COLUMN source_type VARCHAR(32) DEFAULT NULL COMMENT ''来源单据类型(字典 fin_bill_source)：由业务单据生成时填写（如销售出库、采购退货现付的运费），手工录入为空'' AFTER remark', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_io_doc' AND COLUMN_NAME = 'source_id');
SET @s = IF(@c = 0, 'ALTER TABLE fin_io_doc ADD COLUMN source_id BIGINT DEFAULT NULL COMMENT ''来源单据ID'' AFTER source_type', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_io_doc' AND COLUMN_NAME = 'source_no');
SET @s = IF(@c = 0, 'ALTER TABLE fin_io_doc ADD COLUMN source_no VARCHAR(32) DEFAULT NULL COMMENT ''来源单据号'' AFTER source_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_io_doc' AND INDEX_NAME = 'idx_io_doc_source');
SET @s = IF(@c = 0, 'ALTER TABLE fin_io_doc ADD KEY idx_io_doc_source (source_id)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------- 3. 收支类别的内置标识、内置类别「销售运费」「代垫运费」

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_io_category' AND COLUMN_NAME = 'code');
SET @s = IF(@c = 0, 'ALTER TABLE fin_io_category ADD COLUMN code VARCHAR(32) DEFAULT NULL COMMENT ''内置类别标识（程序使用，不能删除、不能改方向）：SALES_FREIGHT销售运费 FREIGHT_ADVANCE代垫运费'' AFTER name', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_io_category' AND INDEX_NAME = 'uk_io_category_code');
SET @s = IF(@c = 0, 'ALTER TABLE fin_io_category ADD UNIQUE KEY uk_io_category_code (code, tenant_id)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已有同名的支出类别（自行建的「销售运费」「代垫运费」）直接标记为内置类别，保留其科目
UPDATE fin_io_category c
JOIN (SELECT '销售运费' AS name, 'SALES_FREIGHT' AS code UNION ALL SELECT '代垫运费', 'FREIGHT_ADVANCE') g ON g.name = c.name
SET c.code = g.code
WHERE c.direction = 'OUT' AND c.code IS NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT tenant_id, code FROM fin_io_category WHERE code IS NOT NULL) x WHERE x.tenant_id = c.tenant_id AND x.code = g.code);

-- 平台和已有租户：科目取凭证科目设置（用途与类别标识同名），没有设置时按编码取末级科目
INSERT INTO fin_io_category (tenant_id, name, code, direction, account_id, sort, status, remark, create_time, update_time)
SELECT t.id, g.name, g.code, 'OUT',
       COALESCE((SELECT r.account_id FROM fin_account_rule r WHERE r.tenant_id = t.id AND r.role = g.code AND r.dim_value = ''),
                (SELECT a.id FROM fin_account a
                 WHERE a.tenant_id = t.id AND a.code = g.acct
                   AND NOT EXISTS (SELECT 1 FROM fin_account c WHERE c.tenant_id = t.id AND c.parent_id = a.id))),
       g.sort, 1, g.remark, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT '销售运费' AS name, 'SALES_FREIGHT' AS code, '6601' AS acct, 10 AS sort, '销售出库本公司承担、现付的运费' AS remark
    UNION ALL SELECT '代垫运费', 'FREIGHT_ADVANCE', '1221', 11, '销售出库、采购退货替往来单位垫付、现付的运费'
) g
WHERE NOT EXISTS (SELECT 1 FROM fin_io_category x WHERE x.tenant_id = t.id AND x.code = g.code)
  AND NOT EXISTS (SELECT 1 FROM fin_io_category x WHERE x.tenant_id = t.id AND x.direction = 'OUT' AND x.name = g.name);

-- ---------------------------------------------------- 4. 应收应付来源字段加长、字典

ALTER TABLE fin_bill MODIFY COLUMN source_type VARCHAR(32) NOT NULL COMMENT '来源(字典 fin_bill_source) SALES_OUT销售出库 SALES_RETURN销售退货 PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 MANUAL期初 / 手工 等';
ALTER TABLE fin_supplier_statement_line MODIFY COLUMN source_type VARCHAR(32) NOT NULL COMMENT '来源(字典 fin_bill_source)';

-- ---------------------------------------------------- 字典：运费结算方式（系统字典）、应收应付来源（ID 已被占用时按自增 ID 插入）

UPDATE sys_dict_type SET remark = '发运单、出入库单的运费由谁承担 / 付给谁'
WHERE type = 'scm_freight_bearer' AND remark = '发运单、采购入库的运费由谁承担 / 付给谁';

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 207, '运费结算方式', 'scm_freight_settle', 'SYSTEM', 1, '销售出库、采购退货的运费付给承运商的方式：现付（生成费用支出单）、月结（生成承运商应付单）', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_freight_settle' OR x.id = 207);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '运费结算方式', 'scm_freight_settle', 'SYSTEM', 1, '销售出库、采购退货的运费付给承运商的方式：现付（生成费用支出单）、月结（生成承运商应付单）', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_freight_settle');

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'scm_freight_settle' AS dict_type, '现付' AS dict_label, 'CASH' AS dict_value, 'green' AS tag_color, 1 AS sort
    UNION ALL SELECT 'scm_freight_settle', '月结', 'MONTHLY', 'blue', 2
    UNION ALL SELECT 'fin_bill_source', '销售运费', 'SALES_FREIGHT', 'cyan', 16
    UNION ALL SELECT 'fin_bill_source', '销售退货运费', 'SALES_RETURN_FREIGHT', 'cyan', 17
    UNION ALL SELECT 'fin_bill_source', '采购退货运费', 'PURCHASE_RETURN_FREIGHT', 'cyan', 18
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value AND x.tenant_id = 1);
