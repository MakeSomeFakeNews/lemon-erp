-- ====================================================
-- 现金银行（issue #39）与返利（issue #41）。可重复执行
-- 1. 其他收支：新表 fin_io_category（收支类别，每个租户一份，标明收 / 支并关联科目）、fin_io_doc / fin_io_doc_item（其他收入单、费用支出单）；
--    管理费用下增加明细科目 业务招待费 660206、水电费 660207、租赁费 660208、快递费 660209（科目编码不存在、管理费用已有下级时才增加）；
--    各租户复制内置收支类别（科目按编码关联，新增的四个明细科目还要求名称一致，不是末级的不关联，留空后在页面中设置）
-- 2. 账户调拨的手续费承担方 fin_fund_transfer.fee_bearer（已有数据为转出账户 FROM，与原来一致）
-- 3. 返利：scm_rebate 增加种类 kind（SALES 销售返利 / PURCHASE 采购返利，已有数据为销售返利）、计算方式 calc_mode（TIER 阶梯比例 / ITEM 按项目填写金额，
--    已有数据为阶梯比例）、供应商 supplier_id，客户改为可空；新表 scm_rebate_item（返利项目）；应收应付来源「销售返利」改名为「返利」
-- 4. 字典：收支方向、收支单状态、关联单位类型、手续费承担方、返利种类、返利计算方式（系统字典），凭证来源增加「其他收支」
-- 5. 菜单：财务 → 资金与费用 → 其他收支（fin:io:list/add/edit/remove/confirm/category），普通用户、总经理只读，会计、出纳全部；
--    供应链 → 采购 → 采购返利（scm:purchaseRebate:list/add/settle），普通用户查看、采购员维护、总经理和会计结算。
--    包含出纳管理 / 采购订单的套餐及其租户管理员随套餐加入；内置角色按编码授权，平台以外的租户只授权套餐中包含的菜单
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

