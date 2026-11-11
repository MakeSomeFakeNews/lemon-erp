-- ====================================================
-- 供应商营运路线加长（issue #81）
-- 物流公司的营运路线（scm_supplier.routes）要能写完整的途经站点，如「青岛北—青岛西—诸城—青州—潍坊—淄博—济南」，
-- 由 VARCHAR(255) 加长到 VARCHAR(1000)，已有数据不变。
-- 该列由 20261111_purchase_discount_freight.sql 新增（按文件名顺序在本脚本之前执行）；可重复执行，已加长时跳过
-- ====================================================

SET @len = (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_supplier' AND COLUMN_NAME = 'routes');
SET @s = IF(@len IS NULL,
            'ALTER TABLE scm_supplier ADD COLUMN routes VARCHAR(1000) DEFAULT NULL COMMENT ''营运路线（物流公司），可写途经站点，如 青岛北—青岛西—诸城—济南'' AFTER logistics',
            IF(@len < 1000,
               'ALTER TABLE scm_supplier MODIFY COLUMN routes VARCHAR(1000) DEFAULT NULL COMMENT ''营运路线（物流公司），可写途经站点，如 青岛北—青岛西—诸城—济南''',
               'SELECT 1'));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
