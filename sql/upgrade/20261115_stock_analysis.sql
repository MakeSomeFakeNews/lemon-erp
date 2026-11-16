-- ====================================================
-- 库存分析（issue #116）。可重复执行，只增加菜单和授权，需要先执行 20261112_stock_limit.sql（积压分析、库存上下限）
-- 菜单「周转分析」「滞销畅销」「库存预警」（供应链 → 进销存报表，权限 bi:jxc:turnover / bi:jxc:salesRate / bi:jxc:stockAlert），
-- 包含「积压分析」（376）的套餐加入；内置角色普通用户、总经理、会计、采购员、仓库管理员授权（与积压分析相同的角色），
-- 其他租户有积压分析的管理员也加入
-- ====================================================

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, 966, g.name, 'C', g.path, g.component, g.perms, g.icon, g.sort, 1, 1, NOW(), NOW()
FROM (SELECT 385 AS id, '周转分析' AS name, 'jxc-turnover' AS path, 'jxc/turnover/index' AS component, 'bi:jxc:turnover' AS perms, 'SyncOutlined' AS icon, 8 AS sort
      UNION ALL SELECT 386, '滞销畅销', 'jxc-sales-rate', 'jxc/salesRate/index', 'bi:jxc:salesRate', 'FireOutlined', 9
      UNION ALL SELECT 387, '库存预警', 'jxc-stock-alert', 'jxc/stockAlert/index', 'bi:jxc:stockAlert', 'AlertOutlined', 10) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = g.id);

-- 套餐：包含「积压分析」（376）的套餐加入
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 376) p
JOIN (SELECT 385 AS menu_id UNION ALL SELECT 386 UNION ALL SELECT 387) g ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

-- 内置角色（按角色编码，与积压分析相同）：连同进销存报表分组（966）和供应链模块（5）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 5 AS menu_id UNION ALL SELECT 966 UNION ALL SELECT 385 UNION ALL SELECT 386 UNION ALL SELECT 387) g ON 1 = 1
WHERE r.code IN ('common', 'gm', 'accountant', 'purchaser', 'warehouse')
  AND (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = 385))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 其他租户的管理员（菜单与套餐一致）：有积压分析的加入
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 385 AS menu_id UNION ALL SELECT 386 UNION ALL SELECT 387) g ON 1 = 1
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 376)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);
