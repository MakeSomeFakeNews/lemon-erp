-- ====================================================
-- 应收应付的综合问题（issue #50）。可重复执行
-- 1. 收付款冲抵预收预付：fin_settlement.advance_offset（冲抵合计，原币）、fin_settlement_item.advance_offset（明细中由预收预付冲抵的部分）、
--    新表 fin_settlement_advance（收付款选用的预收预付，草稿、审批中时登记，生效时冲抵）、fin_advance_apply.settlement_id（在收付款中冲抵的记录，
--    凭证并入收付款凭证）。已有数据都没有冲抵，默认 0 / 空即可
-- 2. 收付款草稿：fin_settlement.status 增加 DRAFT 草稿（只改注释），字典「审批状态」增加「草稿」；
--    是否启用由业务参数「收付款先存草稿」（fin.settlement.draft，默认关闭）控制，不需要改库
-- 3. 菜单：收款单确认（fin:receipt:confirm）、付款单确认（fin:payment:confirm）。包含收款单 / 付款单的套餐随之加入；
--    已有「收款单新增」/「付款单新增」权限的角色（各租户的会计、出纳、管理员及自定义角色）同时授权，开启草稿前后操作不受影响
-- 往来汇总、穿透联查只是查询，不需要改库
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
CALL lemon_add_column('fin_settlement', 'advance_offset', 'advance_offset DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''冲抵预收 / 预付合计（原币，不含在金额中）：实收实付 = 核销合计 - 冲抵'' AFTER advance_id');
ALTER TABLE fin_settlement MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'APPROVED' COMMENT '状态 DRAFT草稿 APPROVED已生效 APPROVING审批中 REJECTED已拒绝（付款审批）';
CALL lemon_add_column('fin_settlement_item', 'advance_offset', 'advance_offset DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''其中由预收 / 预付冲抵的金额（原币），核销金额只含实收实付的部分'' AFTER discount_fc');
CALL lemon_add_column('fin_advance_apply', 'settlement_id', 'settlement_id BIGINT DEFAULT NULL COMMENT ''在收付款中冲抵时的收付款单（凭证并入收付款凭证）'' AFTER bill_id');
CALL lemon_add_index('fin_advance_apply', 'idx_advance_apply_settlement', 'KEY idx_advance_apply_settlement (settlement_id)');

DROP PROCEDURE IF EXISTS lemon_add_column;
DROP PROCEDURE IF EXISTS lemon_add_index;

CREATE TABLE IF NOT EXISTS fin_settlement_advance (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id BIGINT         NOT NULL COMMENT '收付款单',
    advance_id    BIGINT         NOT NULL COMMENT '冲抵的预收 / 预付款',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '冲抵金额（原币）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_settlement_advance_settlement (settlement_id),
    KEY idx_settlement_advance_advance (advance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收付款冲抵的预收预付（草稿、审批中时登记，生效时冲抵）';

-- 2. 字典「审批状态」增加「草稿」（系统字典，平台一份，内置）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, 'sys_approve_status', '草稿', 'DRAFT', 'default', 6, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = 1 AND x.dict_type = 'sys_approve_status' AND x.dict_value = 'DRAFT');

-- 3. 菜单：收款单确认、付款单确认
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, 'F', NULL, NULL, g.perms, NULL, 4, 1, 1, NOW(), NOW()
FROM (
    SELECT 1853 AS id, 185 AS parent_id, '收款单确认' AS name, 'fin:receipt:confirm' AS perms
    UNION ALL SELECT 1863, 186, '付款单确认', 'fin:payment:confirm'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = g.id);

-- 套餐：包含收款单 / 付款单的套餐加入对应的确认按钮
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, g.menu_id
FROM sys_tenant_package_menu p
JOIN (SELECT 185 AS page_id, 1853 AS menu_id UNION ALL SELECT 186, 1863) g ON g.page_id = p.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = g.menu_id);

-- 角色：有新增按钮的角色同时授权确认按钮（新增 1850 → 确认 1853，新增 1860 → 确认 1863）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, g.menu_id
FROM sys_role_menu rm
JOIN (SELECT 1850 AS add_id, 1853 AS menu_id UNION ALL SELECT 1860, 1863) g ON g.add_id = rm.menu_id
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = g.menu_id);
