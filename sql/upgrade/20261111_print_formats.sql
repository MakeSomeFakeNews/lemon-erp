-- ====================================================
-- 打印模板多格式（issue #96）。可重复执行；只修改 sys_print_template，与其他 20261111 脚本没有依赖
-- 1. 同一单据（模板标识）可以有多份打印格式：sys_print_template 增加格式名称、是否默认、排序、状态
--    （name / is_default / sort / status），唯一键改为 (template_key, name, tenant_id)；
--    已有的模板（每个模板标识一行）成为该单据的默认格式「标准格式」
-- 2. 示例：平台和已有租户的销售出库单（scm.salesOut）预置「发货单（不含价格）」格式（标题「发货单」，
--    隐藏单价、税率、金额列和金额类表头字段，给同行的发货单、代发第三方时只显示数量），不作为默认；
--    还没有修改过销售出库单模板的租户同时保存一份默认的「标准格式」（配置为空，与原来的默认版式一致）。
--    已存在同名格式时跳过
-- ====================================================

-- ---------------------------------------------------- 1. 格式字段

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template' AND COLUMN_NAME = 'name');
SET @s = IF(@c = 0, 'ALTER TABLE sys_print_template ADD COLUMN name VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''格式名称'' AFTER template_key', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已有的模板为各单据的标准格式（新加列时为空）
UPDATE sys_print_template SET name = '标准格式' WHERE name = '';
ALTER TABLE sys_print_template MODIFY COLUMN name VARCHAR(32) NOT NULL COMMENT '格式名称，同一模板标识内唯一；只有一份格式时为「标准格式」';

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template' AND COLUMN_NAME = 'is_default');
SET @s = IF(@c = 0, 'ALTER TABLE sys_print_template ADD COLUMN is_default TINYINT NOT NULL DEFAULT 1 COMMENT ''是否默认格式 1是 0否：每个模板标识只有一份，只有一份启用的格式时直接按它打印'' AFTER name', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template' AND COLUMN_NAME = 'sort');
SET @s = IF(@c = 0, 'ALTER TABLE sys_print_template ADD COLUMN sort INT NOT NULL DEFAULT 0 COMMENT ''排序（打印时选择格式的顺序）'' AFTER is_default', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template' AND COLUMN_NAME = 'status');
SET @s = IF(@c = 0, 'ALTER TABLE sys_print_template ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''状态 1启用 0停用（默认格式不能停用）'' AFTER sort', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 唯一键加上格式名称
SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_print_template'
          AND INDEX_NAME = 'uk_print_template' AND COLUMN_NAME = 'name');
SET @s = IF(@c = 0, 'ALTER TABLE sys_print_template DROP INDEX uk_print_template, ADD UNIQUE KEY uk_print_template (template_key, name, tenant_id)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_print_template COMMENT = '打印模板';

-- ---------------------------------------------------- 2. 销售出库单的「发货单（不含价格）」示例

-- 还没有修改过销售出库单模板的租户：先保存默认的标准格式
INSERT INTO sys_print_template (tenant_id, template_key, name, is_default, sort, status, config, create_time, update_time)
SELECT t.id, 'scm.salesOut', '标准格式', 1, 0, 1, '{}', NOW(), NOW()
FROM sys_tenant t
WHERE NOT EXISTS (SELECT 1 FROM sys_print_template x WHERE x.tenant_id = t.id AND x.template_key = 'scm.salesOut');

INSERT INTO sys_print_template (tenant_id, template_key, name, is_default, sort, status, config, create_time, update_time)
SELECT t.id, 'scm.salesOut', '发货单（不含价格）', 0, 1, 1,
       '{"title":"发货单","orientation":"portrait","paper":"A4","margin":10,"fontSize":10,"showCompany":true,"showNote":true,"fields":[{"label":"订单号"},{"label":"客户"},{"label":"仓库"},{"label":"业务日期"},{"label":"仓库单据"},{"label":"发票类型","hidden":true},{"label":"优惠前合计","hidden":true},{"label":"整单优惠","hidden":true},{"label":"原单优惠分摊","hidden":true},{"label":"优惠后合计","hidden":true},{"label":"价税合计","hidden":true},{"label":"运费","hidden":true},{"label":"运费承担","hidden":true},{"label":"含代垫运费合计","hidden":true},{"label":"原币","hidden":true}],"columns":[{"title":"序号"},{"title":"物料编码"},{"title":"物料名称"},{"title":"规格型号"},{"title":"单位"},{"title":"仓库"},{"title":"库位"},{"title":"批次"},{"title":"数量"},{"title":"含税单价","hidden":true},{"title":"税率","hidden":true},{"title":"价税合计","hidden":true},{"title":"序列号"}],"signs":[{"label":"制单"},{"label":"仓管"},{"label":"客户签收"}]}',
       NOW(), NOW()
FROM sys_tenant t
WHERE NOT EXISTS (SELECT 1 FROM sys_print_template x WHERE x.tenant_id = t.id AND x.template_key = 'scm.salesOut' AND x.name = '发货单（不含价格）');