DROP PROCEDURE IF EXISTS lemon_add_index;
DELIMITER //
CREATE PROCEDURE lemon_add_index(IN t VARCHAR(64), IN i VARCHAR(64), IN ddl VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t AND INDEX_NAME = i) THEN
        SET @s = CONCAT('ALTER TABLE ', t, ' ADD ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 1. 表结构
CALL lemon_add_column('fin_fund_transfer', 'fee_bearer', 'fee_bearer VARCHAR(8) NOT NULL DEFAULT ''FROM'' COMMENT ''手续费承担方(字典 fin_fee_bearer) FROM转出账户 TO转入账户'' AFTER fee');
ALTER TABLE fin_fund_transfer MODIFY COLUMN fee DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手续费（计入财务费用）';

CALL lemon_add_column('scm_rebate', 'kind', 'kind VARCHAR(16) NOT NULL DEFAULT ''SALES'' COMMENT ''种类(字典 scm_rebate_kind) SALES销售返利（客户） PURCHASE采购返利（供应商）'' AFTER rebate_no');
CALL lemon_add_column('scm_rebate', 'calc_mode', 'calc_mode VARCHAR(16) NOT NULL DEFAULT ''TIER'' COMMENT ''计算方式(字典 scm_rebate_calc) TIER阶梯比例 ITEM按项目填写金额'' AFTER kind');
ALTER TABLE scm_rebate MODIFY COLUMN customer_id BIGINT DEFAULT NULL COMMENT '客户（销售返利）';
CALL lemon_add_column('scm_rebate', 'supplier_id', 'supplier_id BIGINT DEFAULT NULL COMMENT ''供应商（采购返利）'' AFTER customer_id');
ALTER TABLE scm_rebate MODIFY COLUMN settle_mode VARCHAR(16) NOT NULL COMMENT '兑付方式(字典 scm_rebate_settle) CREDIT_NOTE红字应收（采购返利为红字应付） NEXT_ORDER下次订单抵扣';
ALTER TABLE scm_rebate MODIFY COLUMN sales_amount DECIMAL(18, 2) DEFAULT NULL COMMENT '结算：期间销售额 / 采购额（出库 - 退货或入库 - 退货，含税）';
ALTER TABLE scm_rebate MODIFY COLUMN bill_id BIGINT DEFAULT NULL COMMENT '红字应收 / 应付单（兑付方式为红字应收时）';
CALL lemon_add_index('scm_rebate', 'idx_rebate_supplier', 'KEY idx_rebate_supplier (supplier_id)');
ALTER TABLE scm_rebate COMMENT = '返利协议（销售返利、采购返利）';

DROP PROCEDURE IF EXISTS lemon_add_column;
DROP PROCEDURE IF EXISTS lemon_add_index;

CREATE TABLE IF NOT EXISTS scm_rebate_item (
    id        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    rebate_id BIGINT         NOT NULL COMMENT '返利协议',
    item_name VARCHAR(64)    NOT NULL COMMENT '返利项目',
    brand     VARCHAR(64)             DEFAULT NULL COMMENT '品牌(业务字典 wms_material_brand)',
    category  VARCHAR(32)             DEFAULT NULL COMMENT '物料分类(业务字典 wms_material_category)',
    amount    DECIMAL(18, 2) NOT NULL COMMENT '返利金额（含税）',
    remark    VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    PRIMARY KEY (id),
    KEY idx_rebate_item (rebate_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '返利项目（按项目填写金额）';

CREATE TABLE IF NOT EXISTS fin_io_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)  NOT NULL COMMENT '类别名称',
    direction   VARCHAR(8)   NOT NULL COMMENT '方向(字典 fin_fund_direction) IN收入 OUT支出',
    account_id  BIGINT                DEFAULT NULL COMMENT '凭证科目（末级）：收入记贷方、支出记借方',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_io_category (direction, name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收支类别（其他收入、费用支出的分类）';

CREATE TABLE IF NOT EXISTS fin_io_doc (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no        VARCHAR(32)    NOT NULL COMMENT '单号',
    direction     VARCHAR(8)     NOT NULL COMMENT '方向(字典 fin_fund_direction) IN其他收入 OUT费用支出',
    doc_date      DATE           NOT NULL COMMENT '日期',
    ticket_no     VARCHAR(64)             DEFAULT NULL COMMENT '票据编号',
    partner_type  VARCHAR(16)             DEFAULT NULL COMMENT '关联单位类型(字典 fin_io_partner_type) CUSTOMER客户 SUPPLIER供应商 OTHER其他',
    partner_id    BIGINT                  DEFAULT NULL COMMENT '客户 / 供应商',
    partner_name  VARCHAR(128)            DEFAULT NULL COMMENT '其他单位的名称',
    settle_method VARCHAR(32)             DEFAULT NULL COMMENT '结算方式(业务字典 fin_settle_method)',
    account_id    BIGINT         NOT NULL COMMENT '结算账户（现金 / 银行末级科目）',
    dept_id       BIGINT                  DEFAULT NULL COMMENT '费用归属部门',
    amount        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额合计',
    status        VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_io_status) DRAFT待确认 CONFIRMED已确认',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '确认生成的凭证',
    confirm_by    BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time  DATETIME                DEFAULT NULL COMMENT '确认时间',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by     BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_io_doc_no (doc_no, tenant_id),
    KEY idx_io_doc_date (doc_date),
    KEY idx_io_doc_partner (partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '其他收支单（其他收入单、费用支出单）';

CREATE TABLE IF NOT EXISTS fin_io_doc_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id      BIGINT         NOT NULL COMMENT '收支单',
    category_id BIGINT         NOT NULL COMMENT '收支类别',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_io_item_doc (doc_id),
    KEY idx_io_item_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '其他收支单明细';

-- 2. 管理费用的明细科目（各租户：编码不存在、管理费用已有下级时增加，不给已使用的末级科目增加下级）
INSERT INTO fin_account (tenant_id, code, name, parent_id, category, direction, status, create_time, update_time)
SELECT p.tenant_id, g.code, g.name, p.id, 'PROFIT', 'DEBIT', 1, NOW(), NOW()
FROM fin_account p
JOIN (
    SELECT '660206' AS code, '业务招待费' AS name
    UNION ALL SELECT '660207', '水电费'
    UNION ALL SELECT '660208', '租赁费'
    UNION ALL SELECT '660209', '快递费'
) g ON 1 = 1
WHERE p.code = '6602'
  AND EXISTS (SELECT 1 FROM fin_account c WHERE c.tenant_id = p.tenant_id AND c.parent_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM fin_account x WHERE x.tenant_id = p.tenant_id AND x.code = g.code);

-- 3. 内置收支类别（平台和已有租户各一份）：科目按编码关联，须为末级；新增的四个明细科目还要求名称一致，否则留空
INSERT INTO fin_io_category (tenant_id, name, direction, account_id, sort, status, create_time, update_time)
SELECT t.id, g.name, g.direction,
       (SELECT a.id FROM fin_account a
        WHERE a.tenant_id = t.id AND a.code = g.code AND (g.acc_name IS NULL OR a.name = g.acc_name)
          AND NOT EXISTS (SELECT 1 FROM fin_account c WHERE c.tenant_id = t.id AND c.parent_id = a.id)),
       g.sort, 1, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT '废料销售' AS name, 'IN' AS direction, '6051' AS code, NULL AS acc_name, 1 AS sort
    UNION ALL SELECT '佣金收入', 'IN', '6051', NULL, 2
    UNION ALL SELECT '房租收入', 'IN', '6051', NULL, 3
    UNION ALL SELECT '其他收入', 'IN', '6301', NULL, 9
    UNION ALL SELECT '差旅费', 'OUT', '660204', NULL, 1
    UNION ALL SELECT '业务招待费', 'OUT', '660206', '业务招待费', 2
    UNION ALL SELECT '办公费', 'OUT', '660202', NULL, 3
    UNION ALL SELECT '房租', 'OUT', '660208', '租赁费', 4
    UNION ALL SELECT '水电费', 'OUT', '660207', '水电费', 5
    UNION ALL SELECT '快递费', 'OUT', '660209', '快递费', 6
    UNION ALL SELECT '工资', 'OUT', '660201', NULL, 7
    UNION ALL SELECT '银行手续费', 'OUT', '6603', NULL, 8
    UNION ALL SELECT '其他支出', 'OUT', '6711', NULL, 9
) g ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM fin_io_category x WHERE x.tenant_id = t.id AND x.direction = g.direction AND x.name = g.name);

-- 4. 字典类型（ID 已被占用时按自增 ID 插入）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT g.id, g.name, g.type, 'SYSTEM', 1, g.remark, NOW(), NOW()
FROM (
    SELECT 201 AS id, '收支方向' AS name, 'fin_fund_direction' AS type, '收支类别、其他收支单的方向：收入、支出' AS remark
    UNION ALL SELECT 202, '收支单状态', 'fin_io_status', '其他收入单、费用支出单的状态'
    UNION ALL SELECT 203, '关联单位类型', 'fin_io_partner_type', '其他收支单的关联单位：客户、供应商或其他'
    UNION ALL SELECT 204, '手续费承担方', 'fin_fee_bearer', '账户调拨的手续费由转出账户还是转入账户承担'
    UNION ALL SELECT 205, '返利种类', 'scm_rebate_kind', '销售返利（给客户）、采购返利（供应商给我方）'
    UNION ALL SELECT 206, '返利计算方式', 'scm_rebate_calc', '阶梯比例或按项目填写金额'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = g.type OR x.id = g.id);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT g.name, g.type, 'SYSTEM', 1, g.remark, NOW(), NOW()
FROM (
    SELECT '收支方向' AS name, 'fin_fund_direction' AS type, '收支类别、其他收支单的方向：收入、支出' AS remark
    UNION ALL SELECT '收支单状态', 'fin_io_status', '其他收入单、费用支出单的状态'
    UNION ALL SELECT '关联单位类型', 'fin_io_partner_type', '其他收支单的关联单位：客户、供应商或其他'
    UNION ALL SELECT '手续费承担方', 'fin_fee_bearer', '账户调拨的手续费由转出账户还是转入账户承担'
    UNION ALL SELECT '返利种类', 'scm_rebate_kind', '销售返利（给客户）、采购返利（供应商给我方）'
    UNION ALL SELECT '返利计算方式', 'scm_rebate_calc', '阶梯比例或按项目填写金额'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = g.type);

-- 系统字典的取值（平台一份，全部内置）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'fin_fund_direction' AS dict_type, '收入' AS dict_label, 'IN' AS dict_value, 'green' AS tag_color, 1 AS sort
    UNION ALL SELECT 'fin_fund_direction', '支出', 'OUT', 'orange', 2
    UNION ALL SELECT 'fin_io_status', '待确认', 'DRAFT', 'default', 1
    UNION ALL SELECT 'fin_io_status', '已确认', 'CONFIRMED', 'green', 2
    UNION ALL SELECT 'fin_io_partner_type', '客户', 'CUSTOMER', 'blue', 1
    UNION ALL SELECT 'fin_io_partner_type', '供应商', 'SUPPLIER', 'purple', 2
    UNION ALL SELECT 'fin_io_partner_type', '其他', 'OTHER', 'default', 3
    UNION ALL SELECT 'fin_fee_bearer', '转出账户', 'FROM', 'default', 1
    UNION ALL SELECT 'fin_fee_bearer', '转入账户', 'TO', 'default', 2
    UNION ALL SELECT 'fin_voucher_source', '其他收支', 'INCOME_EXPENSE', 'lime', 22
    UNION ALL SELECT 'scm_rebate_kind', '销售返利', 'SALES', 'volcano', 1
    UNION ALL SELECT 'scm_rebate_kind', '采购返利', 'PURCHASE', 'cyan', 2
    UNION ALL SELECT 'scm_rebate_calc', '阶梯比例', 'TIER', 'blue', 1
    UNION ALL SELECT 'scm_rebate_calc', '按项目填写金额', 'ITEM', 'purple', 2
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = 1 AND x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- 应收应付来源：销售返利、采购返利都生成来源为「返利」的红字单据
UPDATE sys_dict_data SET dict_label = '返利' WHERE dict_type = 'fin_bill_source' AND dict_value = 'REBATE' AND dict_label = '销售返利';

-- 5. 菜单：资金与费用 → 其他收支；采购 → 采购返利
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, g.type, g.path, g.component, g.perms, g.icon, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 391 AS id, 933 AS parent_id, '其他收支' AS name, 'C' AS type, 'income-expense' AS path, 'fin/income-expense/index' AS component,
           'fin:io:list' AS perms, 'DollarCircleOutlined' AS icon, 7 AS sort
    UNION ALL SELECT 3910, 391, '收支单新增', 'F', NULL, NULL, 'fin:io:add', NULL, 1
    UNION ALL SELECT 3911, 391, '收支单修改', 'F', NULL, NULL, 'fin:io:edit', NULL, 2
    UNION ALL SELECT 3912, 391, '收支单删除', 'F', NULL, NULL, 'fin:io:remove', NULL, 3
    UNION ALL SELECT 3913, 391, '收支单确认', 'F', NULL, NULL, 'fin:io:confirm', NULL, 4
    UNION ALL SELECT 3914, 391, '收支类别维护', 'F', NULL, NULL, 'fin:io:category', NULL, 5
    UNION ALL SELECT 392, 901, '采购返利', 'C', 'purchase-rebate', 'scm/purchase-rebate/index', 'scm:purchaseRebate:list', 'RedEnvelopeOutlined', 7
    UNION ALL SELECT 3920, 392, '采购返利协议维护', 'F', NULL, NULL, 'scm:purchaseRebate:add', NULL, 1
    UNION ALL SELECT 3921, 392, '采购返利结算', 'F', NULL, NULL, 'scm:purchaseRebate:settle', NULL, 2
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = g.id);

-- 套餐：包含出纳管理的套餐加入其他收支，包含采购订单的套餐加入采购返利（含按钮）
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 341) p
JOIN sys_menu m ON m.id IN (391, 3910, 3911, 3912, 3913, 3914)
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = m.id);
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 140) p
JOIN sys_menu m ON m.id IN (392, 3920, 3921)
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = m.id);

