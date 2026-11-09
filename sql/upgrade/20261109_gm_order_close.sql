-- ====================================================
-- 总经理角色增加「采购订单关闭」「销售订单关闭」按钮权限（总经理可以审核订单，也应能关闭已审核的订单）。可重复执行
-- 各租户的总经理角色按角色编码 gm 关联
-- ====================================================

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (1404, 1434)
WHERE r.code = 'gm'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

UPDATE sys_role SET remark = '查看全部业务数据和商务智能，审核、关闭采购销售订单，审核招聘需求、假勤和工时'
WHERE code = 'gm' AND remark = '查看全部业务数据和商务智能，审核采购销售订单、招聘需求、假勤和工时';
