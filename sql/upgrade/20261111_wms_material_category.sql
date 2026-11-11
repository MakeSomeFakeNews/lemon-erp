-- ====================================================
-- 物料（商品）多级分类。可重复执行
-- 1. 新表 wms_material_category（多级分类，每个租户一份；物料等仍按分类编码关联）；物料表增加分类索引 idx_material_category，
--    引用分类编码的列（物料、促销、返利项目、检验方案、提成规则）更新注释
-- 2. 每个租户的业务字典 wms_material_category 迁为一级分类：编码 = 原键值，名称 = 原标签，排序、状态、内置标记沿用，
--    已有物料、检验方案、促销、提成规则、凭证科目设置中的分类编码不变；物料上有、字典中没有的分类编码（字典取值已删除）补为一级分类（名称为编码），
--    缺少内置分类（原材料、半成品、成品、低值易耗品）的租户从平台补上
-- 3. 删除原业务字典 wms_material_category（类型和全部租户的取值）：分类改在「仓库 → 基础资料 → 物料管理」左侧的分类树中维护，
--    不再出现在「业务字典」中；前端标签、下拉框、报表仍按该类型读取，由仓库模块提供（显示完整路径，如「电子元器件 / 电阻」）
-- 4. 按钮「物料分类维护」（wms:material:category，物料管理页面下）：包含物料管理的套餐加入，有物料修改权限的角色同时授权
-- ====================================================

-- 1. 表结构
CREATE TABLE IF NOT EXISTS wms_material_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级分类ID，0为一级分类',
    ancestors   VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '祖级ID路径（逗号分隔，从 0 开始，如 0,3,7），查询下级分类用',
    code        VARCHAR(16)  NOT NULL COMMENT '分类编码：物料、检验方案、促销、提成规则、凭证科目设置等按编码关联，保存后不能修改',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称（同一上级下不重复）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '同级排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（停用后连同下级不能再被新物料选用）',
    builtin     TINYINT      NOT NULL DEFAULT 0 COMMENT '内置分类 1是：程序依赖（原材料、半成品、成品、低值易耗品），不能删除',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_category_code (code, tenant_id),
    KEY idx_material_category_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料分类（多级，每个租户一份）';

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

CALL lemon_add_index('wms_material', 'idx_material_category', 'KEY idx_material_category (category)');

DROP PROCEDURE IF EXISTS lemon_add_index;

ALTER TABLE wms_material MODIFY COLUMN category VARCHAR(64) DEFAULT NULL COMMENT '分类编码(wms_material_category.code，多级分类)';
ALTER TABLE scm_promotion MODIFY COLUMN material_category VARCHAR(16) DEFAULT NULL COMMENT '适用物料分类编码(wms_material_category.code，含下级分类)，物料和分类都为空适用全部物料';
ALTER TABLE scm_rebate_item MODIFY COLUMN category VARCHAR(32) DEFAULT NULL COMMENT '物料分类编码(wms_material_category.code)';
ALTER TABLE qms_inspection_plan MODIFY COLUMN material_category VARCHAR(32) DEFAULT NULL COMMENT '适用物料分类编码(wms_material_category.code，含下级分类)';
ALTER TABLE fin_commission_rule MODIFY COLUMN material_category VARCHAR(32) DEFAULT NULL COMMENT '适用物料分类编码(wms_material_category.code，含下级分类)，为空适用全部；回款不区分分类';

-- 2. 字典数据迁为一级分类（按租户 + 编码判断是否已迁移）
INSERT INTO wms_material_category (tenant_id, parent_id, ancestors, code, name, sort, status, builtin, remark, create_time, update_time)
SELECT d.tenant_id, 0, '0', d.dict_value, LEFT(d.dict_label, 64), d.sort, d.status, d.builtin, d.remark, NOW(), NOW()
FROM sys_dict_data d
WHERE d.dict_type = 'wms_material_category' AND CHAR_LENGTH(d.dict_value) <= 16
  AND NOT EXISTS (SELECT 1 FROM wms_material_category c WHERE c.tenant_id = d.tenant_id AND c.code = d.dict_value);

-- 物料上有、分类中没有的编码（字典取值已删除的历史数据）：补为一级分类，名称为编码，排在最后
INSERT INTO wms_material_category (tenant_id, parent_id, ancestors, code, name, sort, status, builtin, remark, create_time, update_time)
SELECT m.tenant_id, 0, '0', m.category, m.category, 99, 1, 0, NULL, NOW(), NOW()
FROM (SELECT DISTINCT tenant_id, category FROM wms_material WHERE category IS NOT NULL AND category <> '' AND CHAR_LENGTH(category) <= 16) m
WHERE NOT EXISTS (SELECT 1 FROM wms_material_category c WHERE c.tenant_id = m.tenant_id AND c.code = m.category);

-- 内置分类：各租户缺少的从平台复制（凭证科目的预置值、低值易耗品领用按编码使用）
INSERT INTO wms_material_category (tenant_id, parent_id, ancestors, code, name, sort, status, builtin, remark, create_time, update_time)
SELECT t.id, 0, '0', p.code, p.name, p.sort, p.status, 1, p.remark, NOW(), NOW()
FROM sys_tenant t
JOIN wms_material_category p ON p.tenant_id = 1 AND p.builtin = 1 AND p.parent_id = 0
WHERE t.id <> 1
  AND NOT EXISTS (SELECT 1 FROM wms_material_category c WHERE c.tenant_id = t.id AND c.code = p.code);

-- 3. 删除原业务字典（数据已迁移到分类表）
DELETE FROM sys_dict_data WHERE dict_type = 'wms_material_category';
DELETE FROM sys_dict_type WHERE type = 'wms_material_category';

-- 4. 按钮：物料分类维护
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 1374, 137, '物料分类维护', 'F', NULL, NULL, 'wms:material:category', NULL, 6, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = 1374);

-- 包含物料管理页面的套餐加入
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT DISTINCT p.package_id, 1374
FROM sys_tenant_package_menu p
WHERE p.menu_id = 137
  AND NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 1374);

-- 有物料修改权限的角色（各租户的仓库管理员、租户管理员及自定义角色）同时授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 1374
FROM sys_role_menu rm
WHERE rm.menu_id = 1371
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 1374);
