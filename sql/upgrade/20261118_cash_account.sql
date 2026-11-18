-- ====================================================
-- 资金账户（issue #136）。可重复执行，与其他脚本没有依赖
-- 1. 新表 fin_cash_account（资金账户：名称、类型、入账科目、开户行、账号、启用日期、日记账期初、对账单期初、状态）、
--    fin_bank_opening_item（银行类账户的期初未达账）
-- 2. 会计科目补充 1012 其他货币资金（各租户没有时），以及其下级 101201 微信、101202 支付宝（1012 还没有下级、没有分录和期初时）
-- 3. 回填资金账户：各租户「指定科目」（凭证科目设置的库存现金、银行存款、其他货币资金，未设置时为 1001、1002、1012）下的
--    每个末级科目建一个账户（名称取科目名称，重名的加科目编码；类型按所在科目：现金 / 银行，其他货币资金下的微信、支付宝按名称识别，
--    其余为其他；状态同科目；启用日期为空，日记账期初仍取科目期初余额），原有单据、凭证、日记账不受影响
-- 4. 系统字典「资金账户类型」fin_cash_account_type、「期初未达账类型」fin_bank_opening_side
-- 5. 菜单「资金账户」（财务 → 资金与费用，fin:cashAccount:list/add/edit/remove），包含「出纳管理」的套餐加入；
--    内置角色出纳、会计可维护，总经理、普通用户只读；其他租户有出纳管理的管理员也加入
-- 升级后清空「用户权限」「字典数据」缓存（login:perms:*、dict:*）
-- ====================================================

-- ---------------------------------------------------- 1. 新表

