-- ====================================================
-- 采购入库的订单优惠、运费入账方式、承运商（issue #68 的 A、B、C 及评论）。可重复执行
-- 1. 引用订单的采购入库可以修改本单的订单优惠：记录与按订单分摊之差（scm_trade_doc.discount_adjust，已有单据为 0）
-- 2. 本公司承担的采购运费可按业务参数计入存货成本（默认，与原来一致）或当期费用：入库单记录入账方式（scm_trade_doc.freight_cost），
--    已有带运费的入库单都是计入存货成本。业务参数「本公司承担的采购运费计入」（scm.purchaseFreight.cost）和
--    凭证科目「采购运费（计入当期费用时）」使用默认值，不需要改库
-- 3. 供应商资料增加「物流公司」标识和营运路线（scm_supplier.logistics / routes），采购运费、发运单的承运商只列出物流公司；
--    已经被用作承运商（采购入库的承运商、发运单的运费结算供应商）的供应商标记为物流公司
-- ====================================================

-- ---------------------------------------------------- 1、2. 出入库单的订单优惠差额、采购运费入账方式

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'discount_adjust');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN discount_adjust DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT ''采购入库修改的订单优惠与按订单分摊之差（本位币，含税），0为与订单一致'' AFTER discount_amount', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE scm_trade_doc MODIFY COLUMN freight DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '采购运费（仅采购入库）';

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_trade_doc' AND COLUMN_NAME = 'freight_cost');
SET @s = IF(@c = 0, 'ALTER TABLE scm_trade_doc ADD COLUMN freight_cost VARCHAR(16) DEFAULT NULL COMMENT ''采购运费入账方式 INVENTORY计入存货成本 EXPENSE计入当期费用（本公司承担时按业务参数）'' AFTER freight_bearer', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已有带运费的采购入库单都计入了存货成本
UPDATE scm_trade_doc SET freight_cost = 'INVENTORY' WHERE freight > 0 AND freight_cost IS NULL;

-- ---------------------------------------------------- 3. 供应商的物流公司标识、营运路线

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_supplier' AND COLUMN_NAME = 'logistics');
SET @s = IF(@c = 0, 'ALTER TABLE scm_supplier ADD COLUMN logistics TINYINT NOT NULL DEFAULT 0 COMMENT ''物流公司（承运商） 1是 0否：采购运费、发运单的承运商只列出物流公司'' AFTER payment_days', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scm_supplier' AND COLUMN_NAME = 'routes');
SET @s = IF(@c = 0, 'ALTER TABLE scm_supplier ADD COLUMN routes VARCHAR(255) DEFAULT NULL COMMENT ''营运路线（物流公司）'' AFTER logistics', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已经被用作承运商的供应商（采购入库本公司承担运费的承运商、发运单的运费结算供应商）标记为物流公司
UPDATE scm_supplier s SET s.logistics = 1
WHERE s.logistics = 0
  AND (EXISTS (SELECT 1 FROM scm_trade_doc d WHERE d.carrier_supplier_id = s.id AND d.tenant_id = s.tenant_id)
       OR EXISTS (SELECT 1 FROM scm_shipment h WHERE h.carrier_supplier_id = s.id AND h.tenant_id = s.tenant_id));
