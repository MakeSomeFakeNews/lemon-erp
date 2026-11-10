-- ====================================================
-- 生产制造（issue #49）。可重复执行
-- 1. 工单报工报废数量 mes_work_order.scrap_qty（各工序报工的报废数合计，按已有报工回填）：完工入库 + 报废达到工单数量时工单完工，
--    在制产出（MRP、齐套、可承诺量）= 工单数量 − 完工 − 报废；工单计划完工日期索引（工单完成情况报表）。
--    已有的「已有完工入库且 完工 + 报废已达到数量」但仍为已下达 / 生产中的工单改为已完工，完工时间取最后一次完工入库或报工的时间
--    （实时库存成本不补转，月末成本核算按 工单数量 − 报废 判断完工，剩余成本计入产品）
-- 2. 菜单：生产制造 → 生产统计（人员计件统计 mes:stat:piece、工序产量统计 mes:stat:output、工单完成情况 mes:stat:wo、
--    产品产量汇总 mes:stat:product、用料差异分析 mes:stat:usage）。包含生产工单的套餐及其租户管理员随套餐加入；
--    内置角色按编码授权：普通用户、总经理、生产计划员、车间主管全部，仓库管理员产品产量，会计人员计件、产品产量、用料差异，
--    人事专员人员计件，质量工程师工序产量、工单完成情况；平台以外的租户只授权套餐中包含的菜单
-- 3. 业务参数「完工入库领料校验」mes.finish.kitCheck 不需要改库（未修改时为默认值「提示」）
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
        -- 新加的列：按已有报工回填
        IF t = 'mes_work_order' AND c = 'scrap_qty' THEN
            UPDATE mes_work_order o
            SET o.scrap_qty = (SELECT COALESCE(SUM(op.scrap_qty), 0) FROM mes_wo_op op WHERE op.wo_id = o.id)
            WHERE o.id > 0;
        END IF;
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

-- 1. 表结构
CALL lemon_add_column('mes_work_order', 'scrap_qty',
    'scrap_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT ''报工报废数量（各工序报废合计），完工 + 报废达到工单数量时完工'' AFTER finished_qty');
ALTER TABLE mes_work_order MODIFY COLUMN close_cost DECIMAL(18, 2) DEFAULT NULL
    COMMENT '关闭（或报工报废、改量后不经完工入库而完工）时尚未转入产品的成本（净领料成本 + 工费 − 已转入完工产品）';
CALL lemon_add_index('mes_work_order', 'idx_wo_plan_end', 'KEY idx_wo_plan_end (plan_end)');

DROP PROCEDURE IF EXISTS lemon_add_column;
DROP PROCEDURE IF EXISTS lemon_add_index;

-- 完工 + 报废已达到数量的在制工单改为已完工
UPDATE mes_work_order o
SET o.status = 'COMPLETED',
    o.finish_time = COALESCE(GREATEST((SELECT MAX(d.create_time) FROM mes_wo_doc d WHERE d.wo_id = o.id AND d.kind = 'FINISH'),
                                      (SELECT MAX(r.create_time) FROM mes_report r WHERE r.wo_id = o.id)),
                             (SELECT MAX(d.create_time) FROM mes_wo_doc d WHERE d.wo_id = o.id AND d.kind = 'FINISH'), NOW()),
    o.update_time = NOW()
WHERE o.status IN ('RELEASED', 'STARTED') AND o.finished_qty > 0 AND o.finished_qty + o.scrap_qty >= o.qty;

-- 2. 菜单：生产制造 → 生产统计
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, g.type, g.path, g.component, g.perms, g.icon, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 970 AS id, 6 AS parent_id, '生产统计' AS name, 'M' AS type, NULL AS path, NULL AS component, NULL AS perms,
           'BarChartOutlined' AS icon, 4 AS sort
    UNION ALL SELECT 410, 970, '人员计件统计', 'C', 'stat-piece', 'mes/stat/piece/index', 'mes:stat:piece', 'TeamOutlined', 1
    UNION ALL SELECT 411, 970, '工序产量统计', 'C', 'stat-output', 'mes/stat/output/index', 'mes:stat:output', 'BarChartOutlined', 2
    UNION ALL SELECT 412, 970, '工单完成情况', 'C', 'stat-wo', 'mes/stat/wo/index', 'mes:stat:wo', 'CarryOutOutlined', 3
    UNION ALL SELECT 413, 970, '产品产量汇总', 'C', 'stat-product', 'mes/stat/product/index', 'mes:stat:product', 'AppstoreOutlined', 4
    UNION ALL SELECT 414, 970, '用料差异分析', 'C', 'stat-usage', 'mes/stat/usage/index', 'mes:stat:usage', 'DiffOutlined', 5
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = g.id);

-- 套餐：包含生产工单的套餐加入生产统计
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 162) p
JOIN sys_menu m ON m.id IN (970, 410, 411, 412, 413, 414)
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = m.id);

-- 其他租户的管理员（菜单与套餐一致）：有生产工单的加生产统计
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (970, 410, 411, 412, 413, 414)
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 162)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- 内置角色（各租户按角色编码），连同分组和生产制造模块
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'common' AS code, 6 AS menu_id UNION ALL SELECT 'common', 970 UNION ALL SELECT 'common', 410 UNION ALL SELECT 'common', 411
      UNION ALL SELECT 'common', 412 UNION ALL SELECT 'common', 413 UNION ALL SELECT 'common', 414
      UNION ALL SELECT 'gm', 6 UNION ALL SELECT 'gm', 970 UNION ALL SELECT 'gm', 410 UNION ALL SELECT 'gm', 411
      UNION ALL SELECT 'gm', 412 UNION ALL SELECT 'gm', 413 UNION ALL SELECT 'gm', 414
      UNION ALL SELECT 'planner', 6 UNION ALL SELECT 'planner', 970 UNION ALL SELECT 'planner', 410 UNION ALL SELECT 'planner', 411
      UNION ALL SELECT 'planner', 412 UNION ALL SELECT 'planner', 413 UNION ALL SELECT 'planner', 414
      UNION ALL SELECT 'workshop', 6 UNION ALL SELECT 'workshop', 970 UNION ALL SELECT 'workshop', 410 UNION ALL SELECT 'workshop', 411
      UNION ALL SELECT 'workshop', 412 UNION ALL SELECT 'workshop', 413 UNION ALL SELECT 'workshop', 414
      UNION ALL SELECT 'warehouse', 6 UNION ALL SELECT 'warehouse', 970 UNION ALL SELECT 'warehouse', 413
      UNION ALL SELECT 'accountant', 6 UNION ALL SELECT 'accountant', 970 UNION ALL SELECT 'accountant', 410
      UNION ALL SELECT 'accountant', 413 UNION ALL SELECT 'accountant', 414
      UNION ALL SELECT 'hr', 6 UNION ALL SELECT 'hr', 970 UNION ALL SELECT 'hr', 410
      UNION ALL SELECT 'quality', 6 UNION ALL SELECT 'quality', 970 UNION ALL SELECT 'quality', 411 UNION ALL SELECT 'quality', 412) g
  ON g.code = r.code
WHERE (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = g.menu_id))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);
