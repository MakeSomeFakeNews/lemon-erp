-- ====================================================
-- 内置角色权限补齐。可重复执行
-- 1. 质量工程师（quality）补齐员工通用权限：员工自助、费用报销、借款申请
-- 2. 销售员（sales）增加服务工单受理 / 处理 / 删除、市场活动维护
-- 3. 新增菜单「项目 → 我的工时」（pm:timesheet:mine，项目成员填报工时，不含审批），授权普通员工（staff）
-- 4. 新增内置角色「销售经理」（sales_mgr，数据权限本部门及以下）：复制本租户销售员的权限，另加客户分析、价目表维护、商品价格维护
-- 5. 缩短总经理（gm）的备注
-- 各租户的内置角色按角色编码关联；平台以外的租户只授权其套餐中包含的菜单（套餐为空的不受限）
-- ====================================================

-- 菜单：项目 → 我的工时
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 246, 10, '我的工时', 'C', 'my-timesheet', 'pm/timesheet/index', 'pm:timesheet:mine', 'FieldTimeOutlined', 7, 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 246);

-- 套餐：包含工时管理的套餐加入我的工时
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, 246
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 243) p
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 246);

-- 其他租户的管理员（菜单与套餐一致）：有工时管理的加我的工时
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 246
FROM sys_role r
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 243)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 246);

-- 质量工程师、销售员、普通员工
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'quality' AS code, 7 AS menu_id UNION ALL SELECT 'quality', 933 UNION ALL SELECT 'quality', 279
      UNION ALL SELECT 'quality', 2790 UNION ALL SELECT 'quality', 280 UNION ALL SELECT 'quality', 2800
      UNION ALL SELECT 'quality', 8 UNION ALL SELECT 'quality', 951 UNION ALL SELECT 'quality', 209
      UNION ALL SELECT 'sales', 224 UNION ALL SELECT 'sales', 2240 UNION ALL SELECT 'sales', 2241 UNION ALL SELECT 'sales', 2242
      UNION ALL SELECT 'sales', 2250 UNION ALL SELECT 'sales', 2251 UNION ALL SELECT 'sales', 2252
      UNION ALL SELECT 'staff', 10 UNION ALL SELECT 'staff', 246) g ON g.code = r.code
WHERE (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = g.menu_id))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 销售经理：已有销售员角色、还没有销售经理的租户各建一个
INSERT INTO sys_role (tenant_id, name, code, sort, data_scope, status, remark, create_time, update_time)
SELECT s.tenant_id, '销售经理', 'sales_mgr', 17, 4, 1, '销售员的全部功能，可查看本部门及以下的客户、订单和业绩，维护价目表和商品价格', NOW(), NOW()
FROM sys_role s
WHERE s.code = 'sales'
  AND NOT EXISTS (SELECT 1 FROM sys_role x WHERE x.tenant_id = s.tenant_id AND x.code = 'sales_mgr');

-- 销售经理的菜单：本租户销售员的菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT m.id, rm.menu_id
FROM sys_role m
JOIN sys_role s ON s.tenant_id = m.tenant_id AND s.code = 'sales'
JOIN sys_role_menu rm ON rm.role_id = s.id
WHERE m.code = 'sales_mgr'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = m.id AND x.menu_id = rm.menu_id);

-- 另加客户分析、价目表维护、商品价格维护（套餐限制同上）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.id
FROM sys_role r
JOIN sys_menu g ON g.id IN (226, 2700, 3650)
WHERE r.code = 'sales_mgr'
  AND (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = g.id))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.id);

-- 总经理备注：缩短（原文太长，角色列表中把状态开关挤到第二行）
UPDATE sys_role SET remark = '查看全部业务和商务智能，审核关闭订单，审核招聘、假勤、工时'
WHERE code = 'gm' AND remark = '查看全部业务数据和商务智能，审核、关闭采购销售订单，审核招聘需求、假勤和工时';
