-- ====================================================
-- 应收应付（issue #30）：收付款自动核销、核销折让 / 抹零、多收转预收预付、冲账单、应付坏账。可重复执行
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

-- 核销折让：单据、收付款单、核销明细记录折让金额（计入已核销）；多收 / 多付转预收预付
CALL lemon_add_column('fin_bill', 'discount_amount', 'discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''收付款时的折让 / 抹零金额（计入已核销）'' AFTER bad_debt_amount');
CALL lemon_add_column('fin_settlement', 'discount_amount', 'discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''折让 / 抹零合计（不含在金额中，单据的已核销 = 金额 + 折让）'' AFTER amount_fc');
CALL lemon_add_column('fin_settlement', 'advance_amount', 'advance_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''多收 / 多付转预收预付的金额（不含在金额中）'' AFTER discount_amount');
CALL lemon_add_column('fin_settlement', 'advance_id', 'advance_id BIGINT DEFAULT NULL COMMENT ''多收 / 多付生成的预收预付单'' AFTER advance_amount');
CALL lemon_add_column('fin_settlement_item', 'discount', 'discount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''折让 / 抹零金额（与单据同号），单据的已核销 = 核销金额 + 折让'' AFTER amount_fc');
DROP PROCEDURE IF EXISTS lemon_add_column;

-- 冲账单
CREATE TABLE IF NOT EXISTS fin_offset (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    offset_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind        VARCHAR(8)     NOT NULL COMMENT '方向 AR应收冲账（客户） AP应付冲账（供应商）',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    offset_date DATE           NOT NULL COMMENT '冲账日期',
    handler_id  BIGINT                  DEFAULT NULL COMMENT '经办人',
    item        VARCHAR(32)    NOT NULL COMMENT '冲账项目(业务字典 fin_offset_item)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '冲账金额（正数），复核后生成金额为其负数的红字应收 / 应付单',
    begin_date  DATE                    DEFAULT NULL COMMENT '冲账项目期间：开始日期',
    end_date    DATE                    DEFAULT NULL COMMENT '冲账项目期间：结束日期',
    ticket_no   VARCHAR(64)             DEFAULT NULL COMMENT '票据编号',
    account_id  BIGINT                  DEFAULT NULL COMMENT '凭证对方科目，为空取凭证科目设置（应收冲账 / 应付冲账）',
    status      VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_offset_status) DRAFT待复核 REVIEWED已复核',
    bill_id     BIGINT                  DEFAULT NULL COMMENT '复核生成的红字应收 / 应付单',
    review_by   BIGINT                  DEFAULT NULL COMMENT '复核人',
    review_time DATETIME                DEFAULT NULL COMMENT '复核时间',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_offset_no (offset_no, tenant_id),
    KEY idx_offset_partner (kind, partner_id),
    KEY idx_offset_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '冲账单（垫付运费、返利、三包赔偿等抵扣往来款）';

-- 字典：冲账项目（业务字典，平台一份 + 已有租户各复制一份）、冲账单状态（系统字典）、应收应付来源增加冲账单
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT d.* FROM (
    SELECT 185 AS id, '冲账项目' AS name, 'fin_offset_item' AS type, 'BUSINESS' AS kind, 1 AS status, '冲账单的项目，如垫付运费、销售返利、三包赔偿' AS remark, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 186, '冲账单状态', 'fin_offset_status', 'SYSTEM', 1, '冲账单的复核状态', NOW(), NOW()
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = d.type);

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, 1, d.builtin, NOW(), NOW() FROM (
    SELECT 'fin_offset_item' AS dict_type, '垫付运费' AS dict_label, 'FREIGHT' AS dict_value, 'cyan' AS tag_color, 1 AS sort, 0 AS builtin
    UNION ALL SELECT 'fin_offset_item', '销售返利', 'REBATE', 'volcano', 2, 0
    UNION ALL SELECT 'fin_offset_item', '三包赔偿', 'WARRANTY', 'orange', 3, 0
    UNION ALL SELECT 'fin_offset_item', '其他', 'OTHER', 'default', 9, 1
    UNION ALL SELECT 'fin_offset_status', '待复核', 'DRAFT', 'default', 1, 1
    UNION ALL SELECT 'fin_offset_status', '已复核', 'REVIEWED', 'green', 2, 1
    UNION ALL SELECT 'fin_bill_source', '冲账单', 'OFFSET', 'magenta', 13, 1
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = d.dict_type AND x.dict_value = d.dict_value AND x.tenant_id = 1);

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, remark, create_time, update_time)
SELECT t.id, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, d.status, d.builtin, d.remark, NOW(), NOW()
FROM sys_tenant t
JOIN sys_dict_data d ON d.tenant_id = 1 AND d.dict_type = 'fin_offset_item'
WHERE t.id <> 1
  AND NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = t.id AND x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- 菜单：财务 → 应收应付 → 冲账单；应付管理增加「应付坏账」按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT m.* FROM (
    SELECT 377 AS id, 932 AS parent_id, '冲账单' AS name, 'C' AS type, 'offset' AS path, 'fin/offset/index' AS component, 'fin:offset:list' AS perms, 'InteractionOutlined' AS icon, 9 AS sort, 1 AS visible, 1 AS status, NOW() AS create_time, NOW() AS update_time
    UNION ALL SELECT 3770, 377, '冲账单新增', 'F', NULL, NULL, 'fin:offset:add', NULL, 1, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3771, 377, '冲账单修改', 'F', NULL, NULL, 'fin:offset:edit', NULL, 2, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3772, 377, '冲账单删除', 'F', NULL, NULL, 'fin:offset:remove', NULL, 3, 1, 1, NOW(), NOW()
    UNION ALL SELECT 3773, 377, '冲账单复核', 'F', NULL, NULL, 'fin:offset:review', NULL, 4, 1, 1, NOW(), NOW()
    UNION ALL SELECT 1843, 184, '应付坏账', 'F', NULL, NULL, 'fin:ap:baddebt', NULL, 9, 1, 1, NOW(), NOW()
) m
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = m.id);

