-- ====================================================
-- 工程变更记录批准人（详情的「批准」显示批准人和时间）。可重复执行
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

-- 批准人：审批流程最后一步的审批人，未发布流程时为提交人；已批准的历史变更单没有记录，详情只显示批准时间
CALL lemon_add_column('mes_ecn', 'approve_by', 'approve_by BIGINT DEFAULT NULL COMMENT ''批准人（审批流程最后一步的审批人；未发布流程时为提交人）'' AFTER flow_instance_id');

DROP PROCEDURE IF EXISTS lemon_add_column;
