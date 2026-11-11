-- ====================================================
-- 下拉列表远程搜索与模糊检索（issue #86）
-- 物料、客户、供应商增加检索列 search_key：编码、名称、规格（物料，另含条码）/ 联系人、电话（往来单位）
-- 去掉分隔符的小写串、只含数字的串、拼音首字母，空格分隔。下拉搜索和列表关键字除原字段包含关键字外，
-- 还按它匹配拼音首字母（「szzk」→「深圳智控」）和去掉分隔符的缩写（「126034」→「12/R60 34TH」）。
--
-- 拼音无法在 SQL 中计算：本脚本只加列，已有数据的 search_key 为 NULL，
-- 由后端启动后自动回填（SearchKeyBackfill，只处理 NULL 行、按主键分批、可重复执行，回填期间仍可按编码、名称检索）。
-- 可重复执行：列已存在时跳过。只给早已存在的表加列，与其他 20261111_ 脚本没有依赖
-- ====================================================

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'search_key');
SET @s = IF(@c = 0,
            'ALTER TABLE wms_material ADD COLUMN search_key VARCHAR(1000) DEFAULT NULL COMMENT ''检索列：编码、名称、规格、条码去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配'' AFTER remark',
            'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_supplier' AND COLUMN_NAME = 'search_key');
SET @s = IF(@c = 0,
            'ALTER TABLE scm_supplier ADD COLUMN search_key VARCHAR(1000) DEFAULT NULL COMMENT ''检索列：编码、名称、联系人、电话去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配'' AFTER remark',
            'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_customer' AND COLUMN_NAME = 'search_key');
SET @s = IF(@c = 0,
            'ALTER TABLE scm_customer ADD COLUMN search_key VARCHAR(1000) DEFAULT NULL COMMENT ''检索列：编码、名称、联系人、电话去掉分隔符的小写串、数字串、拼音首字母（空格分隔），由程序计算，下拉和列表关键字匹配'' AFTER remark',
            'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