-- 套餐：包含收款单的套餐加入冲账单，包含应付管理的套餐加入应付坏账
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 185) p
JOIN sys_menu m ON m.id IN (377, 3770, 3771, 3772, 3773)
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = m.id);
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, 1843
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 184) p
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 1843);

-- 内置角色（各租户按角色编码）：普通用户、总经理、出纳只读，会计全部；有应收应付分组的角色才授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 377 AS menu_id UNION ALL SELECT 'gm', 377 UNION ALL SELECT 'cashier', 377
      UNION ALL SELECT 'accountant', 377 UNION ALL SELECT 'accountant', 3770 UNION ALL SELECT 'accountant', 3771
      UNION ALL SELECT 'accountant', 3772 UNION ALL SELECT 'accountant', 3773 UNION ALL SELECT 'accountant', 1843) g ON g.code = r.code
WHERE EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 932)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 其他租户的管理员（菜单与套餐一致）：有收款单的加冲账单，有应收坏账的加应付坏账
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (377, 3770, 3771, 3772, 3773)
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 185)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 1843
FROM sys_role r
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 1834)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 1843);

-- ====================================================
-- 外币核销折让、外币预收预付（多收转预收支持外币）
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

CALL lemon_add_column('fin_settlement', 'discount_fc', 'discount_fc DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''折让合计（原币）'' AFTER discount_amount');
CALL lemon_add_column('fin_settlement_item', 'discount_fc', 'discount_fc DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''折让原币金额（外币按单据记账汇率折算为折让）'' AFTER discount');
CALL lemon_add_column('fin_advance', 'currency', 'currency VARCHAR(8) NOT NULL DEFAULT ''CNY'' COMMENT ''币种'' AFTER used_amount');
CALL lemon_add_column('fin_advance', 'exchange_rate', 'exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT ''汇率；金额 = 原币金额 × 汇率'' AFTER currency');
CALL lemon_add_column('fin_advance', 'amount_fc', 'amount_fc DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''原币金额'' AFTER exchange_rate');
CALL lemon_add_column('fin_advance', 'used_fc', 'used_fc DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''原币已冲抵'' AFTER amount_fc');
CALL lemon_add_column('fin_advance_apply', 'amount_fc', 'amount_fc DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''冲抵原币金额'' AFTER amount');
DROP PROCEDURE IF EXISTS lemon_add_column;

-- 已有数据都是本位币：原币 = 本位币（只更新还没有原币的行，可重复执行）
UPDATE fin_settlement SET discount_fc = discount_amount WHERE discount_fc = 0 AND discount_amount <> 0 AND currency = 'CNY';
UPDATE fin_settlement_item i JOIN fin_settlement s ON s.id = i.settlement_id
SET i.discount_fc = i.discount WHERE i.discount_fc = 0 AND i.discount <> 0 AND s.currency = 'CNY';
UPDATE fin_advance SET amount_fc = amount, used_fc = used_amount
WHERE currency = 'CNY' AND (amount_fc <> amount OR used_fc <> used_amount);
UPDATE fin_advance_apply p JOIN fin_advance a ON a.id = p.advance_id
SET p.amount_fc = p.amount WHERE p.amount_fc = 0 AND p.amount <> 0 AND a.currency = 'CNY';

-- 预收预付冲抵记录单据一方的本位币（与单据核销的已核销金额一致，外币单据与预收预付一方可能不同），资产状况按截至日倒推应收应付时使用。
-- 历史数据：本位币单据 = 冲抵金额，外币单据 = 冲抵原币 × 单据记账汇率（只回填还没有值的行，可重复执行）
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
CALL lemon_add_column('fin_advance_apply', 'bill_amount', 'bill_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''单据一方的本位币核销金额（计入单据的已核销）'' AFTER amount_fc');
DROP PROCEDURE IF EXISTS lemon_add_column;

UPDATE fin_advance_apply p JOIN fin_bill b ON b.id = p.bill_id
SET p.bill_amount = CASE WHEN b.currency = 'CNY' THEN p.amount ELSE ROUND(p.amount_fc * b.exchange_rate, 2) END
WHERE p.bill_amount = 0 AND p.amount <> 0;
