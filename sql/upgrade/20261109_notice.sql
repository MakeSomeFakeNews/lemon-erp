-- ====================================================
-- 通知公告。可重复执行
-- 1. 新表：sys_notice（公告）、sys_notice_target（发布范围的部门 / 角色）、sys_notice_recipient（接收人与阅读记录）
-- 2. 字典：公告状态 sys_notice_status、公告发布范围 sys_notice_scope（系统字典）；公告类型 sys_notice_type（业务字典，平台和已有租户各一份）；
--    站内消息类别 sys_message_category 增加「公告」（NOTICE）
-- 3. 菜单「系统管理 → 通知公告」（sys:notice:list，按钮 add / edit / remove / publish）：
--    包含系统管理的套餐加入；其他租户的管理员（菜单与套餐一致）随套餐加入；人事专员（hr）全部权限、普通用户（common）只读
--    （各租户按角色编码关联；平台以外的租户只授权其套餐中包含的菜单，套餐为空的不受限）
-- 员工查看公告的「全部公告」页面是固定路由，不需要菜单权限
-- ====================================================

-- 1. 表
CREATE TABLE IF NOT EXISTS sys_notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title        VARCHAR(128) NOT NULL COMMENT '标题',
    type         VARCHAR(32)  NOT NULL COMMENT '类型(业务字典 sys_notice_type)',
    content      MEDIUMTEXT COMMENT '正文(Markdown)',
    scope_type   VARCHAR(16)  NOT NULL DEFAULT 'ALL' COMMENT '发布范围(字典 sys_notice_scope) ALL全员 DEPT指定部门(含下级) ROLE指定角色',
    pinned       TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶 1是 0否',
    expire_date  DATE                  DEFAULT NULL COMMENT '有效期至(含当天)，为空长期有效，过期后员工不再看到',
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 sys_notice_status) DRAFT草稿 PUBLISHED已发布 WITHDRAWN已撤回',
    publish_time DATETIME              DEFAULT NULL COMMENT '最近发布时间',
    publish_by   BIGINT                DEFAULT NULL COMMENT '发布人',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_notice_status (status, publish_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告';

CREATE TABLE IF NOT EXISTS sys_notice_target (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    notice_id   BIGINT NOT NULL COMMENT '公告ID',
    target_id   BIGINT NOT NULL COMMENT '部门ID(指定部门)或角色ID(指定角色)',
    PRIMARY KEY (id),
    KEY idx_notice_target (notice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告发布范围';

CREATE TABLE IF NOT EXISTS sys_notice_recipient (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT   NOT NULL DEFAULT 1 COMMENT '租户ID',
    notice_id   BIGINT   NOT NULL COMMENT '公告ID',
    user_id     BIGINT   NOT NULL COMMENT '接收人(发布时按范围确定)',
    read_time   DATETIME          DEFAULT NULL COMMENT '阅读时间，未读为空',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notice_recipient (notice_id, user_id, tenant_id),
    KEY idx_notice_recipient_user (user_id, read_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告接收人与阅读记录';

-- 站内消息类别字段的说明
ALTER TABLE sys_message MODIFY COLUMN category VARCHAR(16) NOT NULL COMMENT '类别(字典 sys_message_category) APPROVAL审批 REMIND提醒 ALERT预警 SYSTEM系统 NOTICE公告';

-- 2. 字典类型（ID 已被占用时按自增 ID 插入）
INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT g.id, g.name, g.type, g.kind, 1, g.remark, NOW(), NOW()
FROM (
    SELECT 188 AS id, '公告状态' AS name, 'sys_notice_status' AS type, 'SYSTEM' AS kind, '通知公告的状态：草稿、已发布、已撤回' AS remark
    UNION ALL SELECT 189, '公告发布范围', 'sys_notice_scope', 'SYSTEM', '通知公告发给全员、指定部门（含下级部门）或指定角色'
    UNION ALL SELECT 190, '公告类型', 'sys_notice_type', 'BUSINESS', '通知公告的分类'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = g.type OR x.id = g.id);
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT g.name, g.type, g.kind, 1, g.remark, NOW(), NOW()
FROM (
    SELECT '公告状态' AS name, 'sys_notice_status' AS type, 'SYSTEM' AS kind, '通知公告的状态：草稿、已发布、已撤回' AS remark
    UNION ALL SELECT '公告发布范围', 'sys_notice_scope', 'SYSTEM', '通知公告发给全员、指定部门（含下级部门）或指定角色'
    UNION ALL SELECT '公告类型', 'sys_notice_type', 'BUSINESS', '通知公告的分类'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.type = g.type);

-- 系统字典的取值（平台一份，全部内置）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, d.dict_type, d.dict_label, d.dict_value, d.tag_color, d.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'sys_notice_status' AS dict_type, '草稿' AS dict_label, 'DRAFT' AS dict_value, 'default' AS tag_color, 1 AS sort
    UNION ALL SELECT 'sys_notice_status', '已发布', 'PUBLISHED', 'green', 2
    UNION ALL SELECT 'sys_notice_status', '已撤回', 'WITHDRAWN', 'orange', 3
    UNION ALL SELECT 'sys_notice_scope', '全员', 'ALL', 'blue', 1
    UNION ALL SELECT 'sys_notice_scope', '指定部门', 'DEPT', 'cyan', 2
    UNION ALL SELECT 'sys_notice_scope', '指定角色', 'ROLE', 'purple', 3
    UNION ALL SELECT 'sys_message_category', '公告', 'NOTICE', 'cyan', 5
) d
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = 1 AND x.dict_type = d.dict_type AND x.dict_value = d.dict_value);

-- 业务字典：平台和已有租户各一份
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT t.id, 'sys_notice_type', d.dict_label, d.dict_value, d.tag_color, d.sort, 1, 0, NOW(), NOW()
FROM sys_tenant t
JOIN (
    SELECT '通知' AS dict_label, 'NOTICE' AS dict_value, 'blue' AS tag_color, 1 AS sort
    UNION ALL SELECT '公告', 'ANNOUNCE', 'green', 2
    UNION ALL SELECT '制度', 'POLICY', 'purple', 3
    UNION ALL SELECT '活动', 'EVENT', 'orange', 4
) d ON 1 = 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.tenant_id = t.id AND x.dict_type = 'sys_notice_type' AND x.dict_value = d.dict_value);

-- 3. 菜单：系统管理 → 通知公告
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT g.id, g.parent_id, g.name, g.type, g.path, g.component, g.perms, g.icon, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 353 AS id, 1 AS parent_id, '通知公告' AS name, 'C' AS type, 'notice' AS path, 'system/notice/index' AS component,
           'sys:notice:list' AS perms, 'NotificationOutlined' AS icon, 14 AS sort
    UNION ALL SELECT 3530, 353, '公告新增', 'F', NULL, NULL, 'sys:notice:add', NULL, 1
    UNION ALL SELECT 3531, 353, '公告修改', 'F', NULL, NULL, 'sys:notice:edit', NULL, 2
    UNION ALL SELECT 3532, 353, '公告删除', 'F', NULL, NULL, 'sys:notice:remove', NULL, 3
    UNION ALL SELECT 3533, 353, '公告发布与撤回', 'F', NULL, NULL, 'sys:notice:publish', NULL, 4
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.id = g.id);

-- 套餐：包含系统管理的套餐加入通知公告（含按钮）
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT p.package_id, m.id
FROM (SELECT DISTINCT package_id FROM sys_tenant_package_menu WHERE menu_id = 1) p
JOIN sys_menu m ON m.id IN (353, 3530, 3531, 3532, 3533)
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = m.id);

