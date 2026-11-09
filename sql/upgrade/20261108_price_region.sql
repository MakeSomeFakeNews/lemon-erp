-- ====================================================
-- 客户区域改为业务字典 scm_region（每个租户一份）：客户（scm_customer.region）、CRM 客户（crm_customer.region）、
-- 线索（crm_lead.region）原来是自由文本，改为下拉选择；进销存报表「按区域」按字典名称显示。
-- 已有的文本：与字典名称相同的换成对应的键值，其余的不同取值按租户写入字典（名称为原文本，键值为 R + 文本 MD5 的前 12 位），
-- 再把各表的值换成键值。可重复执行（已经是键值的不再处理）
-- ====================================================

-- 1. 字典类型
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT 187, '客户区域', 'scm_region', 'BUSINESS', 1, '客户的销售区域（经销商为授权区域），CRM 客户、线索的地区也用此字典，进销存报表按区域汇总', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_region' OR x.id = 187);
-- id 187 已被占用时（不应出现）按自增 ID 插入
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT '客户区域', 'scm_region', 'BUSINESS', 1, '客户的销售区域（经销商为授权区域），CRM 客户、线索的地区也用此字典，进销存报表按区域汇总', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = 'scm_region');

-- 2. 默认取值：平台和已有租户各一份
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time)
SELECT t.id, 'scm_region', d.dict_label, d.dict_value, d.tag_color, d.sort, 1, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT '华东' AS dict_label, 'EAST' AS dict_value, 'blue' AS tag_color, 1 AS sort
    UNION ALL SELECT '华南', 'SOUTH', 'green', 2
    UNION ALL SELECT '华北', 'NORTH', 'orange', 3
    UNION ALL SELECT '华中', 'CENTRAL', 'cyan', 4
    UNION ALL SELECT '西南', 'SOUTHWEST', 'purple', 5
    UNION ALL SELECT '西北', 'NORTHWEST', 'gold', 6
    UNION ALL SELECT '东北', 'NORTHEAST', 'geekblue', 7
    UNION ALL SELECT '海外', 'OVERSEAS', 'magenta', 8
) d ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = t.id AND x.dict_type = 'scm_region' AND x.dict_value = d.dict_value);

-- 3. 去掉首尾空格，空白改为 NULL
UPDATE scm_customer SET region = NULLIF(TRIM(region), '') WHERE region IS NOT NULL;
UPDATE crm_customer SET region = NULLIF(TRIM(region), '') WHERE region IS NOT NULL;
UPDATE crm_lead SET region = NULLIF(TRIM(region), '') WHERE region IS NOT NULL;

-- 4. 既不是键值、也不是已有名称的文本写入字典（同一租户内名称相同的只写一条）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time)
SELECT g.tenant_id, 'scm_region', g.label, CONCAT('R', UPPER(LEFT(MD5(g.region), 12))), NULL, 100, 1, '升级时由已有的区域文本生成', NOW(), NOW()
FROM (
    SELECT r.tenant_id, LEFT(r.region, 100) AS label, MIN(r.region) AS region
    FROM (
        SELECT tenant_id, region FROM scm_customer WHERE region IS NOT NULL
        UNION ALL SELECT tenant_id, region FROM crm_customer WHERE region IS NOT NULL
        UNION ALL SELECT tenant_id, region FROM crm_lead WHERE region IS NOT NULL
    ) r
    WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = r.tenant_id AND x.dict_type = 'scm_region'
                                                      AND (BINARY x.dict_value = BINARY r.region OR x.dict_label = LEFT(r.region, 100)))
    GROUP BY r.tenant_id, LEFT(r.region, 100)
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = g.tenant_id AND x.dict_type = 'scm_region'
                                                  AND x.dict_value = CONCAT('R', UPPER(LEFT(MD5(g.region), 12))));

-- 5. 各表的文本按名称换成键值（已经是键值的不变）
UPDATE scm_customer c
JOIN sys_dict_data d ON d.tenant_id = c.tenant_id AND d.dict_type = 'scm_region' AND d.dict_label = LEFT(c.region, 100)
SET c.region = d.dict_value
WHERE c.region IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = c.tenant_id AND x.dict_type = 'scm_region' AND BINARY x.dict_value = BINARY c.region);
UPDATE crm_customer c
JOIN sys_dict_data d ON d.tenant_id = c.tenant_id AND d.dict_type = 'scm_region' AND d.dict_label = LEFT(c.region, 100)
SET c.region = d.dict_value
WHERE c.region IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = c.tenant_id AND x.dict_type = 'scm_region' AND BINARY x.dict_value = BINARY c.region);
UPDATE crm_lead c
JOIN sys_dict_data d ON d.tenant_id = c.tenant_id AND d.dict_type = 'scm_region' AND d.dict_label = LEFT(c.region, 100)
SET c.region = d.dict_value
WHERE c.region IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = c.tenant_id AND x.dict_type = 'scm_region' AND BINARY x.dict_value = BINARY c.region);

-- 6. 列注释
ALTER TABLE scm_customer MODIFY COLUMN region VARCHAR(128) DEFAULT NULL COMMENT '区域(业务字典 scm_region)，经销商为授权区域，进销存报表按区域汇总';
ALTER TABLE crm_customer MODIFY COLUMN region VARCHAR(64) DEFAULT NULL COMMENT '所在地区(业务字典 scm_region)';
ALTER TABLE crm_lead MODIFY COLUMN region VARCHAR(64) DEFAULT NULL COMMENT '地区(业务字典 scm_region)';