-- 其他租户的管理员（菜单与套餐一致）：有出纳管理的加其他收支，有采购订单的加采购返利
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (391, 3910, 3911, 3912, 3913, 3914)
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 341)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (392, 3920, 3921)
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 140)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- 内置角色（各租户按角色编码）：其他收支 普通用户、总经理只读，会计、出纳全部；
-- 采购返利 普通用户查看、采购员维护、总经理和会计结算（会计同时补上供应链模块和采购分组）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 391 AS menu_id UNION ALL SELECT 'gm', 391
      UNION ALL SELECT 'accountant', 391 UNION ALL SELECT 'accountant', 3910 UNION ALL SELECT 'accountant', 3911
      UNION ALL SELECT 'accountant', 3912 UNION ALL SELECT 'accountant', 3913 UNION ALL SELECT 'accountant', 3914
      UNION ALL SELECT 'cashier', 391 UNION ALL SELECT 'cashier', 3910 UNION ALL SELECT 'cashier', 3911
      UNION ALL SELECT 'cashier', 3912 UNION ALL SELECT 'cashier', 3913 UNION ALL SELECT 'cashier', 3914
      UNION ALL SELECT 'common', 392 UNION ALL SELECT 'gm', 392 UNION ALL SELECT 'gm', 3921
      UNION ALL SELECT 'purchaser', 392 UNION ALL SELECT 'purchaser', 3920
      UNION ALL SELECT 'accountant', 5 UNION ALL SELECT 'accountant', 901 UNION ALL SELECT 'accountant', 392
      UNION ALL SELECT 'accountant', 3921) g ON g.code = r.code
WHERE (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = g.menu_id))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);
