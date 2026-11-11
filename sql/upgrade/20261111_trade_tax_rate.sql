-- ====================================================
-- 税率（issue #82）。可重复执行；修改了 scm_trade_draft，文件名排在 20261111_trade_draft.sql（建表）之后
-- 1. 字典数据的扩展值（sys_dict_data.ext_value）：业务模块为个别字典声明扩展值（数字）时使用，其他字典为空
-- 2. 业务字典「发票类型」scm_invoice_kind（开票类型，每个租户一份）：扩展值为税率（%），平台和已有租户各一份，租户可在「业务字典」中自行增加
-- 3. 采购 / 销售订单、销售报价单、出入库草稿的发票类型（scm_order / scm_quotation / scm_trade_draft 的 invoice_kind），已有单据为空。
--    出入库单显示所属订单的发票类型，不另存
-- 业务参数「默认税率」（scm.defaultTaxRate，默认 13%，与原来写死的税率一致）使用默认值，不需要改库
-- ====================================================

-- ---------------------------------------------------- 1. 字典数据的扩展值

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'ext_value');
SET @s = IF(@c = 0, 'ALTER TABLE sys_dict_data ADD COLUMN ext_value VARCHAR(32) DEFAULT NULL COMMENT ''扩展值：只用于业务模块声明了扩展值的字典（如开票类型 scm_invoice_kind 的税率 %），其他字典为空'' AFTER remark', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------- 2. 业务字典「发票类型」

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 208, '发票类型', 'scm_invoice_kind', 'BUSINESS', 1, '进销存单据表头的发票类型（开票类型），扩展值为税率（%）：选择后明细税率统一调整为该税率', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_invoice_kind' OR x.id = 208);
-- id 208 已被占用时按自增 ID 插入
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '发票类型', 'scm_invoice_kind', 'BUSINESS', 1, '进销存单据表头的发票类型（开票类型），扩展值为税率（%）：选择后明细税率统一调整为该税率', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_invoice_kind');

-- 平台和已有租户各一份（已有的取值不覆盖）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, ext_value, create_time, update_time)
SELECT t.id, 'scm_invoice_kind', d.dict_label, d.dict_value, d.tag_color, d.sort, 1, d.ext_value, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT '增值税专票 13%' AS dict_label, 'SPECIAL_13' AS dict_value, 'red' AS tag_color, 1 AS sort, '13' AS ext_value
    UNION ALL SELECT '增值税专票 9%', 'SPECIAL_9', 'volcano', 2, '9'
    UNION ALL SELECT '增值税专票 6%', 'SPECIAL_6', 'orange', 3, '6'
    UNION ALL SELECT '增值税普票 13%', 'NORMAL_13', 'blue', 4, '13'
    UNION ALL SELECT '增值税普票 3%', 'NORMAL_3', 'geekblue', 5, '3'
    UNION ALL SELECT '增值税普票 1%', 'NORMAL_1', 'cyan', 6, '1'
    UNION ALL SELECT '不开票', 'NONE', 'default', 7, '0'
) d ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = t.id AND x.dict_type = 'scm_invoice_kind' AND x.dict_value = d.dict_value);

-- ---------------------------------------------------- 3. 单据的发票类型

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order' AND COLUMN_NAME = 'invoice_kind');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order ADD COLUMN invoice_kind VARCHAR(16) DEFAULT NULL COMMENT ''发票类型(业务字典 scm_invoice_kind)：明细未填写税率时取它的税率；出入库单显示所属订单的发票类型'' AFTER whole_discount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_quotation' AND COLUMN_NAME = 'invoice_kind');
SET @s = IF(@c = 0, 'ALTER TABLE scm_quotation ADD COLUMN invoice_kind VARCHAR(16) DEFAULT NULL COMMENT ''发票类型(业务字典 scm_invoice_kind)：明细未填写税率时取它的税率，转销售订单时带到订单'' AFTER whole_discount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_draft' AND COLUMN_NAME = 'invoice_kind');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_draft ADD COLUMN invoice_kind VARCHAR(16) DEFAULT NULL COMMENT ''直接开单的发票类型(业务字典 scm_invoice_kind)，审核时记在隐式订单上；引用订单时为空（取订单的）'' AFTER whole_discount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