-- 其他租户的管理员（菜单与套餐一致）：有系统管理的加入通知公告
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (353, 3530, 3531, 3532, 3533)
WHERE r.code = 'admin' AND r.tenant_id <> 1
  AND EXISTS (SELECT 1 FROM sys_role_menu y WHERE y.role_id = r.id AND y.menu_id = 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = m.id);

-- 人事专员：通知公告全部权限（含系统管理目录）；普通用户：只读
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, g.menu_id
FROM sys_role r
JOIN (SELECT 'hr' AS code, 1 AS menu_id UNION ALL SELECT 'hr', 353 UNION ALL SELECT 'hr', 3530 UNION ALL SELECT 'hr', 3531
      UNION ALL SELECT 'hr', 3532 UNION ALL SELECT 'hr', 3533
      UNION ALL SELECT 'common', 1 UNION ALL SELECT 'common', 353) g ON g.code = r.code
WHERE (r.tenant_id = 1
       OR EXISTS (SELECT 1 FROM sys_tenant t WHERE t.id = r.tenant_id AND t.package_id IS NULL)
       OR EXISTS (SELECT 1 FROM sys_tenant t JOIN sys_tenant_package_menu pm ON pm.package_id = t.package_id
                  WHERE t.id = r.tenant_id AND pm.menu_id = g.menu_id))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = r.id AND x.menu_id = g.menu_id);
