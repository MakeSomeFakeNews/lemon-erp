-- ====================================================
-- 物料资料（issue #94）：商品图片
-- 物料可上传多张图片（新表 wms_material_image，排序最小的为主图），主图 URL 同步到 wms_material.image_url，
-- 列表、下拉、库存查询、小程序显示缩略图。图片存在公开文件目录（lemon.upload.path/material/...，与头像相同），
-- 通过 /api/files/** 直接访问。
-- 复制物料、批次管理的启用 / 停用不需要改库。只给已有的物料表加列并新建一张表，与其他 20261111 脚本没有依赖；可重复执行
-- ====================================================

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'image_url');
SET @s = IF(@c = 0,
            'ALTER TABLE wms_material ADD COLUMN image_url VARCHAR(255) DEFAULT NULL COMMENT ''主图 URL（wms_material_image 排在第一张的图片），由图片维护自动更新'' AFTER variant_text',
            'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS wms_material_image (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT       NOT NULL COMMENT '物料',
    url         VARCHAR(255) NOT NULL COMMENT '图片访问 URL（公开文件 /api/files/material/...）',
    file_name   VARCHAR(255)          DEFAULT NULL COMMENT '原文件名',
    file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，最小的为主图',
    create_by   BIGINT                DEFAULT NULL COMMENT '上传人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_material_image (material_id, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料（商品）图片：排在第一张的为主图，同步到 wms_material.image_url';
