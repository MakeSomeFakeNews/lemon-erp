-- 柠檬ERP 数据库升级：开源首版（2026-10-16）→ v1.0.0（2026-11-06）
--
-- 适用于用这段时间内的 sql/init.sql 初始化的数据库；全新安装直接执行 sql/init.sql，不需要执行本脚本。
-- 包含：单据打印（JasperReports、套打）、凭证辅助核算、自动凭证科目规则、成本计算方法、期初启用、出纳、数电票、
-- 多规格物料、组装拆卸、销售提成、商品价格体系、自定义字段、业务规则、业务报表、数据大屏、钉钉 / 企业微信 / 飞书、
-- 生产看板、收发类别、系统字典与业务字典、发票税率、微信小程序、智能（AI）、采购入库 / 销售出库直接开单、进销存报表等。
--
-- 可重复执行：已经执行过的部分会自动跳过。执行步骤：
--   1. 备份数据库：mysqldump -uroot -p lemon_erp > lemon_erp_backup.sql
--   2. 执行本脚本：mysql -uroot -p lemon_erp < sql/upgrade/20261106_v1.0.0.sql
--   3. 部署新版本的后端和前端并重启
--   4. 在「系统监控 → 缓存监控」中清空「用户权限」「字典数据」「系统参数」缓存（或删除 Redis 中 login:perms:*、dict:* 开头的键和 settings:all）

-- ==================== 单据打印改用 JasperReports ====================
-- 打印模板可上传 JRXML；原有的打印模板配置（字段、列、签字栏）继续有效，纸张默认为 A4

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

CALL lemon_add_column('sys_print_template', 'jrxml', 'jrxml MEDIUMTEXT COMMENT ''自定义 JRXML（Jaspersoft Studio 7），为空时按配置生成默认版式'' AFTER config');
ALTER TABLE sys_print_template COMMENT = '打印模板';

-- ==================== 凭证辅助核算 ====================
-- 辅助核算改为在科目上设置（可多选，手工凭证必填），分录的客户、供应商、存货各自一列，可同时填写

CALL lemon_add_column('fin_account', 'aux_items', 'aux_items VARCHAR(64) DEFAULT NULL COMMENT ''辅助核算项目（逗号分隔，可多选） DEPT部门 CUSTOMER客户 SUPPLIER供应商 PROJECT项目 MATERIAL存货；手工凭证使用该科目时必填'' AFTER remark');
CALL lemon_add_column('fin_voucher_entry', 'customer_id', 'customer_id BIGINT DEFAULT NULL COMMENT ''客户（辅助核算）'' AFTER project_id');
CALL lemon_add_column('fin_voucher_entry', 'supplier_id', 'supplier_id BIGINT DEFAULT NULL COMMENT ''供应商（辅助核算）'' AFTER customer_id');
CALL lemon_add_column('fin_voucher_entry', 'material_id', 'material_id BIGINT DEFAULT NULL COMMENT ''存货（辅助核算）'' AFTER supplier_id');
DROP PROCEDURE IF EXISTS lemon_add_column;

-- 原往来单位拆到客户、供应商列（只在还有原来的 partner_type 列时执行）
DROP PROCEDURE IF EXISTS lemon_voucher_aux;
DELIMITER //
CREATE PROCEDURE lemon_voucher_aux()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_voucher_entry' AND COLUMN_NAME = 'partner_type') THEN
        UPDATE fin_voucher_entry SET customer_id = partner_id WHERE partner_type = 'CUSTOMER' AND partner_id IS NOT NULL;
        UPDATE fin_voucher_entry SET supplier_id = partner_id WHERE partner_type = 'SUPPLIER' AND partner_id IS NOT NULL;
        ALTER TABLE fin_voucher_entry
            DROP INDEX idx_entry_partner,
            DROP COLUMN partner_type,
            DROP COLUMN partner_id,
            ADD KEY idx_entry_customer (customer_id),
            ADD KEY idx_entry_supplier (supplier_id),
            ADD KEY idx_entry_material (material_id);
    END IF;
END //
DELIMITER ;
CALL lemon_voucher_aux();
DROP PROCEDURE IF EXISTS lemon_voucher_aux;

-- 往来科目默认按客户 / 供应商辅助核算（只设置还没有设置过的科目，含各租户）
UPDATE fin_account SET aux_items = 'CUSTOMER' WHERE code IN ('1122', '2203') AND aux_items IS NULL;
UPDATE fin_account SET aux_items = 'SUPPLIER' WHERE code IN ('1123', '2202') AND aux_items IS NULL;

-- ====================================================
-- 凭证科目设置（自动凭证各用途使用的科目可配置，存货、收入、成本可按物料分类，工资费用可按职位类别）。可重复执行

CREATE TABLE IF NOT EXISTS fin_account_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role        VARCHAR(32) NOT NULL COMMENT '用途（AccountRole），如 RECEIVABLE 应收账款、INVENTORY 存货',
    dim_value   VARCHAR(32) NOT NULL DEFAULT '' COMMENT '维度取值：物料分类 / 职位类别，空串为该用途的默认科目',
    account_id  BIGINT      NOT NULL COMMENT '科目（末级）',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_rule (role, dim_value, tenant_id),
    KEY idx_account_rule_account (account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证科目设置：业务单据自动生成凭证时各用途使用的科目，没有设置的用途取预置科目';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 340 AS id, 936 AS parent_id, '凭证科目设置' AS name, 'C' AS type, 'account-rule' AS path, 'fin/account-rule/index' AS component, 'fin:accountRule:list' AS perms, 'NodeIndexOutlined' AS icon, 5 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3400, 340, '科目设置修改', 'F', NULL, NULL, 'fin:accountRule:edit', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- 普通用户查看，会计全部（各租户的内置角色按编码关联）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 340 AS menu_id
      UNION ALL SELECT 'accountant', 340
      UNION ALL SELECT 'accountant', 3400) m ON m.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- ====================================================
-- 存货计价方法（物料可选月末一次加权平均、移动加权平均、先进先出）。可重复执行

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'cost_method');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN cost_method VARCHAR(16) NOT NULL DEFAULT ''WEIGHTED'' COMMENT ''存货计价方法(字典 wms_cost_method) WEIGHTED月末一次加权平均 MOVING移动加权平均 FIFO先进先出'' AFTER ref_cost', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_material_cost' AND COLUMN_NAME = 'cost_method');
SET @s = IF(@c = 0, 'ALTER TABLE fin_material_cost ADD COLUMN cost_method VARCHAR(16) NOT NULL DEFAULT ''WEIGHTED'' COMMENT ''本期使用的计价方法(字典 wms_cost_method)'' AFTER unit_cost', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS fin_cost_layer (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT            NOT NULL COMMENT '会计期间',
    material_id BIGINT         NOT NULL COMMENT '物料',
    seq         INT            NOT NULL COMMENT '批次顺序，越小入库越早',
    in_date     DATE           NOT NULL COMMENT '入库日期',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '结存数量',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '结存金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_cost_layer (period, material_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '先进先出的期末结存批次：下一期核算的期初批次';

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time)
SELECT 155, '存货计价方法', 'wms_cost_method', 1, '物料的存货计价方法，月末成本核算时按此计算出库成本', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'wms_cost_method');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'wms_cost_method' AS dict_type, '月末一次加权平均' AS dict_label, 'WEIGHTED' AS dict_value, 'blue' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'wms_cost_method', '移动加权平均', 'MOVING', 'cyan', 2, 1, NOW(), NOW()
    UNION ALL SELECT 'wms_cost_method', '先进先出', 'FIFO', 'purple', 3, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 启用向导（期初库存导入生成「期初入库」单）。可重复执行

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT 'wms_in_type', '期初入库', 'OPENING', 'gold', 10, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'wms_in_type' AND dict_value = 'OPENING');

-- ====================================================
-- 出纳管理（现金 / 银行日记账、资金日报）菜单。可重复执行

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 341, 933, '出纳管理', 'C', 'cashier', 'fin/cashier/index', 'fin:cashier:list', 'WalletOutlined', 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 341);

-- 普通用户、总经理、会计、出纳可查看（各租户的内置角色按编码关联）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 341
FROM sys_role r
WHERE r.code IN ('common', 'gm', 'accountant', 'cashier')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 341);

-- ====================================================
-- 数电票导入（XML、发票清单），导入的发票可以之后再勾稽应收应付单。可重复执行

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

CALL lemon_add_column('fin_invoice', 'matched_amount', 'matched_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''已勾稽应收应付单的金额，导入的发票可以之后再勾稽'' AFTER tax_amount');
CALL lemon_add_column('fin_invoice', 'seller_tax_no', 'seller_tax_no VARCHAR(32) DEFAULT NULL COMMENT ''销售方纳税人识别号'' AFTER matched_amount');
CALL lemon_add_column('fin_invoice', 'seller_name', 'seller_name VARCHAR(128) DEFAULT NULL COMMENT ''销售方名称'' AFTER seller_tax_no');
CALL lemon_add_column('fin_invoice', 'buyer_tax_no', 'buyer_tax_no VARCHAR(32) DEFAULT NULL COMMENT ''购买方纳税人识别号'' AFTER seller_name');
CALL lemon_add_column('fin_invoice', 'buyer_name', 'buyer_name VARCHAR(128) DEFAULT NULL COMMENT ''购买方名称'' AFTER buyer_tax_no');
CALL lemon_add_column('fin_invoice', 'source', 'source VARCHAR(16) NOT NULL DEFAULT ''MANUAL'' COMMENT ''来源 MANUAL手工登记 XML数电票文件导入 EXCEL发票清单导入'' AFTER buyer_name');
DROP PROCEDURE IF EXISTS lemon_add_column;

-- 原有的发票登记时已全部勾稽
UPDATE fin_invoice i
SET i.matched_amount = (SELECT COALESCE(SUM(t.amount), 0) FROM fin_invoice_item t WHERE t.invoice_id = i.id)
WHERE i.matched_amount = 0 AND i.source = 'MANUAL';

