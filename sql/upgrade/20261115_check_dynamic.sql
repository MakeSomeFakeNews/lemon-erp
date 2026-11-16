-- 动态盘点（issue #115）：盘点单「选择商品」可只列出一段时间内在盘点仓库有过库存变动的物料，
-- 按物料 + 仓库 + 发生时间查询库存流水，增加索引 idx_flow_material_wh。可重复执行

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_flow'
          AND INDEX_NAME = 'idx_flow_material_wh');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_flow ADD KEY idx_flow_material_wh (material_id, warehouse_id, create_time, biz_type)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
