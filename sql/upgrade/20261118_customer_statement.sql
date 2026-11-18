-- ====================================================
-- 客户对账单（issue #134）。可重复执行，与其他脚本没有依赖
-- 新表 fin_customer_statement / fin_customer_statement_line（按客户 + 期间生成的往来对账快照，发送后客户在经销商门户确认或提出异议）；
-- 菜单：应收应付下「客户对账」（425，fin:customerStatement:list，按钮 4250–4252 生成 / 发送及登记结果 / 删除），
-- 排在供应商对账之后（往来对账单、冲账单顺延）；经销商门户「对账单」（426，dealer:statement:list，按钮 4260 对账确认 dealer:statement:confirm）。
-- 套餐：包含「供应商对账」（299）的加入客户对账，包含「我要订货」（320）的加入经销商门户对账单；
-- 内置角色：普通用户、总经理、出纳查看，会计全部，经销商门户账号查看和确认；其他租户的管理员按套餐加入。
-- 字典 fin_statement_status 的说明改为「供应商 / 客户对账单的状态」。升级后清空「用户权限」缓存（login:perms:*）
-- ====================================================

CREATE TABLE IF NOT EXISTS fin_customer_statement (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    statement_no    VARCHAR(32)    NOT NULL COMMENT '对账单号',
    customer_id     BIGINT         NOT NULL COMMENT '客户',
    begin_date      DATE           NOT NULL COMMENT '对账期间起',
    end_date        DATE           NOT NULL COMMENT '对账期间止',
    opening         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初余额',
    bill_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期应收（应收单价税合计，退货、冲账为负）',
    receipt_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期收款（收款实收 + 预收款）',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期折让（收款核销时的折让 / 抹零）',
    netting_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期应收应付对冲',
    closing         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期末余额 = 期初 + 应收 - 收款 - 折让 - 对冲',
    line_count      INT            NOT NULL DEFAULT 0 COMMENT '明细行数',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_statement_status) DRAFT草稿 SENT待确认 CONFIRMED已确认 DISPUTED有异议',
    customer_remark VARCHAR(500)            DEFAULT NULL COMMENT '客户意见',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '客户确认 / 提出异议的时间',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认 / 提出异议的门户账号',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_statement_no (statement_no, tenant_id),
    KEY idx_customer_statement_customer (customer_id, end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户对账单';

CREATE TABLE IF NOT EXISTS fin_customer_statement_line (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    statement_id    BIGINT         NOT NULL COMMENT '对账单',
    line_no         INT            NOT NULL COMMENT '行号（按日期排序）',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    type            VARCHAR(16)    NOT NULL COMMENT '类型 BILL应收单 SETTLEMENT收款 ADVANCE预收款 NETTING应收应付对冲',
    doc_no          VARCHAR(32)    NOT NULL COMMENT '单号',
    source_type     VARCHAR(32)             DEFAULT NULL COMMENT '应收单的来源(字典 fin_bill_source) / 收款的结算方式(字典 fin_settle_method)',
    source_no       VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    increase_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '增加（应收）',
    decrease_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '减少（收款含折让、预收、对冲）',
    discount_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '其中折让',
    balance         DECIMAL(18, 2) NOT NULL COMMENT '余额',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_customer_statement_line (statement_id, line_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户对账单明细';

UPDATE sys_dict_type SET remark = '供应商 / 客户对账单的状态' WHERE type = 'fin_statement_status' AND remark = '供应商对账单的状态';

-- 应收应付：客户对账排在供应商对账（7）之后，往来对账单、冲账单顺延
UPDATE sys_menu SET sort = 9 WHERE id = 348 AND perms = 'fin:partnerStatement:list';
UPDATE sys_menu SET sort = 10 WHERE id = 377 AND perms = 'fin:offset:list';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, g.type, g.path, g.component, g.perms, g.icon, g.sort, 1, 1, NOW(), NOW()
FROM (SELECT 425 AS id, 932 AS parent_id, '客户对账' AS name, 'C' AS type, 'customer-statement' AS path,
             'fin/customer-statement/index' AS component, 'fin:customerStatement:list' AS perms, 'SolutionOutlined' AS icon, 8 AS sort
      UNION ALL SELECT 4250, 425, '客户对账单生成', 'F', NULL, NULL, 'fin:customerStatement:add', NULL, 1
      UNION ALL SELECT 4251, 425, '客户对账单发送', 'F', NULL, NULL, 'fin:customerStatement:edit', NULL, 2
      UNION ALL SELECT 4252, 425, '客户对账单删除', 'F', NULL, NULL, 'fin:customerStatement:remove', NULL, 3
      UNION ALL SELECT 426, 14, '对账单', 'C', 'statement', 'dealer/statement/index', 'dealer:statement:list', 'ReconciliationOutlined', 3
      UNION ALL SELECT 4260, 426, '对账确认', 'F', NULL, NULL, 'dealer:statement:confirm', NULL, 1) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = g.id);

-- 套餐：包含供应商对账（299）的加入客户对账，包含经销商门户「我要订货」（320）的加入门户对账单
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 299) p
JOIN (SELECT 425 AS menu_id UNION ALL SELECT 4250 UNION ALL SELECT 4251 UNION ALL SELECT 4252) g ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 320) p
JOIN (SELECT 426 AS menu_id UNION ALL SELECT 4260) g ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

-- 内置角色（按角色编码）：普通用户、总经理、出纳查看，会计全部（连同应收应付分组 932 和财务模块 7）；
-- 平台租户、没有套餐（全部功能）的租户，或套餐包含该菜单的租户
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 7 AS menu_id UNION ALL SELECT 'common', 932 UNION ALL SELECT 'common', 425
      UNION ALL SELECT 'gm', 7 UNION ALL SELECT 'gm', 932 UNION ALL SELECT 'gm', 425
      UNION ALL SELECT 'cashier', 7 UNION ALL SELECT 'cashier', 932 UNION ALL SELECT 'cashier', 425
      UNION ALL SELECT 'accountant', 7 UNION ALL SELECT 'accountant', 932 UNION ALL SELECT 'accountant', 425
      UNION ALL SELECT 'accountant', 4250 UNION ALL SELECT 'accountant', 4251 UNION ALL SELECT 'accountant', 4252) g ON g.code = r.code
WHERE (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = 425))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 经销商门户账号的内置角色（连同经销商门户模块 14）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 14 AS menu_id UNION ALL SELECT 426 UNION ALL SELECT 4260) g ON 1 = 1
WHERE r.code = 'dealer'
  AND (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = 426))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 其他租户的管理员（菜单与套餐一致）：有供应商对账的加入客户对账，有经销商门户订货的加入门户对账单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 299 AS has_menu, 425 AS menu_id UNION ALL SELECT 299, 4250 UNION ALL SELECT 299, 4251 UNION ALL SELECT 299, 4252
      UNION ALL SELECT 320, 426 UNION ALL SELECT 320, 4260) g ON 1 = 1
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = g.has_menu)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);
