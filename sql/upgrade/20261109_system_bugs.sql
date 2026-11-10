-- ====================================================
-- 系统缺陷修复（生成演示数据时发现）。可重复执行
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

-- 在途调拨收货可填写收货日期，调入流水按收货日期统计；历史单据取收货时间的日期
CALL lemon_add_column('wms_stock_doc', 'receive_date', 'receive_date DATE DEFAULT NULL COMMENT ''调入收货日期：在途调拨收货的业务日期，调入流水按此日期统计'' AFTER receive_by');

-- 工作中心所属车间：工单的数据权限部门取第一道工序工作中心的所属车间（车间主管按「本部门」可以看到计划员下达的工单）
CALL lemon_add_column('mes_work_center', 'dept_id', 'dept_id BIGINT DEFAULT NULL COMMENT ''所属车间（部门）：工单的数据权限部门取第一道工序工作中心的所属车间'' AFTER work_days');

DROP PROCEDURE IF EXISTS lemon_add_column;

UPDATE wms_stock_doc SET receive_date = CAST(receive_time AS DATE) WHERE receive_date IS NULL AND receive_time IS NOT NULL;

-- 金额为 0 的应收应付单（整单赠品等）不生成凭证，状态为已核销
UPDATE fin_bill SET status = 'SETTLED' WHERE amount = 0 AND settled_amount = 0 AND status <> 'SETTLED';
