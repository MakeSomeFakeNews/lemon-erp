-- ====================================================
-- 进销存单据草稿与审核（issue #68）。可重复执行
-- 1. 新表 scm_trade_draft / scm_trade_draft_item：采购入库 / 退货、销售出库 / 退货的草稿（不影响库存、订单、应收应付和凭证，审核后生成出入库单并删除）
-- 2. scm_trade_doc.post_by / post_time：审核人、审核时间。已有单据都是保存即审核，取制单人、创建时间
-- 3. 菜单：采购入库审核（scm:purchaseIn:post）、采购退货审核、销售出库审核、销售退货审核。包含对应页面的套餐随之加入；
--    已有新增权限的角色（各租户的采购员、仓库管理员、管理员及自定义角色）同时授权，开启草稿前后操作不受影响；能查看这些页面的会计角色也授权（审核记账）
-- 是否启用由业务参数「进销存单据先存草稿」（scm.tradeDoc.draft，默认关闭）控制，不需要改库
-- ====================================================

CREATE TABLE IF NOT EXISTS scm_trade_draft (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号（审核后沿用为出入库单号）',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 scm_trade_kind) PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 SALES_OUT销售出库 SALES_RETURN销售退货',
    order_id     BIGINT                  DEFAULT NULL COMMENT '引用的订单，直接开单为空',
    source_doc_id BIGINT                 DEFAULT NULL COMMENT '退货引用的原出入库单（仅用于表单回显）',
    partner_id   BIGINT         NOT NULL COMMENT '供应商 / 客户',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    salesman_id  BIGINT                  DEFAULT NULL COMMENT '直接开单的销售出库：业务员',
    replenish    TINYINT        NOT NULL DEFAULT 0 COMMENT '退货是否补发（补收） 1补发 0退款',
    whole_discount DECIMAL(18, 2)        DEFAULT NULL COMMENT '直接开单的整单优惠（含税）',
    order_discount DECIMAL(18, 2)        DEFAULT NULL COMMENT '引用订单的采购入库：本单的订单优惠（含税，订单币种），为空按订单分摊',
    freight      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '采购运费（仅采购入库）',
    freight_bearer VARCHAR(16)           DEFAULT NULL COMMENT '采购运费付给(字典 scm_freight_bearer) SUPPLIER供应商代垫 COMPANY本公司承担（付承运商）',
    carrier_supplier_id BIGINT           DEFAULT NULL COMMENT '采购运费的承运商（供应商）',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计（按录入单价估算，审核后以出入库单为准）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_draft_no (doc_no, tenant_id),
    KEY idx_trade_draft_kind (kind, id),
    KEY idx_trade_draft_order (order_id),
    KEY idx_trade_draft_partner (partner_id),
    KEY idx_trade_draft_warehouse (warehouse_id),
    KEY idx_trade_draft_dept (dept_id),
    KEY idx_trade_draft_create_by (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货的草稿（不影响库存、往来和凭证，审核后生成出入库单并删除）';

CREATE TABLE IF NOT EXISTS scm_trade_draft_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    draft_id        BIGINT         NOT NULL COMMENT '草稿ID',
    order_item_id   BIGINT                  DEFAULT NULL COMMENT '订单明细ID，直接开单为空',
    material_id     BIGINT                  DEFAULT NULL COMMENT '物料（引用订单时为订单明细的物料）',
    warehouse_id    BIGINT                  DEFAULT NULL COMMENT '本行仓库（仅销售出库可选），为空取表头仓库',
    location_id     BIGINT                  DEFAULT NULL COMMENT '库位',
    batch_no        VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅采购入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅采购入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price           DECIMAL(18, 4)          DEFAULT NULL COMMENT '直接开单：含税单价',
    tax_rate        DECIMAL(5, 2)           DEFAULT NULL COMMENT '直接开单：税率（%）',
    gift            TINYINT        NOT NULL DEFAULT 0 COMMENT '直接开单的销售出库：1 买赠的赠品行',
    unit_name       VARCHAR(16)             DEFAULT NULL COMMENT '直接开单：录入时使用的单位（仅显示）',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    sort            INT            NOT NULL DEFAULT 0 COMMENT '行号',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trade_draft_item_draft (draft_id),
    KEY idx_trade_draft_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货草稿的明细';

-- 2. 审核人、审核时间
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

CALL lemon_add_column('scm_trade_doc', 'post_by', 'post_by BIGINT DEFAULT NULL COMMENT ''审核人（保存即审核时为制单人，草稿审核时为审核人）'' AFTER create_by');
CALL lemon_add_column('scm_trade_doc', 'post_time', 'post_time DATETIME DEFAULT NULL COMMENT ''审核时间（出入库、生成应收应付的时间）'' AFTER post_by');

DROP PROCEDURE IF EXISTS lemon_add_column;

-- 已有单据都是保存即审核：审核人、审核时间取制单人、创建时间
UPDATE scm_trade_doc SET post_by = create_by, post_time = create_time WHERE post_time IS NULL;

-- 3. 菜单：审核按钮
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, 'F', NULL, NULL, g.perms, NULL, 2, 1, 1, NOW(), NOW()
FROM (
    SELECT 1411 AS id, 141 AS parent_id, '采购入库审核' AS name, 'scm:purchaseIn:post' AS perms
    UNION ALL SELECT 1421, 142, '采购退货审核', 'scm:purchaseReturn:post'
    UNION ALL SELECT 1441, 144, '销售出库审核', 'scm:salesOut:post'
    UNION ALL SELECT 1451, 145, '销售退货审核', 'scm:salesReturn:post'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = g.id);

-- 套餐：包含对应页面的套餐加入审核按钮
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM sys_tenant_package_menu p
JOIN (SELECT 141 AS page_id, 1411 AS menu_id UNION ALL SELECT 142, 1421 UNION ALL SELECT 144, 1441 UNION ALL SELECT 145, 1451) g ON g.page_id = p.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

-- 角色：有新增按钮的角色同时授权审核按钮（1410 → 1411，1420 → 1421，1440 → 1441，1450 → 1451）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, g.menu_id
FROM sys_role_menu rm
JOIN (SELECT 1410 AS add_id, 1411 AS menu_id UNION ALL SELECT 1420, 1421 UNION ALL SELECT 1440, 1441 UNION ALL SELECT 1450, 1451) g ON g.add_id = rm.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = g.menu_id);

-- 角色：各租户能查看这些页面的会计（角色编码 accountant）授权审核按钮
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, g.menu_id
FROM sys_role_menu rm
JOIN sys_role r ON r.id = rm.role_id AND r.code = 'accountant'
JOIN (SELECT 141 AS page_id, 1411 AS menu_id UNION ALL SELECT 142, 1421 UNION ALL SELECT 144, 1441 UNION ALL SELECT 145, 1451) g ON g.page_id = rm.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = g.menu_id);