CREATE TABLE IF NOT EXISTS fin_cash_account (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '账户名称，如 招行基本户、微信零钱（单据的账户下拉显示）',
    type              VARCHAR(16)    NOT NULL COMMENT '账户类型(字典 fin_cash_account_type) CASH现金 BANK银行 WECHAT微信 ALIPAY支付宝 OTHER其他',
    account_id        BIGINT         NOT NULL COMMENT '入账科目：指定的现金 / 银行 / 其他货币资金科目下的末级科目，不启用辅助核算；单据中保存的是该科目 ID',
    bank_name         VARCHAR(64)             DEFAULT NULL COMMENT '开户行',
    bank_account_no   VARCHAR(64)             DEFAULT NULL COMMENT '账号',
    enable_date       DATE                    DEFAULT NULL COMMENT '启用日期：日记账从该日起按日记账期初计算；为空表示从建账开始（期初取科目期初余额）',
    journal_opening   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '日记账期初余额（启用日期前一天的余额）',
    statement_opening DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '银行对账单期初余额（非现金账户）',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用：停用的不出现在单据的账户下拉中；停用后才能修改类型、入账科目、启用日期和期初',
    sort              INT            NOT NULL DEFAULT 0 COMMENT '排序',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cash_account_subject (account_id, tenant_id),
    UNIQUE KEY uk_cash_account_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资金账户（出纳账户）：一个入账科目对应一个账户';

CREATE TABLE IF NOT EXISTS fin_bank_opening_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    cash_account_id BIGINT         NOT NULL COMMENT '资金账户（非现金）',
    side            VARCHAR(8)     NOT NULL COMMENT '未达账类型(字典 fin_bank_opening_side) BOOK企业已记银行未记 BANK银行已记企业未记',
    txn_date        DATE           NOT NULL COMMENT '日期（早于账户的启用日期）',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '金额：收入为正、支出为负',
    summary         VARCHAR(255)            DEFAULT NULL COMMENT '摘要',
    ref_no          VARCHAR(64)             DEFAULT NULL COMMENT '流水号 / 票据号',
    entry_id        BIGINT                  DEFAULT NULL COMMENT '银行已记企业未记：企业入账后勾对的凭证分录',
    statement_id    BIGINT                  DEFAULT NULL COMMENT '企业已记银行未记：银行入账后勾对的对账单流水',
    create_by       BIGINT                  DEFAULT NULL COMMENT '录入人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bank_opening_account (cash_account_id),
    KEY idx_bank_opening_entry (entry_id),
    KEY idx_bank_opening_statement (statement_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '银行类资金账户的期初未达账';

-- ---------------------------------------------------- 2. 科目：1012 其他货币资金及微信、支付宝

INSERT INTO fin_account (tenant_id, code, name, parent_id, category, direction, status, create_time, update_time)
SELECT t.tenant_id, '1012', '其他货币资金', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()
FROM (SELECT DISTINCT tenant_id FROM fin_account) t
WHERE NOT EXISTS (SELECT 1 FROM fin_account x WHERE x.tenant_id = t.tenant_id AND x.code = '1012');

DROP TABLE IF EXISTS tmp_fund_parent;
CREATE TABLE tmp_fund_parent AS
SELECT p.tenant_id, p.id
FROM fin_account p
WHERE p.code = '1012'
  AND NOT EXISTS (SELECT 1 FROM fin_account c WHERE c.parent_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM fin_voucher_entry e WHERE e.account_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM fin_account_opening o WHERE o.account_id = p.id);

INSERT INTO fin_account (tenant_id, code, name, parent_id, category, direction, status, create_time, update_time)
SELECT p.tenant_id, g.code, g.name, p.id, 'ASSET', 'DEBIT', 1, NOW(), NOW()
FROM tmp_fund_parent p
JOIN (SELECT '101201' AS code, '微信' AS name UNION ALL SELECT '101202', '支付宝') g ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM fin_account x WHERE x.tenant_id = p.tenant_id AND x.code = g.code);

DROP TABLE IF EXISTS tmp_fund_parent;

-- ---------------------------------------------------- 3. 回填资金账户

-- 工作表用普通表（MySQL 同一语句中不能多次引用临时表），用完删除
-- 各租户的指定科目编码（凭证科目设置的默认科目，未设置时取预置编码）
DROP TABLE IF EXISTS tmp_fund_scope;
CREATE TABLE tmp_fund_scope AS
SELECT t.tenant_id, s.role, COALESCE(
           (SELECT a.code FROM fin_account_rule r JOIN fin_account a ON a.id = r.account_id
            WHERE r.tenant_id = t.tenant_id AND r.role = s.role AND r.dim_value = ''), s.code) AS code
FROM (SELECT DISTINCT tenant_id FROM fin_account) t
JOIN (SELECT 'CASH' AS role, '1001' AS code UNION ALL SELECT 'BANK', '1002' UNION ALL SELECT 'OTHER_FUNDS', '1012') s ON 1 = 1;

-- 指定科目下还没有资金账户的末级科目（同一科目只取一个用途：现金优先、其次银行）
DROP TABLE IF EXISTS tmp_fund_subject;
CREATE TABLE tmp_fund_subject AS
SELECT a.tenant_id, a.id AS account_id, a.code, a.name, a.status,
       CASE
           WHEN EXISTS (SELECT 1 FROM tmp_fund_scope s WHERE s.tenant_id = a.tenant_id AND s.role = 'CASH' AND a.code LIKE CONCAT(s.code, '%')) THEN 'CASH'
           WHEN EXISTS (SELECT 1 FROM tmp_fund_scope s WHERE s.tenant_id = a.tenant_id AND s.role = 'BANK' AND a.code LIKE CONCAT(s.code, '%')) THEN 'BANK'
           WHEN a.name LIKE '%微信%' THEN 'WECHAT'
           WHEN a.name LIKE '%支付宝%' THEN 'ALIPAY'
           ELSE 'OTHER'
       END AS type
FROM fin_account a
WHERE EXISTS (SELECT 1 FROM tmp_fund_scope s WHERE s.tenant_id = a.tenant_id AND a.code LIKE CONCAT(s.code, '%'))
  AND NOT EXISTS (SELECT 1 FROM fin_account c WHERE c.parent_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM fin_cash_account x WHERE x.account_id = a.id);

-- 名称在本租户唯一的直接用科目名称，否则加科目编码
INSERT INTO fin_cash_account (tenant_id, name, type, account_id, status, sort, create_time, update_time)
SELECT f.tenant_id,
       CASE WHEN EXISTS (SELECT 1 FROM tmp_fund_subject o WHERE o.tenant_id = f.tenant_id AND o.name = f.name AND o.account_id <> f.account_id)
                 OR EXISTS (SELECT 1 FROM fin_cash_account x WHERE x.tenant_id = f.tenant_id AND x.name = f.name)
            THEN CONCAT(f.name, ' ', f.code) ELSE f.name END,
       f.type, f.account_id, f.status, 0, NOW(), NOW()
FROM tmp_fund_subject f;

DROP TABLE IF EXISTS tmp_fund_subject;
DROP TABLE IF EXISTS tmp_fund_scope;

-- ---------------------------------------------------- 4. 字典

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 216, '资金账户类型', 'fin_cash_account_type', 'SYSTEM', 1, '资金账户的类型：现金账户的入账科目在现金科目下，其他在银行科目或其他货币资金科目下', NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = 'fin_cash_account_type' OR t.id = 216);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '资金账户类型', 'fin_cash_account_type', 'SYSTEM', 1, '资金账户的类型：现金账户的入账科目在现金科目下，其他在银行科目或其他货币资金科目下', NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = 'fin_cash_account_type');

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 217, '期初未达账类型', 'fin_bank_opening_side', 'SYSTEM', 1, '银行类资金账户启用时的期初未达账：企业已记银行未记、银行已记企业未记', NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = 'fin_bank_opening_side' OR t.id = 217);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '期初未达账类型', 'fin_bank_opening_side', 'SYSTEM', 1, '银行类资金账户启用时的期初未达账：企业已记银行未记、银行已记企业未记', NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = 'fin_bank_opening_side');

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, g.dict_type, g.dict_label, g.dict_value, g.tag_color, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'fin_cash_account_type' AS dict_type, '现金' AS dict_label, 'CASH' AS dict_value, 'gold' AS tag_color, 1 AS sort
    UNION ALL SELECT 'fin_cash_account_type', '银行', 'BANK', 'blue', 2
    UNION ALL SELECT 'fin_cash_account_type', '微信', 'WECHAT', 'green', 3
    UNION ALL SELECT 'fin_cash_account_type', '支付宝', 'ALIPAY', 'cyan', 4
    UNION ALL SELECT 'fin_cash_account_type', '其他', 'OTHER', 'default', 5
    UNION ALL SELECT 'fin_bank_opening_side', '企业已记银行未记', 'BOOK', 'orange', 1
    UNION ALL SELECT 'fin_bank_opening_side', '银行已记企业未记', 'BANK', 'purple', 2
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = g.dict_type AND x.dict_value = g.dict_value AND x.tenant_id = 1);

