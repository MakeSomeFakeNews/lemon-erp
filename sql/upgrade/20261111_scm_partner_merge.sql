-- ====================================================
-- 往来单位合并与关联（issue #95）。可重复执行。scm_supplier.customer_id 加在 routes 之后，routes 由 20261111_purchase_discount_freight.sql 新增，
-- 文件名排在它之后（按文件名顺序执行即可）；其余只新建两张表、增加菜单和字典，与其他脚本没有依赖
-- 1. 供应商的关联客户 scm_supplier.customer_id（同一单位既是供应商又是客户，一对一，唯一约束 uk_supplier_customer），
--    已合并标记 scm_supplier.merged_to / scm_customer.merged_to（资料合并后停用，记录合并到的资料）；已有数据均为空
-- 2. 新表 fin_netting / fin_netting_item：应收应付对冲单及核销明细（凭证 借 应付账款 / 贷 应收账款）
-- 3. 菜单：「客户合并」（scm:customer:merge）、「供应商合并」（scm:supplier:merge）、「应收应付对冲」（fin:netting:add）、
--    「删除对冲单」（fin:netting:remove）；包含对应页面的套餐加入。客户合并授权给有客户删除权限的角色（销售员 sales 除外），
--    供应商合并授权给有供应商删除权限的角色，对冲授权给能查看应收管理的会计（accountant）和有应收单删除权限的角色（租户管理员等）
-- 4. 系统字典「凭证来源」增加「应收应付对冲」NETTING
-- 升级后在「缓存监控」中清空「用户权限」「字典数据」缓存
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

-- 1. 关联客户、已合并标记
CALL lemon_add_column('scm_supplier', 'customer_id',
    'customer_id BIGINT DEFAULT NULL COMMENT ''关联客户（同一单位既是供应商又是客户，一对一）：查询应收时可挂接该供应商的应付，可做应收应付对冲'' AFTER routes');
CALL lemon_add_column('scm_supplier', 'merged_to',
    'merged_to BIGINT DEFAULT NULL COMMENT ''已合并到的供应商（资料合并后本资料停用，业务数据已改为指向该供应商）'' AFTER customer_id');
CALL lemon_add_index('scm_supplier', 'uk_supplier_customer', 'UNIQUE KEY uk_supplier_customer (customer_id, tenant_id)');
CALL lemon_add_column('scm_customer', 'merged_to',
    'merged_to BIGINT DEFAULT NULL COMMENT ''已合并到的客户（资料合并后本资料停用，业务数据已改为指向该客户）'' AFTER owner_id');

DROP PROCEDURE IF EXISTS lemon_add_column;
DROP PROCEDURE IF EXISTS lemon_add_index;

-- 2. 应收应付对冲
CREATE TABLE IF NOT EXISTS fin_netting (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    netting_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    customer_id  BIGINT         NOT NULL COMMENT '客户',
    supplier_id  BIGINT         NOT NULL COMMENT '供应商（关联该客户的供应商）',
    netting_date DATE           NOT NULL COMMENT '对冲日期',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '对冲金额（正数）= 核销的应收合计 = 核销的应付合计（本位币）',
    voucher_id   BIGINT                  DEFAULT NULL COMMENT '凭证：借 应付账款（供应商）/ 贷 应收账款（客户）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_netting_no (netting_no, tenant_id),
    KEY idx_netting_customer (customer_id),
    KEY idx_netting_supplier (supplier_id),
    KEY idx_netting_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收应付对冲：关联的客户与供应商之间用应付抵减应收（不收付现金）';

CREATE TABLE IF NOT EXISTS fin_netting_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    netting_id  BIGINT         NOT NULL COMMENT '对冲单',
    bill_id     BIGINT         NOT NULL COMMENT '核销的应收 / 应付单',
    kind        VARCHAR(8)     NOT NULL COMMENT '种类 AR应收 AP应付',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '核销金额（与单据金额同号，红字单据为负数）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_netting_item_netting (netting_id),
    KEY idx_netting_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收应付对冲的核销明细';

-- 3. 菜单（按钮）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, 'F', NULL, NULL, g.perms, NULL, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 1474 AS id, 147 AS parent_id, '供应商合并' AS name, 'scm:supplier:merge' AS perms, 5 AS sort
    UNION ALL SELECT 1494, 149, '客户合并', 'scm:customer:merge', 5
    UNION ALL SELECT 1835, 183, '应收应付对冲', 'fin:netting:add', 10
    UNION ALL SELECT 1836, 183, '删除对冲单', 'fin:netting:remove', 11
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = g.id);

-- 套餐：包含对应页面的套餐加入这些按钮
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM sys_tenant_package_menu p
JOIN (SELECT 147 AS page_id, 1474 AS menu_id UNION ALL SELECT 149, 1494 UNION ALL SELECT 183, 1835 UNION ALL SELECT 183, 1836) g
  ON g.page_id = p.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

-- 角色：有删除权限的角色授权合并（客户删除 1492 → 1494，销售员除外；供应商删除 1472 → 1474），
-- 有应收单删除权限（1831）的角色授权对冲的新增、删除
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, g.menu_id
FROM sys_role_menu rm
JOIN sys_role r ON r.id = rm.role_id
JOIN (SELECT 1492 AS from_id, 1494 AS menu_id UNION ALL SELECT 1472, 1474
      UNION ALL SELECT 1831, 1835 UNION ALL SELECT 1831, 1836) g ON g.from_id = rm.menu_id
WHERE NOT (g.menu_id = 1494 AND r.code = 'sales')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = g.menu_id);

-- 角色：各租户能查看应收管理的会计（accountant）授权对冲的新增、删除
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, g.menu_id
FROM sys_role_menu rm
JOIN sys_role r ON r.id = rm.role_id AND r.code = 'accountant'
JOIN (SELECT 1835 AS menu_id UNION ALL SELECT 1836) g ON rm.menu_id = 183
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = g.menu_id);

-- 4. 凭证来源（系统字典）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 'fin_voucher_source', '应收应付对冲', 'NETTING', 'volcano', 23, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = 1 AND x.dict_type = 'fin_voucher_source' AND x.dict_value = 'NETTING');
