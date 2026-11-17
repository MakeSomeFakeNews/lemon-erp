-- ====================================================
-- 在途调拨的分批验收与差异处理（issue #124，参考 #127）。可重复执行
-- 1. 调拨单：预计到达日期 expected_date、原调拨单 origin_id（补发 / 多发补调关联原单）；状态增加 PARTIAL 部分到货、
--    EXCEPTION 验收异常；明细增加在途进度 received_qty 已验收、diff_qty 差异待处理、returned_qty 退回、lost_qty 损耗。
--    库存流水增加业务日期 biz_date（在途调拨的各次验收、退回按各自的日期统计，原来按单据的收货日期 receive_date）
-- 2. 新表：调拨验收单 wms_transfer_receipt / wms_transfer_receipt_item、差异 wms_transfer_diff
-- 3. 已有数据：已收货（已完成）的在途调拨视为全部验收完成（明细已验收 = 数量，补一张验收单，单号 DR + 调拨单号），
--    调入流水的业务日期回填为原收货日期；在途（未收货）的按新流程继续，剩余全部为未到量，不需要处理
-- 4. 字典：单据状态增加「部分到货」「验收异常」，新增调拨差异类型 wms_transfer_diff_type、调拨差异处理 wms_transfer_resolution
-- 5. 按钮：「调拨收货」改名「调拨验收入库」；新增「调拨差异处理」（1326，wms:transfer:resolve），
--    包含调拨单页面的套餐加入，有调拨验收入库权限的角色（各租户的仓库管理员、租户管理员及自定义角色）同时授权
-- ====================================================

-- ---------------------------------------------------- 1. 字段

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_flow' AND COLUMN_NAME = 'biz_date');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_flow ADD COLUMN biz_date DATE DEFAULT NULL COMMENT ''业务日期：在途调拨的验收入库、退回、让步接收按操作日期，为空取单据的业务日期'' AFTER cost_estimated', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'expected_date');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN expected_date DATE DEFAULT NULL COMMENT ''预计到达日期（在途调拨，可不填），超过仍未完结时提醒'' AFTER in_transit', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND COLUMN_NAME = 'origin_id');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD COLUMN origin_id BIGINT DEFAULT NULL COMMENT ''原调拨单：补发、多发补调的调拨单关联在途调拨原单'' AFTER expected_date', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc' AND INDEX_NAME = 'idx_stock_doc_origin');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc ADD KEY idx_stock_doc_origin (origin_id)', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE wms_stock_doc
    MODIFY COLUMN status VARCHAR(16) NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中 TRANSIT在途（在途调拨已调出、未验收） PARTIAL部分到货 EXCEPTION验收异常（有待处理的差异） DONE已完成',
    MODIFY COLUMN in_transit TINYINT NOT NULL DEFAULT 0 COMMENT '在途调拨 1是：确认时只调出，调入仓库分批验收入库（wms_transfer_receipt）',
    MODIFY COLUMN receive_by BIGINT DEFAULT NULL COMMENT '最近一次验收人（在途调拨）',
    MODIFY COLUMN receive_date DATE DEFAULT NULL COMMENT '最近一次验收日期（在途调拨）；流水的业务日期见 wms_stock_flow.biz_date',
    MODIFY COLUMN receive_time DATETIME DEFAULT NULL COMMENT '最近一次验收时间';

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'received_qty');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN received_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT ''在途调拨：已验收入库数量（含让步接收）'' AFTER serial_nos', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'diff_qty');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN diff_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT ''在途调拨：登记了差异、待处理的数量（不含多发）'' AFTER received_qty', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'returned_qty');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN returned_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT ''在途调拨：退回调出仓库的数量'' AFTER diff_qty', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_stock_doc_item' AND COLUMN_NAME = 'lost_qty');
SET @s = IF(@c = 0, 'ALTER TABLE wms_stock_doc_item ADD COLUMN lost_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT ''在途调拨：确认损耗的数量（生成报废出库单）。未到 = 数量 - 已验收 - 差异待处理 - 退回 - 损耗'' AFTER returned_qty', 'SELECT 1');
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------- 2. 新表

