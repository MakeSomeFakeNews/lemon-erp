-- ====================================================
-- 整单优惠 / 抹零（issue #29）：销售 / 采购订单（含直接开单生成的隐式订单）、销售报价单增加整单优惠金额。
-- 优惠在促销、返利抵扣之后按行金额比例分摊到各行金额，已有数据为 0，不需要换算。可重复执行
-- ====================================================

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order' AND COLUMN_NAME = 'whole_discount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order ADD COLUMN whole_discount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''整单优惠（含税，含抹零）：促销、返利之后按行金额分摊到各行金额，已从金额中扣除'' AFTER rebate_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_quotation' AND COLUMN_NAME = 'whole_discount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_quotation ADD COLUMN whole_discount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''整单优惠（含税，含抹零），已从价税合计中扣除；转订单时分摊到订单各行'' AFTER tax_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------
-- 分批出入库的尾差、订单变更重新分摊（issue #29 遗留）：
-- 订单明细记录已出入库金额（最后一批取 行金额 − 已出入库金额）、已退款金额、分摊的整单优惠及已出入库部分的分摊额；
-- 订单变更单记录变更前后的整单优惠。已有明细的已出入库金额按历史出入库单回填（外币按单据汇率折回原币）
-- ----------------------------------------------------

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_item' AND COLUMN_NAME = 'done_amount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_item ADD COLUMN done_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''已出入库金额（订单币种，补发的退货扣减）：最后一批取 行金额 − 已出入库金额'' AFTER replenish_qty', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_item' AND COLUMN_NAME = 'return_amount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_item ADD COLUMN return_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''已退款的退货金额（订单币种，不补发的退货）'' AFTER done_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_item' AND COLUMN_NAME = 'discount_share');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_item ADD COLUMN discount_share DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''分摊到本行的整单优惠（行金额已扣除）'' AFTER return_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_item' AND COLUMN_NAME = 'done_share');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_item ADD COLUMN done_share DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''已出入库部分已分摊的整单优惠（订单变更时不变，剩余的重新分摊）'' AFTER discount_share', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_change' AND COLUMN_NAME = 'old_whole_discount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_change ADD COLUMN old_whole_discount DECIMAL(18, 2) DEFAULT NULL COMMENT ''变更前的整单优惠'' AFTER new_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_change' AND COLUMN_NAME = 'new_whole_discount');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_change ADD COLUMN new_whole_discount DECIMAL(18, 2) DEFAULT NULL COMMENT ''变更后的整单优惠（按变更后未出入库部分重新分摊）'' AFTER old_whole_discount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填已出入库金额 = 出入库金额 − 补发的退货金额，已退款金额 = 退款的退货金额；
-- 直接退货的隐式订单没有出库 / 入库单（视为已全部出入库），出入库金额取行金额。
-- 只处理两个金额都还是 0 的明细（已回填或升级后发生过出入库的不再处理），可重复执行
UPDATE scm_order_item i
JOIN (SELECT ti.order_item_id,
             SUM(CASE WHEN d.kind IN ('PURCHASE_IN', 'SALES_OUT') THEN ti.amount / COALESCE(NULLIF(d.exchange_rate, 0), 1) ELSE 0 END) AS out_amount,
             SUM(CASE WHEN d.kind IN ('PURCHASE_IN', 'SALES_OUT') THEN 1 ELSE 0 END) AS out_count,
             SUM(CASE WHEN d.kind IN ('PURCHASE_RETURN', 'SALES_RETURN') AND d.replenish = 1 THEN ti.amount / COALESCE(NULLIF(d.exchange_rate, 0), 1) ELSE 0 END) AS replenish_amount,
             SUM(CASE WHEN d.kind IN ('PURCHASE_RETURN', 'SALES_RETURN') AND d.replenish = 0 THEN ti.amount / COALESCE(NULLIF(d.exchange_rate, 0), 1) ELSE 0 END) AS refund_amount
      FROM scm_trade_doc_item ti
      JOIN scm_trade_doc d ON d.id = ti.doc_id
      GROUP BY ti.order_item_id) t ON t.order_item_id = i.id
SET i.done_amount   = ROUND(CASE WHEN t.out_count = 0 THEN i.amount ELSE t.out_amount END - t.replenish_amount, 2),
    i.return_amount = ROUND(t.refund_amount, 2)
WHERE i.done_amount = 0
  AND i.return_amount = 0;

-- ----------------------------------------------------
-- 订单关闭时的尾差调整（issue #29 遗留）：部分出入库的订单行关闭时，已出入库金额按 行金额 × 已出入库数量 / 订单数量
-- （含整单优惠分摊）调整，差额记入订单 / 明细的 close_adjust 并生成应收 / 应付调整单（来源 ORDER_ADJUST）。
-- 升级前已关闭的订单不追溯调整，均为 0。可重复执行
-- ----------------------------------------------------

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order' AND COLUMN_NAME = 'close_adjust');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order ADD COLUMN close_adjust DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''关闭时的尾差调整（订单币种）：部分出入库的行按 行金额 × 已出入库数量 / 订单数量 调整已出入库金额，生成应收 / 应付调整单'' AFTER whole_discount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_item' AND COLUMN_NAME = 'close_adjust');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_item ADD COLUMN close_adjust DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''订单关闭时本行的尾差调整（订单币种，已计入已出入库金额）'' AFTER done_share', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_order_item' AND COLUMN_NAME = 'close_adjust_net');
SET @s = IF(@c = 0, 'ALTER TABLE scm_order_item ADD COLUMN close_adjust_net DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''尾差调整的不含税金额（本位币）：采购订单计入调整单所在期间的月末成本核算，凭证按物料分类取数'' AFTER close_adjust', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 应收应付来源增加「订单尾差调整」（系统字典，平台租户）
INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, 'fin_bill_source', '订单尾差调整', 'ORDER_ADJUST', 'geekblue', 14, 1, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = 'fin_bill_source' AND x.dict_value = 'ORDER_ADJUST' AND x.tenant_id = 1);