-- ---------------------------------------------------- 5. 菜单「资金账户」

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, g.type, g.path, g.component, g.perms, g.icon, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 393 AS id, 933 AS parent_id, '资金账户' AS name, 'C' AS type, 'cash-account' AS path, 'fin/cash-account/index' AS component,
           'fin:cashAccount:list' AS perms, 'CreditCardOutlined' AS icon, 0 AS sort
    UNION ALL SELECT 3930, 393, '资金账户新增', 'F', NULL, NULL, 'fin:cashAccount:add', NULL, 1
    UNION ALL SELECT 3931, 393, '资金账户修改', 'F', NULL, NULL, 'fin:cashAccount:edit', NULL, 2
    UNION ALL SELECT 3932, 393, '资金账户删除', 'F', NULL, NULL, 'fin:cashAccount:remove', NULL, 3
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = g.id);

-- 套餐：包含「出纳管理」（341）的套餐加入资金账户及其按钮
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 341) p
JOIN (SELECT 393 AS menu_id UNION ALL SELECT 3930 UNION ALL SELECT 3931 UNION ALL SELECT 3932) g ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

-- 内置角色（按角色编码）：出纳、会计可维护，总经理、普通用户只读（资金与费用分组 933 和财务模块 7 已随出纳管理授权）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (
    SELECT 'cashier' AS code, 393 AS menu_id
    UNION ALL SELECT 'cashier', 3930 UNION ALL SELECT 'cashier', 3931 UNION ALL SELECT 'cashier', 3932
    UNION ALL SELECT 'accountant', 393 UNION ALL SELECT 'accountant', 3930 UNION ALL SELECT 'accountant', 3931 UNION ALL SELECT 'accountant', 3932
    UNION ALL SELECT 'gm', 393 UNION ALL SELECT 'common', 393
) g ON g.code = r.code
WHERE (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = 393))
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 341)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 其他租户的管理员（菜单与套餐一致）：有出纳管理的加资金账户及其按钮
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 393 AS menu_id UNION ALL SELECT 3930 UNION ALL SELECT 3931 UNION ALL SELECT 3932) g ON 1 = 1
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 341)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);