CREATE TABLE IF NOT EXISTS wms_transfer_receipt (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    receipt_no   VARCHAR(32)    NOT NULL COMMENT '验收单号',
    doc_id       BIGINT         NOT NULL COMMENT '在途调拨单',
    biz_date     DATE           NOT NULL COMMENT '验收日期（调入流水的业务日期）',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '验收入库数量合计',
    diff_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '登记差异的数量合计（含多发）',
    logistics_no VARCHAR(64)             DEFAULT NULL COMMENT '物流单号',
    plate_no     VARCHAR(32)             DEFAULT NULL COMMENT '车牌号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '验收人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_receipt_no (receipt_no, tenant_id),
    KEY idx_transfer_receipt_doc (doc_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调拨验收单：在途调拨的调入仓库分批验收，每次一张';

CREATE TABLE IF NOT EXISTS wms_transfer_receipt_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    receipt_id  BIGINT         NOT NULL COMMENT '验收单',
    doc_id      BIGINT         NOT NULL COMMENT '在途调拨单',
    item_id     BIGINT         NOT NULL COMMENT '调拨单明细',
    material_id BIGINT         NOT NULL COMMENT '物料',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '验收入库数量',
    serial_nos  TEXT COMMENT '验收入库的序列号，逗号分隔',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_transfer_receipt_item (receipt_id),
    KEY idx_transfer_receipt_item_doc (doc_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调拨验收单明细';

CREATE TABLE IF NOT EXISTS wms_transfer_diff (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id         BIGINT         NOT NULL COMMENT '在途调拨单',
    item_id        BIGINT         NOT NULL COMMENT '调拨单明细',
    receipt_id     BIGINT                  DEFAULT NULL COMMENT '登记差异的验收单，处理未到量时为空',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    diff_type      VARCHAR(16)    NOT NULL COMMENT '差异类型(字典 wms_transfer_diff_type) SHORT少到 DAMAGED破损 WRONG错发 OVER多发 LOST在途丢失',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '差异数量',
    serial_nos     TEXT COMMENT '差异的序列号，逗号分隔',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '差异说明',
    resolution     VARCHAR(16)             DEFAULT NULL COMMENT '处理方式(字典 wms_transfer_resolution) RETURN退回调出仓 LOSS确认损耗 ACCEPT让步接收，为空表示待处理',
    resolve_date   DATE                    DEFAULT NULL COMMENT '处理日期（退回、让步接收的流水业务日期，损耗的报废出库单业务日期）',
    resolve_by     BIGINT                  DEFAULT NULL COMMENT '处理人',
    resolve_time   DATETIME                DEFAULT NULL COMMENT '处理时间',
    resolve_remark VARCHAR(255)            DEFAULT NULL COMMENT '处理说明',
    ref_doc_id     BIGINT                  DEFAULT NULL COMMENT '处理生成的单据：确认损耗的报废出库单、多发让步接收的调拨单草稿',
    create_by      BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_transfer_diff_doc (doc_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在途调拨的差异：验收时登记，处理（退回 / 损耗 / 让步接收）后关闭；部分处理时拆分为已处理、待处理两条';

-- ---------------------------------------------------- 3. 已有数据

-- 已收货的在途调拨：调入流水的业务日期 = 原收货日期（原来报表按 receive_date 统计调入流水，口径不变）
UPDATE wms_stock_flow f
JOIN wms_stock_doc d ON d.id = f.doc_id
SET f.biz_date = d.receive_date
WHERE f.biz_type = 'TRANSFER' AND f.qty > 0 AND f.biz_date IS NULL
  AND d.kind = 'TRANSFER' AND d.in_transit = 1 AND d.receive_date IS NOT NULL;

-- 已完成的在途调拨视为全部验收完成
UPDATE wms_stock_doc_item i
JOIN wms_stock_doc d ON d.id = i.doc_id
SET i.received_qty = i.qty
WHERE d.kind = 'TRANSFER' AND d.in_transit = 1 AND d.status = 'DONE'
  AND i.received_qty = 0 AND i.returned_qty = 0 AND i.lost_qty = 0;

-- 并补一张验收单（单号 DR + 调拨单号），验收记录中可以看到原来的收货
INSERT INTO wms_transfer_receipt (tenant_id, receipt_no, doc_id, biz_date, total_qty, diff_qty, remark, create_by, create_time, update_time)
SELECT d.tenant_id, LEFT(CONCAT('DR', d.doc_no), 32), d.id, COALESCE(d.receive_date, d.biz_date), d.total_qty, 0,
       '升级前的在途调拨收货', d.receive_by, COALESCE(d.receive_time, d.update_time, NOW()), NOW()
FROM wms_stock_doc d
WHERE d.kind = 'TRANSFER' AND d.in_transit = 1 AND d.status = 'DONE' AND d.red_of IS NULL
  AND NOT EXISTS (SELECT 1 FROM wms_transfer_receipt r WHERE r.doc_id = d.id);

INSERT INTO wms_transfer_receipt_item (tenant_id, receipt_id, doc_id, item_id, material_id, qty, serial_nos, create_time, update_time)
SELECT i.tenant_id, r.id, i.doc_id, i.id, i.material_id, i.qty, i.serial_nos, r.create_time, NOW()
FROM wms_transfer_receipt r
JOIN wms_stock_doc d ON d.id = r.doc_id
JOIN wms_stock_doc_item i ON i.doc_id = r.doc_id
WHERE r.receipt_no = LEFT(CONCAT('DR', d.doc_no), 32)
  AND NOT EXISTS (SELECT 1 FROM wms_transfer_receipt_item x WHERE x.receipt_id = r.id);

-- ---------------------------------------------------- 4. 字典

INSERT INTO sys_dict_type (id, name, type, kind, status, remark, create_time, update_time)
SELECT g.id, g.name, g.type, 'SYSTEM', 1, g.remark, NOW(), NOW()
FROM (
    SELECT 210 AS id, '调拨差异类型' AS name, 'wms_transfer_diff_type' AS type, '在途调拨验收时登记的差异：少到、破损、错发、多发，处理未到量时为在途丢失' AS remark
    UNION ALL SELECT 211, '调拨差异处理', 'wms_transfer_resolution', '在途调拨差异的处理方式：退回调出仓、确认损耗（报废出库）、让步接收（入调入仓）'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = g.type OR t.id = g.id);
-- 编号已被占用时不指定编号
INSERT INTO sys_dict_type (name, type, kind, status, remark, create_time, update_time)
SELECT g.name, g.type, 'SYSTEM', 1, g.remark, NOW(), NOW()
FROM (
    SELECT '调拨差异类型' AS name, 'wms_transfer_diff_type' AS type, '在途调拨验收时登记的差异：少到、破损、错发、多发，处理未到量时为在途丢失' AS remark
    UNION ALL SELECT '调拨差异处理', 'wms_transfer_resolution', '在途调拨差异的处理方式：退回调出仓、确认损耗（报废出库）、让步接收（入调入仓）'
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type t WHERE t.type = g.type);

INSERT INTO sys_dict_data (tenant_id, dict_type, dict_label, dict_value, tag_color, sort, status, builtin, create_time, update_time)
SELECT 1, g.dict_type, g.dict_label, g.dict_value, g.tag_color, g.sort, 1, 1, NOW(), NOW()
FROM (
    SELECT 'wms_doc_status' AS dict_type, '部分到货' AS dict_label, 'PARTIAL' AS dict_value, 'cyan' AS tag_color, 5 AS sort
    UNION ALL SELECT 'wms_doc_status', '验收异常', 'EXCEPTION', 'red', 6
    UNION ALL SELECT 'wms_transfer_diff_type', '少到', 'SHORT', 'orange', 1
    UNION ALL SELECT 'wms_transfer_diff_type', '破损', 'DAMAGED', 'red', 2
    UNION ALL SELECT 'wms_transfer_diff_type', '错发', 'WRONG', 'volcano', 3
    UNION ALL SELECT 'wms_transfer_diff_type', '多发', 'OVER', 'blue', 4
    UNION ALL SELECT 'wms_transfer_diff_type', '在途丢失', 'LOST', 'magenta', 5
    UNION ALL SELECT 'wms_transfer_resolution', '退回调出仓', 'RETURN', 'blue', 1
    UNION ALL SELECT 'wms_transfer_resolution', '确认损耗', 'LOSS', 'red', 2
    UNION ALL SELECT 'wms_transfer_resolution', '让步接收', 'ACCEPT', 'green', 3
) g
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.dict_type = g.dict_type AND x.dict_value = g.dict_value AND x.tenant_id = 1);

-- ---------------------------------------------------- 5. 按钮

UPDATE sys_menu SET name = '调拨验收入库' WHERE id = 1325 AND perms = 'wms:transfer:receive' AND name = '调拨收货';

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time)
SELECT 1326, 132, '调拨差异处理', 'F', NULL, NULL, 'wms:transfer:resolve', NULL, 7, 1, 1, NOW(), NOW()
FROM (SELECT 1 AS x) d
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = 1326);

-- 包含调拨单页面的套餐加入
INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT DISTINCT p.package_id, 1326
FROM sys_tenant_package_menu p
WHERE p.menu_id = 132
  AND NOT EXISTS (SELECT 1 FROM sys_tenant_package_menu x WHERE x.package_id = p.package_id AND x.menu_id = 1326);

-- 有调拨验收入库权限的角色（各租户的仓库管理员、租户管理员及自定义角色）同时授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 1326
FROM sys_role_menu rm
WHERE rm.menu_id = 1325
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = 1326);
