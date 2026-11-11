-- ====================================================
-- 商品速查（issue #97）。可重复执行
-- 菜单「供应链 → 销售 → 商品速查」（scm:quickLookup:list，Web 与小程序共用）。价格按已有权限过滤，不新增按钮：
-- 进货类价格看采购订单 / 采购入库的查看权限，库存均价看「查看成本金额」（wms:stock:cost），售价类看报价 / 销售订单 / 销售出库的查看权限。
-- 包含「商品价格」页面的套餐随之加入；各租户的普通用户、总经理、采购员、销售员、仓库管理员、销售经理（按角色编码，非平台租户以套餐为上限）
-- 和已有「商品价格」页面的租户管理员授权，连同销售分组和供应链模块。
-- 用户选择显示的价格保存在个人设置（sys_user_setting，设置项 quickLookup），不需要改库
-- ====================================================

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 368, 902, '商品速查', 'C', 'quick-lookup', 'scm/quickLookup/index', 'scm:quickLookup:list', 'SearchOutlined', 10, 1, 1, NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = 368);

-- 套餐：包含「商品价格」（365）的套餐加入商品速查
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, 368
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 365) p
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 368);

-- 内置角色（按角色编码）：商品速查连同销售分组（902）和供应链模块（5）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 5 AS menu_id UNION ALL SELECT 902 UNION ALL SELECT 368) g ON 1 = 1
WHERE r.code IN ('common', 'gm', 'purchaser', 'sales', 'warehouse', 'sales_mgr')
  AND (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = 368))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 其他租户的管理员（菜单与套餐一致）：有商品价格的加商品速查
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 368
FROM sys_role r
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 365)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 368);
