-- ====================================================
-- 虚拟商品（服务项目，issue #105）
-- 物料增加「虚拟商品」标识（wms_material.is_virtual，已有物料为 0 实物）：如洗车、安装调试、延保等服务，
-- 只用于销售开单和收入统计，销售出库不扣库存、不生成库存流水、出库成本为 0；不能采购、入库、盘点、调拨、用于 BOM 和生产。
-- 只给物料表加列，与其他脚本没有依赖；可重复执行
-- ====================================================

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_material' AND COLUMN_NAME = 'is_virtual');
SET @s = IF(@c = 0,
            'ALTER TABLE wms_material ADD COLUMN is_virtual TINYINT NOT NULL DEFAULT 0 COMMENT ''是否虚拟商品（服务项目）1是：只用于销售开单和收入统计，不管库存，出库成本为 0，不能采购、入库和用于生产'' AFTER brand',
            'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
