-- ====================================================
-- 界面个性化：用户个人设置（新表 sys_user_setting）
-- 每个用户的界面设置（主题模式、主题色、布局、多标签页、紧凑模式等）保存在服务端，换浏览器 / 设备登录后恢复；
-- 只能本人读写，内容为 JSON 对象。只新建一张表，与其他 20261111 脚本没有依赖；可重复执行
-- ====================================================

CREATE TABLE IF NOT EXISTS sys_user_setting (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id       BIGINT      NOT NULL COMMENT '用户ID',
    setting_key   VARCHAR(32) NOT NULL COMMENT '设置项标识：ui 界面设置（主题、主题色、布局、多标签页、紧凑模式等）',
    setting_value TEXT                 DEFAULT NULL COMMENT '设置内容（JSON 对象）',
    create_time   DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_setting (user_id, setting_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户个人设置（只能本人读写）';
