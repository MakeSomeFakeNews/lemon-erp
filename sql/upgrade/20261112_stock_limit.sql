-- ====================================================
-- 库存上下限与积压分析（issue #111）。可重复执行，与同日的 20261112_transfer_price.sql 没有依赖
-- 1. 物料增加库存上限（max_stock）和库存预警方式（alert_mode：TOTAL 按总库存，默认，与原来的安全库存预警一致；
--    WAREHOUSE 分仓库），分仓库的上下限表 wms_material_warehouse_limit；原有的安全库存即库存下限，不需要换算
-- 2. 系统字典「库存预警方式」wms_stock_alert_mode
-- 3. 菜单「积压分析」（供应链 → 进销存报表，权限 bi:jxc:overstock），包含「商品分析」的套餐加入，
--    内置角色普通用户、总经理、会计、采购员、仓库管理员授权（与商品分析相同的角色），其他租户有商品分析的管理员也加入
-- ====================================================

-- ---------------------------------------------------- 1. 物料字段、分仓库上下限

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'max_stock');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN max_stock DECIMAL(18, 4) DEFAULT NULL COMMENT ''库存上限：按总库存预警时所有仓库的合计高于该值时预警；分仓库预警时为各仓库上限之和（保存时计算）'' AFTER safety_stock', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'alert_mode');
SET @s = IF(@c = 0, 'ALTER TABLE wms_material ADD COLUMN alert_mode VARCHAR(16) NOT NULL DEFAULT ''TOTAL'' COMMENT ''库存预警方式(字典 wms_stock_alert_mode) TOTAL按总库存 WAREHOUSE分仓库（wms_material_warehouse_limit）'' AFTER max_stock', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE wms_material MODIFY COLUMN safety_stock DECIMAL(18, 4) DEFAULT NULL COMMENT '安全库存（库存下限）：按总库存预警时所有仓库的合计低于该值时预警；分仓库预警时为各仓库下限之和（保存时计算）';

CREATE TABLE IF NOT EXISTS wms_material_warehouse_limit (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    min_qty      DECIMAL(18, 4)          DEFAULT NULL COMMENT '库存下限（安全库存）：该仓库库存低于时预警，为空不检查',
    max_qty      DECIMAL(18, 4)          DEFAULT NULL COMMENT '库存上限：该仓库库存高于时预警，为空不检查',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_warehouse_limit (material_id, warehouse_id, tenant_id),
    KEY idx_material_limit_warehouse (warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料分仓库的库存上下限（物料的库存预警方式为分仓库时使用，没有设置的仓库不预警）';

-- ---------------------------------------------------- 2. 字典

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 209, '库存预警方式', 'wms_stock_alert_mode', 'SYSTEM', 1, '物料的库存上下限针对所有仓库的合计（按总库存），还是每个仓库单独设置（分仓库）', NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = 'wms_stock_alert_mode' OR t.id = 209);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '库存预警方式', 'wms_stock_alert_mode', 'SYSTEM', 1, '物料的库存上下限针对所有仓库的合计（按总库存），还是每个仓库单独设置（分仓库）', NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = 'wms_stock_alert_mode');

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, g.dict_type, g.dict_label, g.dict_value, g.tag_color, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'wms_stock_alert_mode' AS dict_type, '按总库存' AS dict_label, 'TOTAL' AS dict_value, 'blue' AS tag_color, 1 AS sort
    UNION ALL SELECT 'wms_stock_alert_mode', '分仓库', 'WAREHOUSE', 'purple', 2
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = g.dict_type AND x.dict_value = g.dict_value AND x.tenant_id = 1);

-- ---------------------------------------------------- 3. 菜单「积压分析」

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 376, 966, '积压分析', 'C', 'jxc-overstock', 'jxc/overstock/index', 'bi:jxc:overstock', 'HourglassOutlined', 7, 1, 1, NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = 376);

-- 套餐：包含「商品分析」（374）的套餐加入积压分析
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, 376
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 374) p
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 376);

-- 内置角色（按角色编码，与商品分析相同）：积压分析连同进销存报表分组（966）和供应链模块（5）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 5 AS menu_id UNION ALL SELECT 966 UNION ALL SELECT 376) g ON 1 = 1
WHERE r.code IN ('common', 'gm', 'accountant', 'purchaser', 'warehouse')
  AND (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = 376))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);

-- 其他租户的管理员（菜单与套餐一致）：有商品分析的加积压分析
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 376
FROM sys_role r
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 374)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = 376);