CREATE TABLE IF NOT EXISTS fin_invoice_line (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    seq         INT            NOT NULL COMMENT '行号',
    item_name   VARCHAR(200)   NOT NULL COMMENT '项目名称（含税收分类简称）',
    spec        VARCHAR(128)            DEFAULT NULL COMMENT '规格型号',
    unit        VARCHAR(32)             DEFAULT NULL COMMENT '单位',
    qty         DECIMAL(18, 6)          DEFAULT NULL COMMENT '数量',
    price       DECIMAL(18, 8)          DEFAULT NULL COMMENT '单价（不含税）',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额（不含税）',
    tax_rate    DECIMAL(6, 4)           DEFAULT NULL COMMENT '税率，如 0.13',
    tax_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_line_invoice (invoice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票明细行：导入数电票时保存';

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'fin_invoice_type' AS dict_type, '数电票（专票）' AS dict_label, 'E_SPECIAL' AS dict_value, 'volcano' AS tag_color, 3 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'fin_invoice_type', '数电票（普票）', 'E_NORMAL', 'geekblue', 4, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 多规格物料（规格属性、款式与 SKU）。可重复执行

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

CALL lemon_add_column('wms_material', 'is_template', 'is_template TINYINT NOT NULL DEFAULT 0 COMMENT ''是否多规格款式 1是：按规格属性生成 SKU，款式本身不能出入库'' AFTER cost_method');
CALL lemon_add_column('wms_material', 'template_id', 'template_id BIGINT DEFAULT NULL COMMENT ''SKU 所属的款式（物料）'' AFTER is_template');
CALL lemon_add_column('wms_material', 'variant_text', 'variant_text VARCHAR(255) DEFAULT NULL COMMENT ''SKU 的规格属性值，如 红色 / XL'' AFTER template_id');
DROP PROCEDURE IF EXISTS lemon_add_column;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND INDEX_NAME = 'idx_material_template');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD KEY idx_material_template (template_id)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS wms_attribute (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(32) NOT NULL COMMENT '规格属性名称，如 颜色、尺码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性：多规格物料（款式）按属性值组合生成 SKU';

CREATE TABLE IF NOT EXISTS wms_attribute_value (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    attribute_id BIGINT      NOT NULL COMMENT '规格属性',
    attr_value   VARCHAR(32) NOT NULL COMMENT '属性值，如 红色、XL',
    code         VARCHAR(16)          DEFAULT NULL COMMENT '编码，生成 SKU 编码时使用，为空用属性值',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_value (attribute_id, attr_value, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性值';

CREATE TABLE IF NOT EXISTS wms_variant_value (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_id  BIGINT NOT NULL COMMENT '款式',
    material_id  BIGINT NOT NULL COMMENT 'SKU',
    attribute_id BIGINT NOT NULL COMMENT '规格属性',
    value_id     BIGINT NOT NULL COMMENT '属性值',
    create_time  DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_variant_value (material_id, attribute_id, tenant_id),
    KEY idx_variant_template (template_id),
    KEY idx_variant_value (value_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 的规格属性值';

-- 菜单：仓库 → 基础资料 → 规格属性
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 342 AS id, 914 AS parent_id, '规格属性' AS name, 'C' AS type, 'attribute' AS path, 'wms/attribute/index' AS component, 'wms:attribute:list' AS perms, 'TagsOutlined' AS icon, 4 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3420, 342, '规格属性维护', 'F', NULL, NULL, 'wms:attribute:edit', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 342 AS menu_id UNION ALL SELECT 'gm', 342 UNION ALL SELECT 'purchaser', 342
      UNION ALL SELECT 'warehouse', 342 UNION ALL SELECT 'warehouse', 3420) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 各租户预置颜色、尺码（没有任何规格属性的租户）
INSERT INTO wms_attribute (tenant_id, name, sort, status, create_time, update_time)
SELECT t.id, a.name, a.sort, 1, NOW(), NOW()
FROM sys_tenant t
JOIN (SELECT '颜色' AS name, 1 AS sort UNION ALL SELECT '尺码', 2) a
WHERE NOT EXISTS (SELECT 1 FROM wms_attribute x WHERE x.tenant_id = t.id);

INSERT INTO wms_attribute_value (tenant_id, attribute_id, attr_value, code, sort, create_time, update_time)
SELECT a.tenant_id, a.id, v.value, v.code, v.sort, NOW(), NOW()
FROM wms_attribute a
JOIN (SELECT '颜色' AS attr, '黑色' AS value, 'BK' AS code, 1 AS sort
      UNION ALL SELECT '颜色', '白色', 'WH', 2 UNION ALL SELECT '颜色', '红色', 'RD', 3 UNION ALL SELECT '颜色', '蓝色', 'BL', 4
      UNION ALL SELECT '尺码', 'S', 'S', 1 UNION ALL SELECT '尺码', 'M', 'M', 2 UNION ALL SELECT '尺码', 'L', 'L', 3
      UNION ALL SELECT '尺码', 'XL', 'XL', 4 UNION ALL SELECT '尺码', 'XXL', 'XXL', 5) v ON v.attr = a.name
WHERE NOT EXISTS (SELECT 1 FROM wms_attribute_value x WHERE x.attribute_id = a.id);

-- ====================================================
-- 组装拆卸单。可重复执行

CREATE TABLE IF NOT EXISTS wms_assembly (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_no     VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '种类(字典 wms_assembly_kind) ASSEMBLE组装 DISASSEMBLE拆卸',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    material_id     BIGINT         NOT NULL COMMENT '成套件（组装得到 / 拆卸的物料）',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '成套件库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '成套件批次号',
    production_date DATE                    DEFAULT NULL COMMENT '成套件生产日期（组装入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '成套件有效期至（组装入库新批次）',
    serial_nos      TEXT COMMENT '成套件序列号，逗号分隔',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '成套件数量',
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 DONE已完成',
    out_doc_id      BIGINT                  DEFAULT NULL COMMENT '生成的出库单（组装为子件出库，拆卸为成套件出库）',
    out_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '生成的出库单号',
    in_doc_id       BIGINT                  DEFAULT NULL COMMENT '生成的入库单（组装为成套件入库，拆卸为子件入库）',
    in_doc_no       VARCHAR(32)             DEFAULT NULL COMMENT '生成的入库单号',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assembly_no (assembly_no, tenant_id),
    KEY idx_assembly_date (biz_date),
    KEY idx_assembly_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单：组装消耗子件得到成套件，拆卸相反；确认后生成出库、入库单（类型 ASSEMBLY），成本核算按子件成本计算成套件成本';

CREATE TABLE IF NOT EXISTS wms_assembly_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_id     BIGINT         NOT NULL COMMENT '组装拆卸单',
    material_id     BIGINT         NOT NULL COMMENT '子件',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期（拆卸入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至（拆卸入库新批次）',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_assembly_item_assembly (assembly_id),
    KEY idx_assembly_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单的子件';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 343 AS id, 911 AS parent_id, '组装拆卸' AS name, 'C' AS type, 'assembly' AS path, 'wms/assembly/index' AS component, 'wms:assembly:list' AS perms, 'BuildOutlined' AS icon, 5 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3430, 343, '组装拆卸维护', 'F', NULL, NULL, 'wms:assembly:add', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3431, 343, '组装拆卸确认', 'F', NULL, NULL, 'wms:assembly:confirm', NULL, 2, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 343 AS menu_id UNION ALL SELECT 'gm', 343 UNION ALL SELECT 'accountant', 343
      UNION ALL SELECT 'warehouse', 343 UNION ALL SELECT 'warehouse', 3430 UNION ALL SELECT 'warehouse', 3431) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time)
SELECT 156, '组装拆卸种类', 'wms_assembly_kind', 1, '组装、拆卸', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'wms_assembly_kind');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'wms_assembly_kind' AS dict_type, '组装' AS dict_label, 'ASSEMBLE' AS dict_value, 'blue' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'wms_assembly_kind', '拆卸', 'DISASSEMBLE', 'orange', 2, 1, NOW(), NOW()
    UNION ALL SELECT 'wms_in_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW()
    UNION ALL SELECT 'wms_out_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 销售提成（销售订单的业务员、提成规则与按月计算，确认后计入工资表）。可重复执行

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

CALL lemon_add_column('scm_order', 'salesman_id', 'salesman_id BIGINT DEFAULT NULL COMMENT ''业务员（销售订单，计算销售提成）：默认客户的负责销售，没有时为制单人'' AFTER exchange_rate');
CALL lemon_add_column('hr_payroll_item', 'commission', 'commission DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''销售提成（取已确认的销售提成表）'' AFTER piece_wage');
DROP PROCEDURE IF EXISTS lemon_add_column;

-- 已有销售订单的业务员：客户的负责销售，没有时为制单人
UPDATE scm_order o
SET o.salesman_id = COALESCE((SELECT c.owner_id FROM scm_customer c WHERE c.id = o.partner_id), o.create_by)
WHERE o.kind = 'SALES' AND o.salesman_id IS NULL;

CREATE TABLE IF NOT EXISTS fin_commission_rule (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '规则名称',
    basis             VARCHAR(16)    NOT NULL COMMENT '提成依据(字典 fin_commission_basis) SALES销售额（不含税） PROFIT毛利 RECEIPT回款',
    salesman_id       BIGINT                  DEFAULT NULL COMMENT '适用业务员，为空适用全部',
    material_category VARCHAR(32)             DEFAULT NULL COMMENT '适用物料分类(字典 wms_material_category)，为空适用全部；回款不区分分类',
    threshold         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '业绩门槛：业务员当月该依据的业绩达到此金额时适用（阶梯提成）',
    rate              DECIMAL(6, 3)  NOT NULL COMMENT '提成比例（%）',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成规则：同一业务员、分类、依据取最具体（指定业务员 > 指定分类）且门槛最高的已达成规则';

CREATE TABLE IF NOT EXISTS fin_commission (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period       INT            NOT NULL COMMENT '期间 yyyyMM',
    status       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 CONFIRMED已确认（计入工资表）',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '提成合计（含调整）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '计算人',
    confirm_by   BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_commission_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成表：按期间计算各业务员的业绩和提成';

CREATE TABLE IF NOT EXISTS fin_commission_line (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    commission_id     BIGINT         NOT NULL COMMENT '提成表',
    salesman_id       BIGINT         NOT NULL COMMENT '业务员',
    sales_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售额（不含税，扣除退货）',
    profit_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '毛利（销售额 - 成本）',
    receipt_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '回款',
    commission_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '按规则计算的提成',
    adjust_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手工调整',
    final_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '应发提成 = 提成 + 调整',
    detail            TEXT COMMENT '计算明细（JSON）：依据、分类、业绩、规则、比例、提成',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注（调整原因）',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_commission_line (commission_id, salesman_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成表明细：每个业务员一行';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 344 AS id, 902 AS parent_id, '销售提成' AS name, 'C' AS type, 'commission' AS path, 'fin/commission/index' AS component, 'fin:commission:list' AS perms, 'PercentageOutlined' AS icon, 20 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3440, 344, '提成规则维护', 'F', NULL, NULL, 'fin:commission:rule', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3441, 344, '提成计算与确认', 'F', NULL, NULL, 'fin:commission:edit', NULL, 2, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 344 AS menu_id UNION ALL SELECT 'gm', 344 UNION ALL SELECT 'hr', 344
      UNION ALL SELECT 'accountant', 344 UNION ALL SELECT 'accountant', 3440 UNION ALL SELECT 'accountant', 3441) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time)
SELECT 157, '提成依据', 'fin_commission_basis', 1, '销售提成的计算依据', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'fin_commission_basis');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'fin_commission_basis' AS dict_type, '销售额' AS dict_label, 'SALES' AS dict_value, 'blue' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'fin_commission_basis', '毛利', 'PROFIT', 'green', 2, 1, NOW(), NOW()
    UNION ALL SELECT 'fin_commission_basis', '回款', 'RECEIPT', 'gold', 3, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 套打（打印模板的套打底图）。可重复执行

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template' AND COLUMN_NAME = 'background');
SET @s = IF(@c = 0, 'ALTER TABLE sys_print_template ADD COLUMN background MEDIUMTEXT COMMENT ''套打底图（data URL）：设计套打位置时对照，不打印'' AFTER jrxml', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ====================================================
-- 业务参数（按租户的业务控制规则：超收超发比例、信用控制方式、凭证审核等）。可重复执行

CREATE TABLE IF NOT EXISTS sys_biz_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(64)  NOT NULL COMMENT '参数标识，如 scm.purchaseIn.overRate',
    rule_value  VARCHAR(255) NOT NULL COMMENT '参数值',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务参数（按租户）：各模块的业务控制规则，只保存修改过的';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 346 AS id, 1 AS parent_id, '业务参数' AS name, 'C' AS type, 'biz-rule' AS path, 'system/biz-rule/index' AS component, 'sys:bizRule:list' AS perms, 'SlidersOutlined' AS icon, 12 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3460, 346, '业务参数修改', 'F', NULL, NULL, 'sys:bizRule:edit', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 346 FROM sys_role r
WHERE r.code = 'common' AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 346);

-- ====================================================
-- 业务报表菜单（收发存汇总、往来对账单）。可重复执行
-- 销售毛利明细在「经营分析 → 销售毛利」中，无需菜单

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 347 AS id, 912 AS parent_id, '收发存汇总' AS name, 'C' AS type, 'in-out' AS path, 'wms/inout/index' AS component, 'wms:inout:list' AS perms, 'SwapOutlined' AS icon, 5 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 348, 932, '往来对账单', 'C', 'partner-statement', 'fin/partner-statement/index', 'fin:partnerStatement:list', 'AuditOutlined', 8, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 347 AS menu_id UNION ALL SELECT 'gm', 347 UNION ALL SELECT 'purchaser', 347
      UNION ALL SELECT 'warehouse', 347 UNION ALL SELECT 'accountant', 347
      UNION ALL SELECT 'common', 348 UNION ALL SELECT 'gm', 348 UNION ALL SELECT 'accountant', 348 UNION ALL SELECT 'cashier', 348) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- ====================================================
-- 价格体系（客户专属价、价格控制）。可重复执行

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_price_list' AND COLUMN_NAME = 'customer_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_price_list ADD COLUMN customer_id BIGINT DEFAULT NULL COMMENT ''客户专属价：指定客户时只适用该客户（优先于客户等级）'' AFTER material_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS scm_price_control (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id        BIGINT         NOT NULL COMMENT '物料',
    min_sale_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低销售价（含税，基本单位，本位币），为空不控制',
    max_purchase_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '最高采购价（含税，基本单位，本位币），为空不控制',
    control_mode       VARCHAR(8)     NOT NULL DEFAULT 'WARN' COMMENT '控制方式(字典 scm_price_control_mode) WARN提示 BLOCK禁止保存',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_control (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格控制：销售订单单价不低于最低销售价，采购订单单价不高于最高采购价（手工录入的订单）';

INSERT INTO sys_dict_type (name, type, status, remark, create_time, update_time)
SELECT '价格控制方式', 'scm_price_control_mode', 1, '超出最低销售价 / 最高采购价时的处理', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'scm_price_control_mode');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'scm_price_control_mode' AS dict_type, '提示' AS dict_label, 'WARN' AS dict_value, 'orange' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'scm_price_control_mode', '禁止保存', 'BLOCK', 'red', 2, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 自定义字段。可重复执行

CREATE TABLE IF NOT EXISTS sys_custom_field (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（与附件相同的业务记录类型，如 scm_customer、wms_material）',
    field_key    VARCHAR(32)  NOT NULL COMMENT '字段标识，字母开头的字母、数字、下划线',
    label        VARCHAR(32)  NOT NULL COMMENT '显示名称',
    field_type   VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '类型(字典 sys_custom_field_type) TEXT文本 TEXTAREA多行文本 NUMBER数字 DATE日期 SELECT下拉 BOOL是否',
    options      VARCHAR(1000)         DEFAULT NULL COMMENT '下拉选项，逗号或换行分隔',
    required     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填',
    show_in_list TINYINT      NOT NULL DEFAULT 0 COMMENT '是否在列表中显示',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '说明（显示为输入提示）',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_field (biz_type, field_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段：为单据、主数据扩展字段，不改表结构';

CREATE TABLE IF NOT EXISTS sys_custom_value (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)   NOT NULL COMMENT '业务类型',
    biz_id      BIGINT        NOT NULL COMMENT '业务记录ID',
    field_key   VARCHAR(32)   NOT NULL COMMENT '字段标识',
    field_value VARCHAR(1000) NOT NULL COMMENT '值（文本；数字、日期、是否按约定格式）',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_value (biz_type, biz_id, field_key, tenant_id),
    KEY idx_custom_value_field (biz_type, field_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段的值';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 345 AS id, 1 AS parent_id, '自定义字段' AS name, 'C' AS type, 'custom-field' AS path, 'system/custom-field/index' AS component, 'sys:customField:list' AS perms, 'FormOutlined' AS icon, 11 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3450, 345, '自定义字段维护', 'F', NULL, NULL, 'sys:customField:edit', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 345 FROM sys_role r
WHERE r.code = 'common' AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 345);

INSERT INTO sys_dict_type (name, type, status, remark, create_time, update_time)
SELECT '自定义字段类型', 'sys_custom_field_type', 1, '自定义字段的输入类型', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'sys_custom_field_type');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'sys_custom_field_type' AS dict_type, '文本' AS dict_label, 'TEXT' AS dict_value, 'default' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'sys_custom_field_type', '多行文本', 'TEXTAREA', 'default', 2, 1, NOW(), NOW()
    UNION ALL SELECT 'sys_custom_field_type', '数字', 'NUMBER', 'blue', 3, 1, NOW(), NOW()
    UNION ALL SELECT 'sys_custom_field_type', '日期', 'DATE', 'cyan', 4, 1, NOW(), NOW()
    UNION ALL SELECT 'sys_custom_field_type', '下拉选择', 'SELECT', 'purple', 5, 1, NOW(), NOW()
    UNION ALL SELECT 'sys_custom_field_type', '是否', 'BOOL', 'green', 6, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 数据大屏与投屏链接。可重复执行
-- 大屏汇总全公司数据，只授权给有「全部数据」权限的内置角色（总经理、仓库管理员）

CREATE TABLE IF NOT EXISTS bi_screen_share (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name             VARCHAR(64)  NOT NULL COMMENT '名称，如 一车间电视',
    token            CHAR(32)     NOT NULL COMMENT '访问令牌（128 位随机数），链接中使用',
    screens          VARCHAR(128) NOT NULL COMMENT '大屏（逗号分隔） FACTORY生产车间 WAREHOUSE仓储物流 SALES销售经营 QUALITY质量管理，多个时轮播',
    interval_seconds INT          NOT NULL DEFAULT 60 COMMENT '轮播间隔（秒）',
    work_center_id   BIGINT                DEFAULT NULL COMMENT '生产车间大屏只显示该工作中心，为空显示全部',
    expire_time      DATETIME              DEFAULT NULL COMMENT '有效期至，为空长期有效',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_access_time DATETIME              DEFAULT NULL COMMENT '最近访问时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_screen_share_token (token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大屏投屏链接：车间、仓库的电视免登录查看指定的大屏（只读）';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 349 AS id, 11 AS parent_id, '数据大屏' AS name, 'C' AS type, 'screen' AS path, 'bi/screen/index' AS component, 'bi:screen:list' AS perms, 'FundProjectionScreenOutlined' AS icon, 0 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3490, 349, '投屏链接管理', 'F', NULL, NULL, 'bi:screen:share', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'gm' AS code, 349 AS menu_id UNION ALL SELECT 'gm', 3490 UNION ALL SELECT 'warehouse', 349) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- ====================================================
-- 第三方集成（钉钉、企业微信、飞书）。可重复执行
-- 菜单只给超级管理员（包含应用密钥），租户管理员按套餐获得

CREATE TABLE IF NOT EXISTS sys_im_config (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    platform          VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK钉钉 WECOM企业微信 FEISHU飞书',
    enabled           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否启用 1是 0否',
    corp_id           VARCHAR(64)           DEFAULT NULL COMMENT '企业ID（企业微信 corpid）',
    app_key           VARCHAR(64)           DEFAULT NULL COMMENT '应用标识（钉钉 Client ID / AppKey，飞书 App ID）',
    app_secret        VARCHAR(128)          DEFAULT NULL COMMENT '应用密钥（钉钉 Client Secret，企业微信应用 Secret，飞书 App Secret），不返回前端',
    agent_id          VARCHAR(32)           DEFAULT NULL COMMENT '应用 AgentId（钉钉、企业微信，发送工作通知用）',
    home_url          VARCHAR(255)          DEFAULT NULL COMMENT '系统访问地址（外网可访问的前端地址），登录回调和消息链接使用',
    login_enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '扫码登录与应用内免登 1开启 0关闭',
    message_enabled   TINYINT      NOT NULL DEFAULT 1 COMMENT '站内消息同步推送给已绑定的成员 1开启 0关闭',
    sync_create       TINYINT      NOT NULL DEFAULT 0 COMMENT '同步通讯录时自动创建部门和账号 1是 0否（否则只按手机号绑定已有账号）',
    robot_webhook     VARCHAR(500)          DEFAULT NULL COMMENT '群机器人 Webhook 地址',
    robot_secret      VARCHAR(128)          DEFAULT NULL COMMENT '群机器人加签密钥（钉钉、飞书），不返回前端',
    robot_categories  VARCHAR(64)           DEFAULT NULL COMMENT '推送到群机器人的消息类别（逗号分隔，字典 sys_message_category）',
    last_sync_time    DATETIME              DEFAULT NULL COMMENT '最近同步通讯录时间',
    last_sync_result  VARCHAR(255)          DEFAULT NULL COMMENT '最近同步结果',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_config (platform, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '第三方集成（钉钉、企业微信、飞书）的应用配置，按租户';

CREATE TABLE IF NOT EXISTS sys_im_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '系统用户',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK WECOM FEISHU',
    im_user_id  VARCHAR(64)  NOT NULL COMMENT '平台中的成员 userid',
    im_name     VARCHAR(64)           DEFAULT NULL COMMENT '平台中的成员姓名',
    create_time DATETIME              DEFAULT NULL COMMENT '绑定时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_user_member (platform, im_user_id, tenant_id),
    UNIQUE KEY uk_im_user_user (user_id, platform, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户与钉钉 / 企业微信 / 飞书成员的绑定';

CREATE TABLE IF NOT EXISTS sys_im_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    dept_id     BIGINT       NOT NULL COMMENT '系统部门',
    platform    VARCHAR(16)  NOT NULL COMMENT '平台 DINGTALK WECOM FEISHU',
    im_dept_id  VARCHAR(64)  NOT NULL COMMENT '平台中的部门 ID',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_im_dept (platform, im_dept_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '同步通讯录时系统部门与平台部门的对应关系';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 350 AS id, 1 AS parent_id, '第三方集成' AS name, 'C' AS type, 'im' AS path, 'system/im/index' AS component, 'sys:im:list' AS perms, 'ApiOutlined' AS icon, 13 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3500, 350, '集成配置修改', 'F', NULL, NULL, 'sys:im:edit', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3501, 350, '通讯录同步与绑定', 'F', NULL, NULL, 'sys:im:sync', NULL, 2, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_dict_type (name, type, status, remark, create_time, update_time)
SELECT '集成平台', 'sys_im_platform', 1, '第三方集成的平台', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type = 'sys_im_platform');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 'sys_im_platform' AS dict_type, '钉钉' AS dict_label, 'DINGTALK' AS dict_value, 'blue' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 'sys_im_platform', '企业微信', 'WECOM', 'green', 2, 1, NOW(), NOW()
    UNION ALL SELECT 'sys_im_platform', '飞书', 'FEISHU', 'cyan', 3, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 生产看板。可重复执行
-- 菜单放在「生产 → 车间执行」下，授权给普通用户、总经理、生产计划员、车间主管

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 351, 922, '生产看板', 'C', 'board', 'mes/board/index', 'mes:board:list', 'DashboardOutlined', 0, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 351);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 351
FROM sys_role r
WHERE r.code IN ('common', 'gm', 'planner', 'workshop')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 351);

-- ====================================================
-- 系统字典 / 业务字典、收发类别。可重复执行
-- 业务字典每个租户各自一份：已有租户从平台复制一份；系统字典仍为平台共用
-- 收发类别的出入库单字段、凭证科目设置见下一段

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND COLUMN_NAME = 'kind');
SET @s = IF(@c = 0, 'ALTER TABLE sys_dict_type ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT ''SYSTEM'' COMMENT ''种类 SYSTEM系统字典（取值由程序内置，全平台共用，只能改名称、颜色、排序） BUSINESS业务字典（每个租户各自维护，可新增取值）'' AFTER type', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'tenant_id');
SET @s = IF(@c = 0, 'ALTER TABLE sys_dict_data ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID：系统字典只有平台（1）一份；业务字典每个租户一份，新租户开通时从平台复制'' AFTER id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'builtin');
SET @s = IF(@c = 0, 'ALTER TABLE sys_dict_data ADD COLUMN builtin TINYINT NOT NULL DEFAULT 0 COMMENT ''内置取值 1是：程序依赖该取值，不能删除、不能改键值（系统字典的取值全部内置）'' AFTER status', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 唯一约束加上租户
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND INDEX_NAME = 'uk_dict_type_value' AND COLUMN_NAME = 'tenant_id');
SET @s = IF(@c = 0, 'ALTER TABLE sys_dict_data DROP INDEX uk_dict_type_value, ADD UNIQUE KEY uk_dict_type_value (dict_type, dict_value, tenant_id)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 收发类别
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT t.* FROM (
    SELECT '入库类别' AS name, 'wms_in_category' AS type, 'BUSINESS' AS kind, 1 AS status, '其他入库的用途分类，可在凭证科目设置中按类别指定对方科目' AS remark, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT '出库类别', 'wms_out_category', 'BUSINESS', 1, '其他出库、报废出库的用途分类，可在凭证科目设置中按类别指定对方科目', NOW(), NOW()
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = t.type);

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 1 AS tenant_id, 'wms_in_category' AS dict_type, '样品退回' AS dict_label, 'SAMPLE_RETURN' AS dict_value, 'blue' AS tag_color, 1 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 1, 'wms_in_category', '受赠入库', 'GIFT', 'green', 2, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_in_category', '领用退回', 'USE_RETURN', 'cyan', 3, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_in_category', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_out_category', '样品出库', 'SAMPLE', 'blue', 1, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_out_category', '赠品出库', 'GIFT', 'green', 2, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_out_category', '福利领用', 'WELFARE', 'orange', 3, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_out_category', '研发领用', 'RD', 'purple', 4, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_out_category', '办公领用', 'OFFICE', 'cyan', 5, 1, NOW(), NOW()
    UNION ALL SELECT 1, 'wms_out_category', '其他', 'OTHER', 'default', 9, 1, NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value AND x.tenant_id = 1);

-- 字典种类：业务字典由各租户自己维护（可新增取值），其余为系统字典；程序依赖的取值标记为内置（不能删除、不能改键值）
UPDATE sys_dict_type SET kind = 'BUSINESS' WHERE type IN ('wf_category', 'scm_customer_level', 'scm_carrier', 'mes_supplement_reason',
    'fin_asset_category', 'hr_education', 'hr_candidate_source', 'crm_industry', 'crm_campaign_type', 'hr_training_category',
    'crm_follow_method', 'crm_ticket_channel', 'pm_priority', 'wms_material_category', 'hr_position_category', 'fin_settle_method',
    'mes_defect_type', 'crm_source', 'hr_emp_type', 'crm_ticket_type', 'pm_project_type', 'hr_contract_type', 'hr_leave_type',
    'pm_member_role', 'mes_ecn_reason', 'mes_doc_type', 'qms_responsibility', 'mes_mold_type', 'mes_stop_reason');
UPDATE sys_dict_data SET builtin = 1
WHERE dict_type NOT IN (SELECT type FROM sys_dict_type WHERE kind = 'BUSINESS')
   OR (dict_type = 'wms_material_category' AND dict_value IN ('RAW', 'SEMI', 'FINISHED', 'LOW_VALUE'))
   OR (dict_type = 'hr_position_category' AND dict_value IN ('PRODUCTION', 'SALES'))
   OR (dict_type = 'fin_settle_method' AND dict_value IN ('TRANSFER', 'NOTE'))
   OR (dict_type IN ('mes_defect_type', 'crm_source') AND dict_value = 'OTHER')
   OR (dict_type = 'hr_emp_type' AND dict_value = 'FULL_TIME')
   OR (dict_type = 'hr_contract_type' AND dict_value IN ('FIXED', 'OPEN'))
   OR (dict_type = 'hr_leave_type' AND dict_value IN ('ANNUAL', 'PERSONAL', 'SICK', 'COMPENSATORY'))
   OR (dict_type = 'pm_member_role' AND dict_value IN ('MANAGER', 'DEVELOPER'))
   OR dict_type = 'wf_category'
   OR (dict_type = 'scm_customer_level' AND dict_value = 'POTENTIAL')
   OR (dict_type = 'crm_ticket_type' AND dict_value = 'REPAIR')
   OR (dict_type = 'pm_project_type' AND dict_value = 'DELIVERY')
   OR (dict_type = 'mes_ecn_reason' AND dict_value = 'DESIGN')
   OR (dict_type = 'mes_doc_type' AND dict_value = 'DRAWING')
   OR (dict_type = 'mes_mold_type' AND dict_value = 'MOLD')
   OR (dict_type = 'qms_responsibility' AND dict_value = 'SUPPLIER')
   OR (dict_type = 'fin_asset_category' AND dict_value = 'ELECTRONIC')
   OR (dict_type = 'mes_stop_reason' AND dict_value IN ('BREAKDOWN', 'MAINTAIN', 'NO_ORDER'))
   OR (dict_type IN ('crm_follow_method', 'crm_ticket_channel') AND dict_value = 'PHONE')
   OR (dict_type = 'crm_ticket_channel' AND dict_value = 'ONSITE')
   OR (dict_type = 'pm_priority' AND dict_value = 'MEDIUM')
   OR (dict_type = 'hr_training_category' AND dict_value = 'SKILL');

-- 已有租户：复制一份平台的业务字典
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, remark, create_time, update_time)
SELECT t.id, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, d.status, d.builtin, d.remark, NOW(), NOW()
FROM sys_tenant t
JOIN sys_dict_data d ON d.tenant_id = 1
JOIN sys_dict_type dt ON dt.type = d.dict_type AND dt.kind = 'BUSINESS'
WHERE t.id <> 1
  AND NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = t.id AND x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- 菜单：系统管理 → 业务字典，授权给已有「部门管理」的角色（各租户的管理员），并加入包含系统管理的套餐
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 352 AS id, 1 AS parent_id, '业务字典' AS name, 'C' AS type, 'biz-dict' AS path, 'system/bizDict/index' AS component, 'sys:bizdict:list' AS perms, 'TagsOutlined' AS icon, 5 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3520, 352, '业务字典维护', 'F', NULL, NULL, 'sys:bizdict:edit', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 104) p
JOIN sys_menu m ON m.id IN (352, 3520)
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.id
FROM (SELECT DISTINCT rm.role_id FROM sys_role_menu rm JOIN sys_role ro ON ro.id = rm.role_id WHERE rm.menu_id = 104 AND ro.code <> 'common') r
JOIN sys_menu m ON m.id IN (352, 3520)
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = m.id);

-- 业务字典的键值最长 16 位，项目优先级字段放宽到 16
ALTER TABLE pm_project MODIFY COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级(字典 pm_priority)';

-- ====================================================
-- 出入库单的收发类别。可重复执行
-- 依赖上一段的收发类别字典 wms_in_category / wms_out_category
-- 凭证科目设置中「其他入库对方科目」「其他出库对方科目」两个用途按类别指定科目，未设置时仍为待处理财产损溢，无需初始化数据

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'io_category');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN io_category VARCHAR(32) DEFAULT NULL COMMENT ''收发类别：其他入库为字典 wms_in_category，其他出库、报废出库为字典 wms_out_category（业务字典），成本凭证按类别取对方科目；业务单据生成的为空'' AFTER biz_type', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ====================================================
-- 发票明细税率改为百分数存储（与订单、报价等其他单据一致，13 表示 13%）。可重复执行
-- 原列 DECIMAL(6,4) 存小数（0.13）。先放宽为 DECIMAL(9,4) 以免乘 100 后溢出，再换算并改为 DECIMAL(5,2)；按列的当前定义判断进行到哪一步

SET @p = (SELECT NUMERIC_PRECISION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_invoice_line' AND COLUMN_NAME = 'tax_rate');
SET @s = IF(@p = 6, 'ALTER TABLE fin_invoice_line MODIFY COLUMN tax_rate DECIMAL(9, 4) DEFAULT NULL COMMENT ''税率（换算中）''', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @p = (SELECT NUMERIC_PRECISION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_invoice_line' AND COLUMN_NAME = 'tax_rate');
SET @s = IF(@p = 9, 'UPDATE fin_invoice_line SET tax_rate = tax_rate * 100 WHERE tax_rate IS NOT NULL', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = IF(@p = 9, 'ALTER TABLE fin_invoice_line MODIFY COLUMN tax_rate DECIMAL(5, 2) DEFAULT NULL COMMENT ''税率（%），如 13''', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ====================================================
-- 微信小程序（lemon-mp）登录绑定。可重复执行

CREATE TABLE IF NOT EXISTS sys_miniapp_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '系统用户',
    openid      VARCHAR(64)  NOT NULL COMMENT '微信小程序 openid',
    create_time DATETIME              DEFAULT NULL COMMENT '绑定时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_miniapp_user_openid (openid, tenant_id),
    UNIQUE KEY uk_miniapp_user_user (user_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户与微信小程序的绑定（登录时按 openid 跨租户查询）';

-- ====================================================
-- 智能（AI）模块第一期——模型配置（按租户）、用量、AI 问数的会话与消息，顶部模块「智能」。可重复执行
-- AI 设置只给超级管理员（包含 API Key），租户管理员按套餐获得；AI 问数给平台租户的总经理角色（按角色编码关联）

CREATE TABLE IF NOT EXISTS ai_config (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    provider            VARCHAR(16)   NOT NULL COMMENT '服务商 DEEPSEEK QWEN ZHIPU MOONSHOT DOUBAO CUSTOM（AiProvider）',
    base_url            VARCHAR(255)           DEFAULT NULL COMMENT '接口地址（不含 /chat/completions），为空时用服务商的默认地址',
    api_key             VARCHAR(255)           DEFAULT NULL COMMENT 'API Key，不返回前端',
    chat_model          VARCHAR(64)            DEFAULT NULL COMMENT '对话 / 工具调用模型',
    vision_model        VARCHAR(64)            DEFAULT NULL COMMENT '识图模型，为空时智能录入只支持 PDF、Excel、文本',
    enabled             TINYINT       NOT NULL DEFAULT 0 COMMENT '总开关 1启用 0停用',
    monthly_token_limit BIGINT                 DEFAULT NULL COMMENT '每月 token 上限（输入 + 输出），为空不限',
    chat_enabled        TINYINT       NOT NULL DEFAULT 0 COMMENT 'AI 问数 1开启 0关闭',
    extract_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '智能录入 1开启 0关闭',
    cleanse_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '数据整理 1开启 0关闭',
    report_enabled      TINYINT       NOT NULL DEFAULT 0 COMMENT '智能报告 1开启 0关闭',
    temperature         DECIMAL(3, 2) NOT NULL DEFAULT 0.30 COMMENT '温度',
    max_tokens          INT                    DEFAULT NULL COMMENT '单次回答的最大 token 数，为空由服务商决定',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_config (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型配置（国产模型，OpenAI 兼容协议），每个租户一条';

CREATE TABLE IF NOT EXISTS ai_usage (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id           BIGINT               DEFAULT NULL COMMENT '调用人，没有登录用户时为空',
    feature           VARCHAR(16) NOT NULL COMMENT '功能 CHAT问数 EXTRACT录入 CLEANSE整理 REPORT报告 TEST测试连接（AiFeature）',
    model             VARCHAR(64)          DEFAULT NULL COMMENT '模型',
    prompt_tokens     INT         NOT NULL DEFAULT 0 COMMENT '输入 token',
    completion_tokens INT         NOT NULL DEFAULT 0 COMMENT '输出 token',
    create_time       DATETIME             DEFAULT NULL COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_ai_usage_time (tenant_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型调用的 token 用量';

CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '所属用户（只有本人能访问）',
    title       VARCHAR(100)          DEFAULT NULL COMMENT '标题（默认为第一条提问）',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '最近提问时间',
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user (user_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的会话';

CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    conversation_id BIGINT      NOT NULL COMMENT '会话',
    role            VARCHAR(16) NOT NULL COMMENT '角色 USER提问 ASSISTANT回答 TOOL工具结果（AiMessageRole）',
    content         TEXT COMMENT '提问 / 回答的文字；工具消息为调用参数',
    tool_call_id    VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：对应的工具调用 ID',
    tool_name       VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：工具名称',
    tool_calls      TEXT COMMENT '回答：请求调用的工具（JSON），列表不返回',
    tool_result     MEDIUMTEXT COMMENT '工具消息：回传给模型的结果（JSON），列表不返回',
    tokens          INT                  DEFAULT NULL COMMENT '回答消耗的 token',
    create_time     DATETIME             DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation (conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的消息';

-- 顶部导航排在商务智能（id 11）之后：只在还没有「智能」模块时把后面的模块顺延一位
UPDATE sys_menu m JOIN (SELECT sort FROM sys_menu WHERE id = 11) bi ON 1 = 1
SET m.sort = m.sort + 1
WHERE m.parent_id = 0 AND m.sort > bi.sort
  AND NOT EXISTS (SELECT 1 FROM (SELECT id FROM sys_menu WHERE id = 16) x);

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 16 AS id, 0 AS parent_id, '智能' AS name, 'M' AS type, '/ai' AS path, NULL AS component, NULL AS perms, 'RobotOutlined' AS icon,
           COALESCE((SELECT sort + 1 FROM sys_menu WHERE id = 11), 10) AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 360, 16, 'AI 问数', 'C', 'chat', 'ai/chat/index', 'ai:chat:use', 'MessageOutlined', 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 361, 16, 'AI 设置', 'C', 'settings', 'ai/settings/index', 'ai:config:list', 'SettingOutlined', 99, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3610, 361, 'AI 设置修改', 'F', NULL, NULL, 'ai:config:edit', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r
JOIN (SELECT 16 AS menu_id UNION ALL SELECT 360) m
WHERE r.code = 'gm' AND r.tenant_id = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- ====================================================
-- 智能录入（菜单 362，ai:extract:use）。可重复执行
-- 执行后清理权限缓存（删除 Redis 中的 login:perms:*）

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 362 AS id, 16 AS parent_id, '智能录入' AS name, 'C' AS type, 'extract' AS path, 'ai/extract/index' AS component,
           'ai:extract:use' AS perms, 'ScanOutlined' AS icon, 2 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- 平台租户的内置角色：总经理、采购员、销售员、会计（各自只能生成有新增权限的单据），连带「智能」模块
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r
JOIN (SELECT 16 AS menu_id UNION ALL SELECT 362) m ON 1 = 1
WHERE r.code IN ('gm', 'purchaser', 'sales', 'accountant') AND r.tenant_id = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- ====================================================
-- 智能（AI）数据整理（物料、客户、供应商的查重与规范化建议）。可重复执行

CREATE TABLE IF NOT EXISTS ai_cleanse_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    source_key      VARCHAR(64)  NOT NULL COMMENT '数据类型（AiCleanseSource.key，如 wms.material）',
    keyword         VARCHAR(64)           DEFAULT NULL COMMENT '编码或名称关键字（整理范围），为空表示全部',
    status          VARCHAR(16)  NOT NULL COMMENT '状态 RUNNING进行中 COMPLETED已完成 FAILED失败 CANCELLED已取消（AiCleanseTaskStatus）',
    phase           VARCHAR(16)  NOT NULL COMMENT '阶段 LOADING读取 DUPLICATE查重 NORMALIZE规范化 DONE结束（AiCleansePhase）',
    running_key     VARCHAR(64)           DEFAULT NULL COMMENT '进行中时为数据类型，结束后清空（唯一约束：同一数据类型同时只有一个进行中的任务）',
    record_count    INT          NOT NULL DEFAULT 0 COMMENT '读取的记录数',
    truncated       TINYINT      NOT NULL DEFAULT 0 COMMENT '记录超过上限，只分析了一部分 1是 0否',
    group_count     INT          NOT NULL DEFAULT 0 COMMENT '疑似重复候选组数',
    total_steps     INT          NOT NULL DEFAULT 0 COMMENT '模型调用总步数',
    done_steps      INT          NOT NULL DEFAULT 0 COMMENT '已完成步数',
    failed_steps    INT          NOT NULL DEFAULT 0 COMMENT '模型输出不合格而跳过的步数',
    duplicate_count INT          NOT NULL DEFAULT 0 COMMENT '疑似重复建议数（待停用的记录）',
    normalize_count INT          NOT NULL DEFAULT 0 COMMENT '规范化建议数',
    missing_count   INT          NOT NULL DEFAULT 0 COMMENT '缺失字段提示数',
    discarded_count INT          NOT NULL DEFAULT 0 COMMENT '校验不通过而丢弃的建议数',
    tokens          INT          NOT NULL DEFAULT 0 COMMENT '消耗的 token',
    error_key       VARCHAR(100)          DEFAULT NULL COMMENT '失败原因的消息键',
    error_args      VARCHAR(1000)         DEFAULT NULL COMMENT '失败原因的参数（JSON 数组）',
    create_by       BIGINT                DEFAULT NULL COMMENT '发起人',
    create_time     DATETIME              DEFAULT NULL COMMENT '发起时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '最近进展时间',
    finish_time     DATETIME              DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_cleanse_task_running (running_key, tenant_id),
    KEY idx_ai_cleanse_task_source (tenant_id, source_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理任务（主数据查重与规范化）';

CREATE TABLE IF NOT EXISTS ai_cleanse_suggestion (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    task_id        BIGINT       NOT NULL COMMENT '整理任务',
    kind           VARCHAR(16)  NOT NULL COMMENT '种类 DUPLICATE疑似重复 NORMALIZE规范化 MISSING缺失字段（AiCleanseKind）',
    group_no       INT                   DEFAULT NULL COMMENT '疑似重复的组号',
    record_id      BIGINT       NOT NULL COMMENT '记录ID',
    record_code    VARCHAR(64)           DEFAULT NULL COMMENT '记录编码',
    record_name    VARCHAR(255)          DEFAULT NULL COMMENT '记录名称',
    field          VARCHAR(16)           DEFAULT NULL COMMENT '字段 NAME SPEC CATEGORY UNIT CONTACT ADDRESS（AiCleanseField），疑似重复为空',
    old_value      VARCHAR(255)          DEFAULT NULL COMMENT '生成建议时的原值',
    new_value      VARCHAR(255)          DEFAULT NULL COMMENT '建议值',
    keep_record_id BIGINT                DEFAULT NULL COMMENT '疑似重复：建议保留的记录',
    keep_code      VARCHAR(64)           DEFAULT NULL COMMENT '保留记录的编码',
    keep_name      VARCHAR(255)          DEFAULT NULL COMMENT '保留记录的名称',
    reason         VARCHAR(500)          DEFAULT NULL COMMENT '理由（模型给出）',
    confidence     INT                   DEFAULT NULL COMMENT '置信度 0～100',
    status         VARCHAR(16)  NOT NULL COMMENT '状态 PENDING待处理 APPLIED已采纳 IGNORED已忽略（AiSuggestionStatus）',
    handled_by     BIGINT                DEFAULT NULL COMMENT '采纳 / 忽略人',
    handled_time   DATETIME              DEFAULT NULL COMMENT '采纳 / 忽略时间',
    create_time    DATETIME              DEFAULT NULL COMMENT '生成时间',
    PRIMARY KEY (id),
    KEY idx_ai_cleanse_suggestion_task (task_id, kind, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理建议';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 363 AS id, 16 AS parent_id, '数据整理' AS name, 'C' AS type, 'cleanse' AS path, 'ai/cleanse/index' AS component,
           'ai:cleanse:list' AS perms, 'ClearOutlined' AS icon, 3 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3630, 363, '采纳整理建议', 'F', NULL, NULL, 'ai:cleanse:apply', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- 给平台租户的采购员（供应商）、仓库管理员（物料），连同「智能」模块
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r
JOIN (SELECT 16 AS menu_id UNION ALL SELECT 363 UNION ALL SELECT 3630) m
WHERE r.code IN ('purchaser', 'warehouse') AND r.tenant_id = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- ====================================================
-- 智能（AI）第二期——智能报告（周报 / 月报，手动生成或定时任务自动生成）与 BI 预警的 AI 分析。可重复执行
-- 定时任务「智能报告-周报」「智能报告-月报」由程序首次启动时自动登记到 sys_job，不需要脚本插入
-- 菜单给平台租户的总经理角色（数据权限为全部数据，能看自动生成的全公司报告；按角色编码关联）

CREATE TABLE IF NOT EXISTS ai_report (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    period_type       VARCHAR(8)   NOT NULL COMMENT '期间类型 WEEK周报 MONTH月报（AiReportPeriodType）',
    period_start      DATE         NOT NULL COMMENT '期间开始',
    period_end        DATE         NOT NULL COMMENT '期间结束（含）',
    title             VARCHAR(100) NOT NULL COMMENT '标题',
    content           MEDIUMTEXT COMMENT '报告正文（Markdown），列表不返回',
    user_id           BIGINT                DEFAULT NULL COMMENT '生成人，定时任务自动生成时为空（自动报告只有全部数据权限的用户能看）',
    sources           VARCHAR(500)          DEFAULT NULL COMMENT '包含的数据来源标识，逗号分隔',
    snapshot          MEDIUMTEXT COMMENT '交给模型的数据快照（JSON），列表不返回',
    status            VARCHAR(16)  NOT NULL COMMENT '状态 GENERATING生成中 DONE已完成 FAILED失败（AiReportStatus）',
    error             VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
    prompt_tokens     INT                   DEFAULT NULL COMMENT '输入 token',
    completion_tokens INT                   DEFAULT NULL COMMENT '输出 token',
    create_time       DATETIME              DEFAULT NULL COMMENT '生成时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_report_period (tenant_id, period_type, period_start),
    KEY idx_ai_report_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能报告（周报 / 月报）';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 364 AS id, 16 AS parent_id, '智能报告' AS name, 'C' AS type, 'report' AS path, 'ai/report/index' AS component,
           'ai:report:list' AS perms, 'FileTextOutlined' AS icon, 4 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3640, 364, '生成报告', 'F', NULL, NULL, 'ai:report:generate', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.menu_id FROM sys_role r
JOIN (SELECT 16 AS menu_id UNION ALL SELECT 364 UNION ALL SELECT 3640) m ON 1 = 1
WHERE r.code = 'gm' AND r.tenant_id = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.menu_id);

-- ====================================================
-- 智能（AI）模块——模型配置、用量、AI 问数、智能录入、数据整理、智能报告。可重复执行
-- 定时任务（智能周报、月报）由程序启动时注册，无需初始化数据；菜单授权与 init.sql 相同，按角色编码给各租户的内置角色

CREATE TABLE IF NOT EXISTS ai_config (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    provider            VARCHAR(16)   NOT NULL COMMENT '服务商 DEEPSEEK QWEN ZHIPU MOONSHOT DOUBAO CUSTOM（AiProvider）',
    base_url            VARCHAR(255)           DEFAULT NULL COMMENT '接口地址（不含 /chat/completions），为空时用服务商的默认地址',
    api_key             VARCHAR(255)           DEFAULT NULL COMMENT 'API Key，不返回前端',
    chat_model          VARCHAR(64)            DEFAULT NULL COMMENT '对话 / 工具调用模型',
    vision_model        VARCHAR(64)            DEFAULT NULL COMMENT '识图模型，为空时智能录入只支持 PDF、Excel、文本',
    enabled             TINYINT       NOT NULL DEFAULT 0 COMMENT '总开关 1启用 0停用',
    monthly_token_limit BIGINT                 DEFAULT NULL COMMENT '每月 token 上限（输入 + 输出），为空不限',
    chat_enabled        TINYINT       NOT NULL DEFAULT 0 COMMENT 'AI 问数 1开启 0关闭',
    extract_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '智能录入 1开启 0关闭',
    cleanse_enabled     TINYINT       NOT NULL DEFAULT 0 COMMENT '数据整理 1开启 0关闭',
    report_enabled      TINYINT       NOT NULL DEFAULT 0 COMMENT '智能报告 1开启 0关闭',
    temperature         DECIMAL(3, 2) NOT NULL DEFAULT 0.30 COMMENT '温度',
    max_tokens          INT                    DEFAULT NULL COMMENT '单次回答的最大 token 数，为空由服务商决定',
    create_time         DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_config (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型配置（国产模型，OpenAI 兼容协议），每个租户一条';

CREATE TABLE IF NOT EXISTS ai_usage (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id           BIGINT               DEFAULT NULL COMMENT '调用人，没有登录用户时为空',
    feature           VARCHAR(16) NOT NULL COMMENT '功能 CHAT问数 EXTRACT录入 CLEANSE整理 REPORT报告 TEST测试连接（AiFeature）',
    model             VARCHAR(64)          DEFAULT NULL COMMENT '模型',
    prompt_tokens     INT         NOT NULL DEFAULT 0 COMMENT '输入 token',
    completion_tokens INT         NOT NULL DEFAULT 0 COMMENT '输出 token',
    create_time       DATETIME             DEFAULT NULL COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_ai_usage_time (tenant_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型调用的 token 用量';

CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '所属用户（只有本人能访问）',
    title       VARCHAR(100)          DEFAULT NULL COMMENT '标题（默认为第一条提问）',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '最近提问时间',
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user (user_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的会话';

CREATE TABLE IF NOT EXISTS ai_message (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    conversation_id BIGINT      NOT NULL COMMENT '会话',
    role            VARCHAR(16) NOT NULL COMMENT '角色 USER提问 ASSISTANT回答 TOOL工具结果（AiMessageRole）',
    content         TEXT COMMENT '提问 / 回答的文字；工具消息为调用参数',
    tool_call_id    VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：对应的工具调用 ID',
    tool_name       VARCHAR(64)          DEFAULT NULL COMMENT '工具消息：工具名称',
    tool_calls      TEXT COMMENT '回答：请求调用的工具（JSON），列表不返回',
    tool_result     MEDIUMTEXT COMMENT '工具消息：回传给模型的结果（JSON），列表不返回',
    tokens          INT                  DEFAULT NULL COMMENT '回答消耗的 token',
    create_time     DATETIME             DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation (conversation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 问数的消息';

CREATE TABLE IF NOT EXISTS ai_cleanse_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    source_key      VARCHAR(64)  NOT NULL COMMENT '数据类型（AiCleanseSource.key，如 wms.material）',
    keyword         VARCHAR(64)           DEFAULT NULL COMMENT '编码或名称关键字（整理范围），为空表示全部',
    status          VARCHAR(16)  NOT NULL COMMENT '状态 RUNNING进行中 COMPLETED已完成 FAILED失败 CANCELLED已取消（AiCleanseTaskStatus）',
    phase           VARCHAR(16)  NOT NULL COMMENT '阶段 LOADING读取 DUPLICATE查重 NORMALIZE规范化 DONE结束（AiCleansePhase）',
    running_key     VARCHAR(64)           DEFAULT NULL COMMENT '进行中时为数据类型，结束后清空（唯一约束：同一数据类型同时只有一个进行中的任务）',
    record_count    INT          NOT NULL DEFAULT 0 COMMENT '读取的记录数',
    truncated       TINYINT      NOT NULL DEFAULT 0 COMMENT '记录超过上限，只分析了一部分 1是 0否',
    group_count     INT          NOT NULL DEFAULT 0 COMMENT '疑似重复候选组数',
    total_steps     INT          NOT NULL DEFAULT 0 COMMENT '模型调用总步数',
    done_steps      INT          NOT NULL DEFAULT 0 COMMENT '已完成步数',
    failed_steps    INT          NOT NULL DEFAULT 0 COMMENT '模型输出不合格而跳过的步数',
    duplicate_count INT          NOT NULL DEFAULT 0 COMMENT '疑似重复建议数（待停用的记录）',
    normalize_count INT          NOT NULL DEFAULT 0 COMMENT '规范化建议数',
    missing_count   INT          NOT NULL DEFAULT 0 COMMENT '缺失字段提示数',
    discarded_count INT          NOT NULL DEFAULT 0 COMMENT '校验不通过而丢弃的建议数',
    tokens          INT          NOT NULL DEFAULT 0 COMMENT '消耗的 token',
    error_key       VARCHAR(100)          DEFAULT NULL COMMENT '失败原因的消息键',
    error_args      VARCHAR(1000)         DEFAULT NULL COMMENT '失败原因的参数（JSON 数组）',
    create_by       BIGINT                DEFAULT NULL COMMENT '发起人',
    create_time     DATETIME              DEFAULT NULL COMMENT '发起时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '最近进展时间',
    finish_time     DATETIME              DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_cleanse_task_running (running_key, tenant_id),
    KEY idx_ai_cleanse_task_source (tenant_id, source_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理任务（主数据查重与规范化）';

CREATE TABLE IF NOT EXISTS ai_cleanse_suggestion (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    task_id        BIGINT       NOT NULL COMMENT '整理任务',
    kind           VARCHAR(16)  NOT NULL COMMENT '种类 DUPLICATE疑似重复 NORMALIZE规范化 MISSING缺失字段（AiCleanseKind）',
    group_no       INT                   DEFAULT NULL COMMENT '疑似重复的组号',
    record_id      BIGINT       NOT NULL COMMENT '记录ID',
    record_code    VARCHAR(64)           DEFAULT NULL COMMENT '记录编码',
    record_name    VARCHAR(255)          DEFAULT NULL COMMENT '记录名称',
    field          VARCHAR(16)           DEFAULT NULL COMMENT '字段 NAME SPEC CATEGORY UNIT CONTACT ADDRESS（AiCleanseField），疑似重复为空',
    old_value      VARCHAR(255)          DEFAULT NULL COMMENT '生成建议时的原值',
    new_value      VARCHAR(255)          DEFAULT NULL COMMENT '建议值',
    keep_record_id BIGINT                DEFAULT NULL COMMENT '疑似重复：建议保留的记录',
    keep_code      VARCHAR(64)           DEFAULT NULL COMMENT '保留记录的编码',
    keep_name      VARCHAR(255)          DEFAULT NULL COMMENT '保留记录的名称',
    reason         VARCHAR(500)          DEFAULT NULL COMMENT '理由（模型给出）',
    confidence     INT                   DEFAULT NULL COMMENT '置信度 0～100',
    status         VARCHAR(16)  NOT NULL COMMENT '状态 PENDING待处理 APPLIED已采纳 IGNORED已忽略（AiSuggestionStatus）',
    handled_by     BIGINT                DEFAULT NULL COMMENT '采纳 / 忽略人',
    handled_time   DATETIME              DEFAULT NULL COMMENT '采纳 / 忽略时间',
    create_time    DATETIME              DEFAULT NULL COMMENT '生成时间',
    PRIMARY KEY (id),
    KEY idx_ai_cleanse_suggestion_task (task_id, kind, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据整理建议';

CREATE TABLE IF NOT EXISTS ai_report (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    period_type       VARCHAR(8)   NOT NULL COMMENT '期间类型 WEEK周报 MONTH月报（AiReportPeriodType）',
    period_start      DATE         NOT NULL COMMENT '期间开始',
    period_end        DATE         NOT NULL COMMENT '期间结束（含）',
    title             VARCHAR(100) NOT NULL COMMENT '标题',
    content           MEDIUMTEXT COMMENT '报告正文（Markdown），列表不返回',
    user_id           BIGINT                DEFAULT NULL COMMENT '生成人，定时任务自动生成时为空（自动报告只有全部数据权限的用户能看）',
    sources           VARCHAR(500)          DEFAULT NULL COMMENT '包含的数据来源标识，逗号分隔',
    snapshot          MEDIUMTEXT COMMENT '交给模型的数据快照（JSON），列表不返回',
    status            VARCHAR(16)  NOT NULL COMMENT '状态 GENERATING生成中 DONE已完成 FAILED失败（AiReportStatus）',
    error             VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
    prompt_tokens     INT                   DEFAULT NULL COMMENT '输入 token',
    completion_tokens INT                   DEFAULT NULL COMMENT '输出 token',
    create_time       DATETIME              DEFAULT NULL COMMENT '生成时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_report_period (tenant_id, period_type, period_start),
    KEY idx_ai_report_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能报告（周报 / 月报）';

-- 顶部导航：「智能」排在商务智能之后（只在第一次执行时后移排序）
SET @has_ai = (SELECT COUNT(*) FROM sys_menu WHERE id = 16);
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 0 AND sort >= 10 AND @has_ai = 0;

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 16 AS id, 0 AS parent_id, '智能' AS name, 'M' AS type, '/ai' AS path, NULL AS component, NULL AS perms, 'RobotOutlined' AS icon, 10 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 360, 16, 'AI 问数', 'C', 'chat', 'ai/chat/index', 'ai:chat:use', 'MessageOutlined', 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 361, 16, 'AI 设置', 'C', 'settings', 'ai/settings/index', 'ai:config:list', 'SettingOutlined', 99, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3610, 361, 'AI 设置修改', 'F', NULL, NULL, 'ai:config:edit', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 362, 16, '智能录入', 'C', 'extract', 'ai/extract/index', 'ai:extract:use', 'ScanOutlined', 2, 1, 1, NOW(), NOW()
    UNION ALL SELECT 363, 16, '数据整理', 'C', 'cleanse', 'ai/cleanse/index', 'ai:cleanse:list', 'ClearOutlined', 3, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3630, 363, '采纳整理建议', 'F', NULL, NULL, 'ai:cleanse:apply', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 364, 16, '智能报告', 'C', 'report', 'ai/report/index', 'ai:report:list', 'FileTextOutlined', 4, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3640, 364, '生成报告', 'F', NULL, NULL, 'ai:report:generate', NULL, 1, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- AI 问数、智能报告给总经理；智能录入给总经理、采购员、销售员、会计；数据整理给采购员、仓库管理员；AI 设置只给超级管理员（租户管理员按套餐获得）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (
    SELECT 'gm' AS code, 16 AS menu_id UNION ALL SELECT 'gm', 360 UNION ALL SELECT 'gm', 362 UNION ALL SELECT 'gm', 364 UNION ALL SELECT 'gm', 3640
    UNION ALL SELECT 'purchaser', 16 UNION ALL SELECT 'purchaser', 362 UNION ALL SELECT 'purchaser', 363 UNION ALL SELECT 'purchaser', 3630
    UNION ALL SELECT 'sales', 16 UNION ALL SELECT 'sales', 362
    UNION ALL SELECT 'accountant', 16 UNION ALL SELECT 'accountant', 362
    UNION ALL SELECT 'warehouse', 16 UNION ALL SELECT 'warehouse', 363 UNION ALL SELECT 'warehouse', 3630
) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- ====================================================
-- 「AI 设置」的 API Key 改为加密保存（密文比明文长，加宽字段）。可重复执行
-- 已保存的明文 API Key 不需要手工处理：系统读取时自动改写为密文

ALTER TABLE ai_config MODIFY COLUMN api_key VARCHAR(512) DEFAULT NULL COMMENT 'API Key（AES-GCM 加密，enc: 开头），不返回前端';

-- ====================================================
-- 直接开单与销售出库明细仓库（已有库升级用，可重复执行）
-- 1. 销售出库、采购入库（及退货）可以不选订单直接开单：系统生成并直接审核隐式订单，来源 DIRECT（字典 scm_order_source）
-- 2. 销售出库的明细行可以各自选择仓库：scm_trade_doc_item / wms_stock_doc_item 增加 warehouse_id，为空表示表头仓库
-- 业务参数 scm.directDoc.enabled、scm.salesOut.lineWarehouse 默认开启，未修改过时不需要在 sys_biz_rule 中插入数据

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc_item' AND COLUMN_NAME = 'warehouse_id');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc_item ADD COLUMN warehouse_id BIGINT DEFAULT NULL COMMENT ''本行仓库（仅销售出库可选），为空取表头仓库'' AFTER material_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'warehouse_id');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN warehouse_id BIGINT DEFAULT NULL COMMENT ''本行仓库（入库、出库），为空取表头仓库；调拨不用'' AFTER material_id', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE scm_order MODIFY COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 scm_order_source) MANUAL手工 MRP物料需求计划 CRM商机 KIT生产缺料 DIRECT直接开单（隐式订单）';

-- 订单来源是系统字典（平台租户一份）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT d.* FROM (
    SELECT 1 AS tenant_id, 'scm_order_source' AS dict_type, '直接开单' AS dict_label, 'DIRECT' AS dict_value, 'blue' AS tag_color, 10 AS sort, 1 AS status, NOW() AS create_time, NOW() AS update_time
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- ====================================================
-- 进销存报表（供应链 → 进销存报表：销售报表、进货报表、经营状况、资产状况、商品分析、客户分析）。可重复执行
-- 报表实时查询业务表，没有新表；菜单授权与 init.sql 相同，按角色编码给各租户的内置角色

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 966 AS id, 5 AS parent_id, '进销存报表' AS name, 'M' AS type, NULL AS path, NULL AS component, NULL AS perms, 'BarChartOutlined' AS icon, 5 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 370, 966, '销售报表', 'C', 'jxc-sales', 'jxc/sales/index', 'bi:jxc:sales', 'RiseOutlined', 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 371, 966, '进货报表', 'C', 'jxc-purchase', 'jxc/purchase/index', 'bi:jxc:purchase', 'ShoppingCartOutlined', 2, 1, 1, NOW(), NOW()
    UNION ALL SELECT 372, 966, '经营状况', 'C', 'jxc-business', 'jxc/business/index', 'bi:jxc:business', 'LineChartOutlined', 3, 1, 1, NOW(), NOW()
    UNION ALL SELECT 373, 966, '资产状况', 'C', 'jxc-assets', 'jxc/assets/index', 'bi:jxc:asset', 'AccountBookOutlined', 4, 1, 1, NOW(), NOW()
    UNION ALL SELECT 374, 966, '商品分析', 'C', 'jxc-goods', 'jxc/goods/index', 'bi:jxc:goods', 'AppstoreOutlined', 5, 1, 1, NOW(), NOW()
    UNION ALL SELECT 375, 966, '客户分析', 'C', 'jxc-rfm', 'jxc/rfm/index', 'bi:jxc:rfm', 'TeamOutlined', 6, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- 普通用户（除资产状况）、总经理、会计全部；采购员进货报表和商品分析；销售员销售报表和客户分析；仓库管理员商品分析；出纳资产状况（连同分组和供应链模块）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (
    SELECT 'common' AS code, 370 AS menu_id UNION ALL SELECT 'common', 371 UNION ALL SELECT 'common', 372 UNION ALL SELECT 'common', 374 UNION ALL SELECT 'common', 375
    UNION ALL SELECT 'gm', 370 UNION ALL SELECT 'gm', 371 UNION ALL SELECT 'gm', 372 UNION ALL SELECT 'gm', 373 UNION ALL SELECT 'gm', 374 UNION ALL SELECT 'gm', 375
    UNION ALL SELECT 'accountant', 370 UNION ALL SELECT 'accountant', 371 UNION ALL SELECT 'accountant', 372 UNION ALL SELECT 'accountant', 373
    UNION ALL SELECT 'accountant', 374 UNION ALL SELECT 'accountant', 375
    UNION ALL SELECT 'purchaser', 371 UNION ALL SELECT 'purchaser', 374
    UNION ALL SELECT 'sales', 370 UNION ALL SELECT 'sales', 375
    UNION ALL SELECT 'warehouse', 374
    UNION ALL SELECT 'cashier', 373
    UNION ALL SELECT 'common', 966 UNION ALL SELECT 'gm', 966 UNION ALL SELECT 'accountant', 966 UNION ALL SELECT 'purchaser', 966
    UNION ALL SELECT 'sales', 966 UNION ALL SELECT 'warehouse', 966 UNION ALL SELECT 'cashier', 966
    UNION ALL SELECT 'common', 5 UNION ALL SELECT 'gm', 5 UNION ALL SELECT 'accountant', 5 UNION ALL SELECT 'purchaser', 5
    UNION ALL SELECT 'sales', 5 UNION ALL SELECT 'warehouse', 5 UNION ALL SELECT 'cashier', 5
) g ON g.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- ====================================================
-- 价格体系（品牌、价格列、客户价格等级与执行价、价格记忆）。可重复执行
-- 价格记忆从历史的销售出库、采购入库回填（只补没有记录的 往来单位 × 物料）

-- 物料：品牌、零售价、批发价1-8
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'brand');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN brand VARCHAR(64) DEFAULT NULL COMMENT ''品牌(业务字典 wms_material_brand)'' AFTER category', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'retail_price');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN retail_price DECIMAL(18, 4) DEFAULT NULL COMMENT ''零售价（含税，基本单位，本位币）：价格列 0'' AFTER ref_cost', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price1');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price1 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价1（含税）'' AFTER retail_price', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price2');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price2 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价2（含税）'' AFTER wholesale_price1', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price3');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price3 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价3（含税）'' AFTER wholesale_price2', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price4');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price4 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价4（含税）'' AFTER wholesale_price3', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price5');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price5 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价5（含税）'' AFTER wholesale_price4', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price6');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price6 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价6（含税）'' AFTER wholesale_price5', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price7');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price7 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价7（含税）'' AFTER wholesale_price6', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'wholesale_price8');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN wholesale_price8 DECIMAL(18, 4) DEFAULT NULL COMMENT ''批发价8（含税）'' AFTER wholesale_price7', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 客户：价格等级
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_customer' AND COLUMN_NAME = 'price_level');
SET @s = IF(@c = 0, 'ALTER TABLE scm_customer ADD COLUMN price_level INT NOT NULL DEFAULT 0 COMMENT ''价格等级：开单默认价取物料的价格列 0零售价 1-8批发价1-8'' AFTER level', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS wms_price_level (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    level       INT         NOT NULL COMMENT '价格列 0零售价 1-8批发价1-8（对应 wms_material.retail_price / wholesale_price1-8）',
    name        VARCHAR(32) NOT NULL COMMENT '显示名称',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用：停用的列在物料表单、列表、导入导出和价格选择中隐藏',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_level (level, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格列设置：物料的零售价、批发价1-8的名称和是否启用（按租户）';

CREATE TABLE IF NOT EXISTS scm_customer_price (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    customer_id BIGINT         NOT NULL COMMENT '客户',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '执行价（含税，基本单位，本位币）',
    tax_rate    DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%），为空时开单不改税率',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_price (customer_id, material_id, tenant_id),
    KEY idx_customer_price_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户执行价：开单默认价最优先';

CREATE TABLE IF NOT EXISTS scm_price_memory (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind        VARCHAR(16)    NOT NULL COMMENT '种类 SALES销售（销售出库） PURCHASE采购（采购入库）',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    material_id BIGINT         NOT NULL COMMENT '物料',
    last_date   DATE           NOT NULL COMMENT '最新日期',
    last_price  DECIMAL(18, 4) NOT NULL COMMENT '最新价格（含税，基本单位，本位币）',
    min_date    DATE           NOT NULL COMMENT '最低价日期',
    min_price   DECIMAL(18, 4) NOT NULL COMMENT '最低价格',
    max_date    DATE           NOT NULL COMMENT '最高价日期',
    max_price   DECIMAL(18, 4) NOT NULL COMMENT '最高价格',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_memory (kind, partner_id, material_id, tenant_id),
    KEY idx_price_memory_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格记忆：销售出库、采购入库确认后登记往来单位 × 物料的最新 / 最低 / 最高价';

-- 价格列设置：每个租户 9 列，默认启用零售价、批发价1-3
INSERT INTO wms_price_level (tenant_id, level, name, status, create_time, update_time)
SELECT t.id, l.level, l.name, l.status, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT 0 AS level, '零售价' AS name, 1 AS status UNION ALL SELECT 1, '批发价1', 1 UNION ALL SELECT 2, '批发价2', 1
    UNION ALL SELECT 3, '批发价3', 1 UNION ALL SELECT 4, '批发价4', 0 UNION ALL SELECT 5, '批发价5', 0
    UNION ALL SELECT 6, '批发价6', 0 UNION ALL SELECT 7, '批发价7', 0 UNION ALL SELECT 8, '批发价8', 0
) l ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM wms_price_level x WHERE x.tenant_id = t.id AND x.level = l.level);

-- 品牌字典（业务字典）：平台一份，并给已有租户各复制一份
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '商品品牌', 'wms_material_brand', 'BUSINESS', 1, '物料的品牌，进销存报表可按品牌汇总', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'wms_material_brand');

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT t.id, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, 1, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT 'wms_material_brand' AS dict_type, '自有品牌' AS dict_label, 'OWN' AS dict_value, 'blue' AS tag_color, 1 AS sort
    UNION ALL SELECT 'wms_material_brand', '其他', 'OTHER', 'default', 9
) d ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value AND x.tenant_id = t.id);

-- 菜单：供应链 → 销售 → 商品价格；物料管理的「价格列设置」按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 365 AS id, 902 AS parent_id, '商品价格' AS name, 'C' AS type, 'goods-price' AS path, 'scm/goodsPrice/index' AS component,
           'scm:goodsPrice:list' AS perms, 'DollarOutlined' AS icon, 3 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3650, 365, '商品价格维护', 'F', NULL, NULL, 'scm:goodsPrice:edit', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3651, 137, '价格列设置', 'F', NULL, NULL, 'wms:material:priceLevel', NULL, 5, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- 普通用户、总经理、销售员只读（连带模块和分组）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 5 AS menu_id UNION ALL SELECT 902 UNION ALL SELECT 365) g ON 1 = 1
WHERE r.code IN ('common', 'gm', 'sales')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 价格记忆回填：按历史销售出库、采购入库（单价 > 0）汇总，最新价取最后一张单据的最后一行，最低 / 最高价日期取最早一次
INSERT INTO scm_price_memory (tenant_id, kind, partner_id, material_id, last_date, last_price, min_date, min_price, max_date, max_price,
                              create_time, update_time)
SELECT g.tenant_id, g.kind, g.partner_id, g.material_id, g.last_date,
       (SELECT i2.price FROM scm_trade_doc_item i2 JOIN scm_trade_doc d2 ON d2.id = i2.doc_id
        WHERE d2.tenant_id = g.tenant_id AND d2.kind = g.doc_kind AND d2.partner_id = g.partner_id AND i2.material_id = g.material_id
          AND i2.price > 0 AND d2.biz_date = g.last_date
        ORDER BY d2.id DESC, i2.id DESC LIMIT 1),
       (SELECT MIN(d2.biz_date) FROM scm_trade_doc_item i2 JOIN scm_trade_doc d2 ON d2.id = i2.doc_id
        WHERE d2.tenant_id = g.tenant_id AND d2.kind = g.doc_kind AND d2.partner_id = g.partner_id AND i2.material_id = g.material_id
          AND i2.price = g.min_price),
       g.min_price,
       (SELECT MIN(d2.biz_date) FROM scm_trade_doc_item i2 JOIN scm_trade_doc d2 ON d2.id = i2.doc_id
        WHERE d2.tenant_id = g.tenant_id AND d2.kind = g.doc_kind AND d2.partner_id = g.partner_id AND i2.material_id = g.material_id
          AND i2.price = g.max_price),
       g.max_price, NOW(), NOW()
FROM (
    SELECT d.tenant_id, d.kind AS doc_kind, CASE d.kind WHEN 'SALES_OUT' THEN 'SALES' ELSE 'PURCHASE' END AS kind, d.partner_id,
           i.material_id, MAX(d.biz_date) AS last_date, MIN(i.price) AS min_price, MAX(i.price) AS max_price
    FROM scm_trade_doc_item i
    JOIN scm_trade_doc d ON d.id = i.doc_id
    WHERE d.kind IN ('SALES_OUT', 'PURCHASE_IN') AND i.price > 0
    GROUP BY d.tenant_id, d.kind, d.partner_id, i.material_id
) g
WHERE NOT EXISTS (SELECT 1 FROM scm_price_memory x
                  WHERE x.tenant_id = g.tenant_id AND x.kind = g.kind AND x.partner_id = g.partner_id AND x.material_id = g.material_id);

-- ====================================================
-- 补齐两处变更。可重复执行
-- 1. AI 设置的 API Key 改为 AES-GCM 加密存储（enc: 开头），密文比原文长，列放宽到 512
-- 2. 订单来源「直接开单」是系统字典的取值，程序依赖，标记为内置（与 init.sql 一致）

ALTER TABLE ai_config MODIFY COLUMN api_key VARCHAR(512) DEFAULT NULL COMMENT 'API Key（AES-GCM 加密，enc: 开头），不返回前端';

UPDATE sys_dict_data SET builtin = 1 WHERE dict_type = 'scm_order_source' AND dict_value = 'DIRECT' AND builtin = 0;

-- ====================================================
-- 同步字段说明（只修改注释，类型和默认值不变）。可重复执行

ALTER TABLE wms_material MODIFY COLUMN wholesale_price1 DECIMAL(18, 4) DEFAULT NULL COMMENT '批发价1（含税）：价格列 1，名称在价格列设置中修改';
ALTER TABLE scm_trade_doc_item MODIFY COLUMN order_item_id BIGINT NOT NULL COMMENT '订单明细ID（直接开单为隐式订单的明细）';
ALTER TABLE fin_invoice MODIFY COLUMN invoice_type VARCHAR(16) NOT NULL DEFAULT 'SPECIAL' COMMENT '发票类型(字典 fin_invoice_type) SPECIAL增值税专用发票 NORMAL普通发票 E_SPECIAL数电票（专票） E_NORMAL数电票（普票）';
ALTER TABLE fin_material_cost MODIFY COLUMN unit_cost DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '本期出库单价（月末一次加权平均为加权平均单价，其他计价方法为本期出库的平均单价）';
ALTER TABLE bi_screen_share MODIFY COLUMN screens VARCHAR(128) NOT NULL COMMENT '大屏（逗号分隔） FACTORY生产车间 PROGRESS生产进度 WAREHOUSE仓储物流 SALES销售经营 QUALITY质量管理，多个时轮播';
ALTER TABLE bi_screen_share MODIFY COLUMN work_center_id BIGINT DEFAULT NULL COMMENT '生产车间、生产进度大屏只显示该工作中心，为空显示全部';
ALTER TABLE fin_material_cost COMMENT = '存货成本（按物料的计价方法核算）';
ALTER TABLE scm_price_list COMMENT = '销售价目表：取价时优先匹配客户专属价，其次客户等级，再取满足起订数量的最大一档';
