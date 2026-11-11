-- ====================================================
-- 账户调拨（issue #66）。可重复执行
-- 1. fin_fund_transfer 增加经办人 handler_id、票据号码 ticket_no（可空）；fee_bearer 改称「手续费支付账户」（只改注释，取值不变）
-- 2. 系统字典 fin_fee_bearer 改名为「手续费支付账户」，取值名称改为「转出账户支付」「转入账户支付」（只改仍为原名称的）
-- 手续费单独生成凭证分录、收支汇总列出调拨手续费只是程序变化，不需要改库；已有调拨的凭证保持原样
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

-- 1. 表结构
ALTER TABLE fin_fund_transfer MODIFY COLUMN fee_bearer VARCHAR(8) NOT NULL DEFAULT 'FROM' COMMENT '手续费支付账户(字典 fin_fee_bearer) FROM转出账户支付 TO转入账户支付';
CALL lemon_add_column('fin_fund_transfer', 'handler_id', 'handler_id BIGINT DEFAULT NULL COMMENT ''经办人'' AFTER fee_bearer');
CALL lemon_add_column('fin_fund_transfer', 'ticket_no', 'ticket_no VARCHAR(64) DEFAULT NULL COMMENT ''票据号码（转账回单、支票号等）'' AFTER handler_id');

DROP PROCEDURE IF EXISTS lemon_add_column;

-- 2. 字典
UPDATE sys_dict_type SET name = '手续费支付账户', remark = '账户调拨的手续费由转出账户还是转入账户支付', update_time = NOW()
WHERE type = 'fin_fee_bearer' AND name = '手续费承担方';
UPDATE sys_dict_data SET dict_label = '转出账户支付', update_time = NOW()
WHERE dict_type = 'fin_fee_bearer' AND dict_value = 'FROM' AND dict_label = '转出账户';
UPDATE sys_dict_data SET dict_label = '转入账户支付', update_time = NOW()
WHERE dict_type = 'fin_fee_bearer' AND dict_value = 'TO' AND dict_label = '转入账户';
