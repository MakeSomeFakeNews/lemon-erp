-- 柠檬ERP 初始化脚本（MySQL 8）
-- 默认账号：admin / admin123（超级管理员）、test / admin123（普通用户，只有查看权限，数据权限为本部门及以下）
-- 工作流引擎的 ACT_ 开头的表由 Flowable 启动时自动创建，不在本脚本中

DROP TABLE IF EXISTS bi_alert_rule;
DROP TABLE IF EXISTS bi_dashboard;
DROP TABLE IF EXISTS bi_report;
DROP TABLE IF EXISTS bi_etl_log;
DROP TABLE IF EXISTS bi_fact_project;
DROP TABLE IF EXISTS bi_fact_crm;
DROP TABLE IF EXISTS bi_fact_hr;
DROP TABLE IF EXISTS bi_fact_finance;
DROP TABLE IF EXISTS bi_fact_production;
DROP TABLE IF EXISTS bi_fact_inventory;
DROP TABLE IF EXISTS bi_fact_purchase;
DROP TABLE IF EXISTS bi_fact_sales;
DROP TABLE IF EXISTS pm_billing;
DROP TABLE IF EXISTS pm_revenue;
DROP TABLE IF EXISTS pm_cost;
DROP TABLE IF EXISTS pm_budget;
DROP TABLE IF EXISTS pm_timesheet;
DROP TABLE IF EXISTS pm_task;
DROP TABLE IF EXISTS pm_member;
DROP TABLE IF EXISTS pm_project;
DROP TABLE IF EXISTS crm_ticket_part;
DROP TABLE IF EXISTS crm_ticket_log;
DROP TABLE IF EXISTS crm_ticket;
DROP TABLE IF EXISTS crm_campaign;
DROP TABLE IF EXISTS crm_stage_log;
DROP TABLE IF EXISTS crm_opportunity_item;
DROP TABLE IF EXISTS crm_collection_plan;
DROP TABLE IF EXISTS crm_contract;
DROP TABLE IF EXISTS crm_opportunity;
DROP TABLE IF EXISTS crm_followup;
DROP TABLE IF EXISTS crm_lead;
DROP TABLE IF EXISTS crm_contact;
DROP TABLE IF EXISTS crm_customer;
DROP TABLE IF EXISTS hr_perf_score;
DROP TABLE IF EXISTS hr_perf_appraisal;
DROP TABLE IF EXISTS hr_perf_indicator;
DROP TABLE IF EXISTS hr_perf_plan;
DROP TABLE IF EXISTS hr_payroll_item;
DROP TABLE IF EXISTS hr_payroll;
DROP TABLE IF EXISTS hr_salary_history;
DROP TABLE IF EXISTS hr_tax_deduction;
DROP TABLE IF EXISTS hr_social_scheme;
DROP TABLE IF EXISTS hr_salary;
DROP TABLE IF EXISTS hr_leave;
DROP TABLE IF EXISTS hr_attendance;
DROP TABLE IF EXISTS hr_holiday;
DROP TABLE IF EXISTS hr_shift;
DROP TABLE IF EXISTS hr_training_record;
DROP TABLE IF EXISTS hr_training;
DROP TABLE IF EXISTS hr_interview;
DROP TABLE IF EXISTS hr_candidate;
DROP TABLE IF EXISTS hr_recruit;
DROP TABLE IF EXISTS hr_contract;
DROP TABLE IF EXISTS hr_employee_change;
DROP TABLE IF EXISTS hr_employee;
DROP TABLE IF EXISTS hr_position;
DROP TABLE IF EXISTS fin_commission_line;
DROP TABLE IF EXISTS fin_commission;
DROP TABLE IF EXISTS fin_commission_rule;
DROP TABLE IF EXISTS fin_account_rule;
DROP TABLE IF EXISTS fin_budget;
DROP TABLE IF EXISTS fin_note_log;
DROP TABLE IF EXISTS scm_delivery_package_item;
DROP TABLE IF EXISTS scm_delivery_package;
DROP TABLE IF EXISTS scm_wave;
DROP TABLE IF EXISTS scm_channel_report_item;
DROP TABLE IF EXISTS scm_channel_report;
DROP TABLE IF EXISTS scm_rebate_tier;
DROP TABLE IF EXISTS scm_rebate;
DROP TABLE IF EXISTS scm_promotion_usage;
DROP TABLE IF EXISTS scm_promotion;
DROP TABLE IF EXISTS fin_std_cost;
DROP TABLE IF EXISTS fin_std_cost_version;
DROP TABLE IF EXISTS fin_consumable_use_item;
DROP TABLE IF EXISTS fin_consumable_use;
DROP TABLE IF EXISTS fin_asset_check_item;
DROP TABLE IF EXISTS fin_asset_check;
DROP TABLE IF EXISTS fin_asset_move;
DROP TABLE IF EXISTS mes_tech_doc;
DROP TABLE IF EXISTS mes_ecn;
DROP TABLE IF EXISTS fin_supplier_statement_line;
DROP TABLE IF EXISTS fin_supplier_statement;
DROP TABLE IF EXISTS scm_asn_item;
DROP TABLE IF EXISTS scm_asn;
DROP TABLE IF EXISTS scm_bid_item;
DROP TABLE IF EXISTS scm_bid;
DROP TABLE IF EXISTS scm_tender_supplier;
DROP TABLE IF EXISTS scm_tender_item;
DROP TABLE IF EXISTS scm_tender;
DROP TABLE IF EXISTS scm_purchase_contract_item;
DROP TABLE IF EXISTS scm_purchase_contract;
DROP TABLE IF EXISTS qms_supplier_claim;
DROP TABLE IF EXISTS qms_capa_step;
DROP TABLE IF EXISTS qms_capa;
DROP TABLE IF EXISTS qms_ncr;
DROP TABLE IF EXISTS qms_inspection_item;
DROP TABLE IF EXISTS qms_inspection_plan_item;
DROP TABLE IF EXISTS qms_inspection_plan;
DROP TABLE IF EXISTS fin_note;
DROP TABLE IF EXISTS fin_fund_transfer;
DROP TABLE IF EXISTS fin_fund_plan_item;
DROP TABLE IF EXISTS fin_wo_cost;
DROP TABLE IF EXISTS fin_cost_layer;
DROP TABLE IF EXISTS fin_material_cost;
DROP TABLE IF EXISTS fin_cost_run;
DROP TABLE IF EXISTS fin_depreciation;
DROP TABLE IF EXISTS fin_asset;
DROP TABLE IF EXISTS fin_invoice;
DROP TABLE IF EXISTS fin_invoice_line;
DROP TABLE IF EXISTS fin_invoice_item;
DROP TABLE IF EXISTS fin_advance;
DROP TABLE IF EXISTS fin_advance_apply;
DROP TABLE IF EXISTS fin_bank_statement;
DROP TABLE IF EXISTS fin_expense_claim;
DROP TABLE IF EXISTS fin_expense_claim_item;
DROP TABLE IF EXISTS fin_loan;
DROP TABLE IF EXISTS fin_loan_repay;
DROP TABLE IF EXISTS fin_settlement_item;
DROP TABLE IF EXISTS fin_settlement;
DROP TABLE IF EXISTS fin_bill;
DROP TABLE IF EXISTS fin_voucher_entry;
DROP TABLE IF EXISTS fin_voucher;
DROP TABLE IF EXISTS fin_period;
DROP TABLE IF EXISTS fin_account_opening;
DROP TABLE IF EXISTS fin_exchange_rate;
DROP TABLE IF EXISTS fin_currency;
DROP TABLE IF EXISTS fin_account;
DROP TABLE IF EXISTS mes_wo_supplement_item;
DROP TABLE IF EXISTS mes_wo_supplement;
DROP TABLE IF EXISTS mes_material_substitute;
DROP TABLE IF EXISTS mes_material_plan;
DROP TABLE IF EXISTS mes_mold_log;
DROP TABLE IF EXISTS mes_mold_product;
DROP TABLE IF EXISTS mes_mold;
DROP TABLE IF EXISTS mes_equipment_stop;
DROP TABLE IF EXISTS mes_maintenance;
DROP TABLE IF EXISTS mes_equipment;
DROP TABLE IF EXISTS mes_inspection;
DROP TABLE IF EXISTS mes_wo_doc_item;
DROP TABLE IF EXISTS mes_wo_doc;
DROP TABLE IF EXISTS mes_report;
DROP TABLE IF EXISTS mes_wo_op;
DROP TABLE IF EXISTS mes_wo_material;
DROP TABLE IF EXISTS mes_work_order;
DROP TABLE IF EXISTS mes_mrp_result;
DROP TABLE IF EXISTS mes_outsource_consume;
DROP TABLE IF EXISTS mes_outsource_material;
DROP TABLE IF EXISTS mes_forecast;
DROP TABLE IF EXISTS mes_mrp_run;
DROP TABLE IF EXISTS mes_plan;
DROP TABLE IF EXISTS mes_bom_op;
DROP TABLE IF EXISTS mes_bom_item;
DROP TABLE IF EXISTS mes_bom;
DROP TABLE IF EXISTS mes_calendar;
DROP TABLE IF EXISTS mes_work_center;
DROP TABLE IF EXISTS scm_delivery;
DROP TABLE IF EXISTS scm_delivery_item;
DROP TABLE IF EXISTS scm_delivery_pick;
DROP TABLE IF EXISTS scm_shipment_track;
DROP TABLE IF EXISTS scm_shipment;
DROP TABLE IF EXISTS scm_trade_doc_item;
DROP TABLE IF EXISTS scm_trade_doc;
DROP TABLE IF EXISTS scm_order_item;
DROP TABLE IF EXISTS scm_order_change_item;
DROP TABLE IF EXISTS scm_order_change;
DROP TABLE IF EXISTS scm_order;
DROP TABLE IF EXISTS scm_customer;
DROP TABLE IF EXISTS scm_supplier_eval;
DROP TABLE IF EXISTS scm_supplier_price_tier;
DROP TABLE IF EXISTS scm_price_list;
DROP TABLE IF EXISTS scm_price_control;
DROP TABLE IF EXISTS scm_requisition;
DROP TABLE IF EXISTS scm_requisition_item;
DROP TABLE IF EXISTS scm_inquiry;
DROP TABLE IF EXISTS scm_inquiry_item;
DROP TABLE IF EXISTS scm_inquiry_quote;
DROP TABLE IF EXISTS scm_quotation;
DROP TABLE IF EXISTS scm_quotation_item;
DROP TABLE IF EXISTS scm_supplier_price;
DROP TABLE IF EXISTS scm_supplier;
DROP TABLE IF EXISTS wms_doc_sequence;
DROP TABLE IF EXISTS wms_check_item;
DROP TABLE IF EXISTS wms_check;
DROP TABLE IF EXISTS wms_assembly_item;
DROP TABLE IF EXISTS wms_assembly;
DROP TABLE IF EXISTS wms_stock_doc_item;
DROP TABLE IF EXISTS wms_stock_doc;
DROP TABLE IF EXISTS wms_serial_log;
DROP TABLE IF EXISTS wms_stock_flow;
DROP TABLE IF EXISTS wms_serial;
DROP TABLE IF EXISTS wms_reservation;
DROP TABLE IF EXISTS wms_stock_hold;
DROP TABLE IF EXISTS wms_stock;
DROP TABLE IF EXISTS wms_batch;
DROP TABLE IF EXISTS wms_variant_value;
DROP TABLE IF EXISTS wms_attribute_value;
DROP TABLE IF EXISTS wms_attribute;
DROP TABLE IF EXISTS wms_material_unit;
DROP TABLE IF EXISTS wms_material;
DROP TABLE IF EXISTS wms_location;
DROP TABLE IF EXISTS wms_warehouse;
DROP TABLE IF EXISTS wf_copy;
DROP TABLE IF EXISTS wf_model;
DROP TABLE IF EXISTS sys_change_log;
DROP TABLE IF EXISTS sys_attachment;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_job;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_tenant;
DROP TABLE IF EXISTS sys_code_rule;
DROP TABLE IF EXISTS sys_biz_rule;
DROP TABLE IF EXISTS sys_custom_value;
DROP TABLE IF EXISTS sys_custom_field;
DROP TABLE IF EXISTS sys_print_template;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_tenant_package_menu;
DROP TABLE IF EXISTS sys_tenant_package;

-- 租户：SaaS 模式（lemon.tenant.enabled）下业务表按 tenant_id 隔离；租户 1 是平台租户（运营方），
-- 未开启 SaaS 模式时所有数据都属于它。菜单、字典、定时任务等平台共享的表没有 tenant_id
CREATE TABLE sys_tenant_package (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '套餐名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_package_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户套餐';

CREATE TABLE sys_tenant_package_menu (
    package_id BIGINT NOT NULL COMMENT '套餐ID',
    menu_id    BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (package_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '套餐可用菜单';

CREATE TABLE sys_tenant (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(64)  NOT NULL COMMENT '租户名称，登录时填写',
    package_id    BIGINT                DEFAULT NULL COMMENT '套餐，平台租户为空（不受限）',
    contact_name  VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    expire_time   DATETIME              DEFAULT NULL COMMENT '到期时间，为空不限',
    account_limit INT                   DEFAULT NULL COMMENT '账号数量上限，为空不限',
    admin_user_id BIGINT                DEFAULT NULL COMMENT '租户管理员（创建租户时开通）',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_name (name),
    KEY idx_tenant_package (package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租户';

-- 编码规则：租户修改过的单据编号规则，未修改的单据使用各模块声明的默认规则（DocNoCatalog）
CREATE TABLE sys_code_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(16) NOT NULL COMMENT '规则标识（默认前缀），如 CG',
    prefix      VARCHAR(8)  NOT NULL COMMENT '前缀',
    date_format VARCHAR(16) NOT NULL COMMENT '日期格式 YYYYMMDD / YYMMDD / YYYYMM / YYMM / YYYY / NONE',
    seq_length  INT         NOT NULL COMMENT '流水位数',
    reset_cycle VARCHAR(8)  NOT NULL COMMENT '流水重置周期 DAY / MONTH / YEAR / NEVER',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '编码规则';

-- 系统参数（参数设置）：平台级，所有租户共用；只保存修改过的参数，参数定义见 framework/setting/Setting
CREATE TABLE sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(64)  NOT NULL COMMENT '参数标识，如 captcha.mode',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数';

-- 打印模板：租户修改过的单据打印配置（JSON，由前端解释），未修改的单据使用默认版式；上传了 JRXML 的单据按 JRXML 打印
CREATE TABLE sys_print_template (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_key VARCHAR(64) NOT NULL COMMENT '模板标识，如 scm.purchaseOrder；_common 为通用设置',
    config       TEXT        NOT NULL COMMENT '模板配置 JSON',
    jrxml        MEDIUMTEXT COMMENT '自定义 JRXML（Jaspersoft Studio 7），为空时按配置生成默认版式',
    background   MEDIUMTEXT COMMENT '套打底图（data URL）：设计套打位置时对照，不打印',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_print_template (template_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '打印模板';

CREATE TABLE sys_biz_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    rule_key    VARCHAR(64)  NOT NULL COMMENT '参数标识，如 scm.purchaseIn.overRate',
    rule_value  VARCHAR(255) NOT NULL COMMENT '参数值',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_rule (rule_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务参数（按租户）：各模块的业务控制规则，只保存修改过的';

CREATE TABLE sys_custom_field (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（与附件相同的业务记录类型，如 scm_customer、wms_material）',
    field_key    VARCHAR(32)  NOT NULL COMMENT '字段标识，字母开头的字母、数字、下划线',
    label        VARCHAR(32)  NOT NULL COMMENT '显示名称',
    field_type   VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '类型(字典 sys_custom_field_type) TEXT文本 TEXTAREA多行文本 NUMBER数字 DATE日期 SELECT下拉 BOOL是否',
    options      VARCHAR(1000)         DEFAULT NULL COMMENT '下拉选项，逗号或换行分隔',
    required     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填',
    show_in_list TINYINT      NOT NULL DEFAULT 0 COMMENT '是否在列表中显示',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '说明（显示为输入提示）',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_field (biz_type, field_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段：为单据、主数据扩展字段，不改表结构';

CREATE TABLE sys_custom_value (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)   NOT NULL COMMENT '业务类型',
    biz_id      BIGINT        NOT NULL COMMENT '业务记录ID',
    field_key   VARCHAR(32)   NOT NULL COMMENT '字段标识',
    field_value VARCHAR(1000) NOT NULL COMMENT '值（文本；数字、日期、是否按约定格式）',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_value (biz_type, biz_id, field_key, tenant_id),
    KEY idx_custom_value_field (biz_type, field_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段的值';

CREATE TABLE sys_dept (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级部门ID，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '部门名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    leader_user_id BIGINT               DEFAULT NULL COMMENT '负责人（用户ID），审批人「部门主管」据此计算',
    phone       VARCHAR(32)          DEFAULT NULL COMMENT '联系电话',
    email       VARCHAR(128)         DEFAULT NULL COMMENT '邮箱',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dept_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门';

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    dept_id     BIGINT                DEFAULT NULL COMMENT '部门ID',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    pwd_update_time DATETIME              DEFAULT NULL COMMENT '最近设置密码的时间（密码有效期）',
    pwd_reset   TINYINT      NOT NULL DEFAULT 0 COMMENT '1 密码由管理员设置，登录后提示本人修改',
    nickname    VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    supplier_id BIGINT                DEFAULT NULL COMMENT '供应商门户账号关联的供应商，为空是内部用户',
    customer_id BIGINT                DEFAULT NULL COMMENT '经销商门户账号关联的客户，为空是内部用户',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, tenant_id),
    KEY idx_user_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    data_scope  TINYINT     NOT NULL DEFAULT 3 COMMENT '数据权限(字典 sys_data_scope) 1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

CREATE TABLE sys_menu (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '菜单名称',
    type        CHAR(1)     NOT NULL COMMENT '类型 M目录 C菜单 F按钮',
    path        VARCHAR(200)         DEFAULT NULL COMMENT '路由地址',
    component   VARCHAR(200)         DEFAULT NULL COMMENT '组件路径(相对 src/views，不带 .vue)',
    perms       VARCHAR(100)         DEFAULT NULL COMMENT '权限标识',
    icon        VARCHAR(64)          DEFAULT NULL COMMENT '图标',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    visible     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否显示 1显示 0隐藏',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    -- 按角色反查用户（删除角色前检查、角色关联用户）
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    -- 删除菜单时清理关联
    KEY idx_menu_id (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联';

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id),
    -- 删除部门时清理关联
    KEY idx_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色自定义数据权限（部门）';

CREATE TABLE sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '字典名称',
    type        VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型';

CREATE TABLE sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典键值',
    tag_color   VARCHAR(32)           DEFAULT NULL COMMENT '标签颜色(ant-design-vue Tag color)',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据';

CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title          VARCHAR(64)           DEFAULT NULL COMMENT '模块标题',
    oper_type      TINYINT      NOT NULL DEFAULT 0 COMMENT '操作类型(字典 sys_oper_type)',
    method         VARCHAR(200)          DEFAULT NULL COMMENT '类名.方法名',
    request_method VARCHAR(10)           DEFAULT NULL COMMENT '请求方式',
    url            VARCHAR(255)          DEFAULT NULL COMMENT '请求地址',
    ip             VARCHAR(64)           DEFAULT NULL COMMENT 'IP',
    location       VARCHAR(128)          DEFAULT NULL COMMENT 'IP 归属地',
    operator       VARCHAR(64)           DEFAULT NULL COMMENT '操作人',
    params         TEXT COMMENT '请求参数',
    result         TEXT COMMENT '返回结果',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    error_msg      VARCHAR(2000)         DEFAULT NULL COMMENT '错误信息',
    cost_time      BIGINT                DEFAULT NULL COMMENT '耗时(毫秒)',
    oper_time      DATETIME              DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

CREATE TABLE sys_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    category    VARCHAR(16)  NOT NULL COMMENT '类别(字典 sys_message_category) APPROVAL审批 REMIND提醒 ALERT预警 SYSTEM系统',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '内容',
    link        VARCHAR(255)          DEFAULT NULL COMMENT '前端路由，点击跳转',
    dedup_key   VARCHAR(128)          DEFAULT NULL COMMENT '去重键：同一用户相同去重键只发送一次（定时提醒）',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 1是 0否',
    read_time   DATETIME              DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_message_user (user_id, read_flag),
    KEY idx_message_dedup (dedup_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息';

CREATE TABLE sys_attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型（AttachmentType.code），如 scm_purchase',
    biz_id       BIGINT       NOT NULL COMMENT '业务记录ID',
    file_name    VARCHAR(255) NOT NULL COMMENT '原文件名',
    file_size    BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    content_type VARCHAR(128)          DEFAULT NULL COMMENT '文件类型',
    storage_path VARCHAR(255) NOT NULL COMMENT '相对附件目录的存储路径',
    create_by    BIGINT                DEFAULT NULL COMMENT '上传人',
    create_time  DATETIME              DEFAULT NULL COMMENT '上传时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_attachment_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务附件';

CREATE TABLE sys_change_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type    VARCHAR(32)  NOT NULL COMMENT '业务类型（RecordType.code），如 wms_material',
    biz_id      BIGINT       NOT NULL COMMENT '业务记录ID',
    field_name  VARCHAR(64)  NOT NULL COMMENT '字段',
    field_label VARCHAR(64)           DEFAULT NULL COMMENT '字段名称',
    old_value   VARCHAR(500)          DEFAULT NULL COMMENT '旧值',
    new_value   VARCHAR(500)          DEFAULT NULL COMMENT '新值',
    create_by   BIGINT                DEFAULT NULL COMMENT '修改人',
    create_time DATETIME              DEFAULT NULL COMMENT '修改时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_change_log_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字段变更历史';

CREATE TABLE sys_job (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_key        VARCHAR(64)  NOT NULL COMMENT '任务标识（JobHandler.key）',
    name           VARCHAR(64)  NOT NULL COMMENT '任务名称',
    cron           VARCHAR(64)  NOT NULL COMMENT 'Cron 表达式（秒 分 时 日 月 周）',
    params         VARCHAR(255)          DEFAULT NULL COMMENT '参数 key=value，多个用逗号分隔',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_run_time  DATETIME              DEFAULT NULL COMMENT '最近执行时间',
    last_status    VARCHAR(16)           DEFAULT NULL COMMENT '最近执行结果 SUCCESS成功 FAILED失败',
    last_message   VARCHAR(500)          DEFAULT NULL COMMENT '最近执行说明',
    remark         VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time    DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_key (job_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务';

CREATE TABLE sys_job_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_key      VARCHAR(64)  NOT NULL COMMENT '任务标识',
    trigger_type VARCHAR(16)  NOT NULL COMMENT '触发方式 SCHEDULE定时 MANUAL手动',
    start_time   DATETIME     NOT NULL COMMENT '开始时间',
    cost_ms      BIGINT                DEFAULT NULL COMMENT '耗时（毫秒）',
    status       VARCHAR(16)  NOT NULL COMMENT '结果 SUCCESS成功 FAILED失败',
    message      VARCHAR(500)          DEFAULT NULL COMMENT '执行说明或错误信息',
    create_by    BIGINT                DEFAULT NULL COMMENT '手动执行人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_job_log_key (job_key, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务执行日志';

CREATE TABLE sys_login_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    username   VARCHAR(64)          DEFAULT NULL COMMENT '用户名',
    ip         VARCHAR(64)          DEFAULT NULL COMMENT 'IP',
    location   VARCHAR(128)         DEFAULT NULL COMMENT 'IP 归属地',
    browser    VARCHAR(64)          DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(64)          DEFAULT NULL COMMENT '操作系统',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
    msg        VARCHAR(255)         DEFAULT NULL COMMENT '提示信息',
    login_time DATETIME             DEFAULT NULL COMMENT '登录时间',
    PRIMARY KEY (id),
    -- 首页今日登录数、近 7 天趋势只需这两列，覆盖索引不回表
    KEY idx_login_time_status (login_time, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志';

CREATE TABLE wf_model (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    model_key     VARCHAR(64)  NOT NULL COMMENT '流程标识（BPMN 流程 ID），发布后不可修改',
    name          VARCHAR(64)  NOT NULL COMMENT '流程名称',
    category      VARCHAR(64)           DEFAULT NULL COMMENT '分类(字典 wf_category)',
    icon          VARCHAR(64)           DEFAULT NULL COMMENT '图标',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用（停用后不能发起）',
    version       INT                   DEFAULT NULL COMMENT '最新发布的版本号，NULL 为未发布',
    definition_id VARCHAR(64)           DEFAULT NULL COMMENT '最新发布的流程定义ID',
    deploy_time   DATETIME              DEFAULT NULL COMMENT '最近发布时间',
    design_json   MEDIUMTEXT COMMENT '流程设计（表单 + 节点树）JSON 草稿',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_key (model_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程模型';

CREATE TABLE wf_copy (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    process_instance_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
    process_name        VARCHAR(255)         DEFAULT NULL COMMENT '流程标题（快照）',
    process_key         VARCHAR(64)          DEFAULT NULL COMMENT '流程标识',
    node_id             VARCHAR(64) NOT NULL COMMENT '抄送节点ID',
    node_name           VARCHAR(64)          DEFAULT NULL COMMENT '抄送节点名称',
    start_user_id       BIGINT               DEFAULT NULL COMMENT '发起人',
    user_id             BIGINT      NOT NULL COMMENT '被抄送人',
    create_time         DATETIME             DEFAULT NULL COMMENT '抄送时间',
    update_time         DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 抄送我的：按用户查询，按 id 倒序
    KEY idx_copy_user_id (user_id, id),
    -- 流程详情中的抄送记录
    KEY idx_copy_instance_id (process_instance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程抄送';

-- ---------------------------------------------------------------- 仓库管理

CREATE TABLE wms_warehouse (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '仓库编码',
    name        VARCHAR(64)  NOT NULL COMMENT '仓库名称',
    type        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '仓库类型(字典 wms_warehouse_type) NORMAL普通 RAW原料 FINISHED成品 DEFECT不良品 CONSIGN_OUT客户寄售仓 CONSIGN_IN供应商寄售仓（不良品、寄售仓不计入可用库存，供应商寄售仓不计入存货成本）',
    partner_id  BIGINT                DEFAULT NULL COMMENT '寄售仓的客户（客户寄售仓）/ 供应商（供应商寄售仓）',
    address     VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    contact     VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '仓库';

CREATE TABLE wms_location (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id BIGINT       NOT NULL COMMENT '所属仓库',
    code         VARCHAR(32)  NOT NULL COMMENT '库位编码，仓库内唯一，如 A-01-02',
    name         VARCHAR(64)           DEFAULT NULL COMMENT '库位名称',
    zone         VARCHAR(32)           DEFAULT NULL COMMENT '区域，如 A 区、冷藏区；拣货路径按 区域 → 排序 → 编码',
    loc_type     VARCHAR(16)  NOT NULL DEFAULT 'STORAGE' COMMENT '类型(字典 wms_location_type) STORAGE存储位 PICK拣货位 RECEIVE收货暂存 SHIP发货暂存 DEFECT不良品位',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序（拣货路径顺序）',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_location_code (warehouse_id, code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库位';

CREATE TABLE wms_material (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code           VARCHAR(64)   NOT NULL COMMENT '物料编码',
    name           VARCHAR(128)  NOT NULL COMMENT '物料名称',
    spec           VARCHAR(128)           DEFAULT NULL COMMENT '规格型号',
    unit           VARCHAR(16)            DEFAULT NULL COMMENT '计量单位',
    barcode        VARCHAR(64)            DEFAULT NULL COMMENT '条码（基本单位），扫码录入物料',
    category       VARCHAR(64)            DEFAULT NULL COMMENT '分类(字典 wms_material_category)',
    batch_enabled  TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用批次管理 1是 0否',
    serial_enabled TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用序列号管理 1是 0否',
    iqc_required   TINYINT        NOT NULL DEFAULT 0 COMMENT '来料检验 1是：采购入库后冻结为待检，检验合格后放行',
    safety_stock   DECIMAL(18, 4)         DEFAULT NULL COMMENT '安全库存，汇总库存低于该值时预警',
    reorder_point  DECIMAL(18, 4)         DEFAULT NULL COMMENT '再订货点：预计库存（可用 + 在途 + 在制 − 待发 − 待领）不高于此值时建议补货，为空不参与',
    reorder_qty    DECIMAL(18, 4)         DEFAULT NULL COMMENT '再订货量（经济批量），补货建议不少于此数量',
    default_warehouse_id BIGINT       DEFAULT NULL COMMENT '默认仓库：录入出入库单、订单时默认带出',
    default_location_id  BIGINT       DEFAULT NULL COMMENT '默认库位（默认仓库中的）',
    ref_cost       DECIMAL(18, 4)         DEFAULT NULL COMMENT '参考成本价：没有核算单价时（首次核算的其他入库等）按此计价',
    cost_method    VARCHAR(16)   NOT NULL DEFAULT 'WEIGHTED' COMMENT '存货计价方法(字典 wms_cost_method) WEIGHTED月末一次加权平均 MOVING移动加权平均 FIFO先进先出',
    is_template    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否多规格款式 1是：按规格属性生成 SKU，款式本身不能出入库',
    template_id    BIGINT                 DEFAULT NULL COMMENT 'SKU 所属的款式（物料）',
    variant_text   VARCHAR(255)           DEFAULT NULL COMMENT 'SKU 的规格属性值，如 红色 / XL',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_code (code, tenant_id),
    KEY idx_material_barcode (barcode),
    KEY idx_material_template (template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料';

CREATE TABLE wms_attribute (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(32) NOT NULL COMMENT '规格属性名称，如 颜色、尺码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_name (name, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性：多规格物料（款式）按属性值组合生成 SKU';

CREATE TABLE wms_attribute_value (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    attribute_id BIGINT      NOT NULL COMMENT '规格属性',
    attr_value   VARCHAR(32) NOT NULL COMMENT '属性值，如 红色、XL',
    code         VARCHAR(16)          DEFAULT NULL COMMENT '编码，生成 SKU 编码时使用，为空用属性值',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attribute_value (attribute_id, attr_value, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '规格属性值';

CREATE TABLE wms_variant_value (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    template_id  BIGINT NOT NULL COMMENT '款式',
    material_id  BIGINT NOT NULL COMMENT 'SKU',
    attribute_id BIGINT NOT NULL COMMENT '规格属性',
    value_id     BIGINT NOT NULL COMMENT '属性值',
    create_time  DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_variant_value (material_id, attribute_id, tenant_id),
    KEY idx_variant_template (template_id),
    KEY idx_variant_value (value_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 的规格属性值';

CREATE TABLE wms_material_unit (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    unit        VARCHAR(16)    NOT NULL COMMENT '辅助单位，如 箱',
    ratio       DECIMAL(18, 6) NOT NULL COMMENT '换算率：1 辅助单位 = ratio 基本单位',
    barcode     VARCHAR(64)             DEFAULT NULL COMMENT '包装条码，扫码时按换算率带出数量',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_unit (material_id, unit, tenant_id),
    KEY idx_material_unit_barcode (barcode)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料辅助计量单位：库存、成本都按基本单位，单据可按辅助单位录入后换算';

CREATE TABLE wms_batch (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id     BIGINT      NOT NULL COMMENT '物料',
    batch_no        VARCHAR(64) NOT NULL COMMENT '批次号',
    production_date DATE                 DEFAULT NULL COMMENT '生产日期',
    expiry_date     DATE                 DEFAULT NULL COMMENT '有效期至',
    create_time     DATETIME             DEFAULT NULL COMMENT '首次入库时间',
    update_time     DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch (material_id, batch_no, tenant_id),
    -- 批次追踪：按批次号查询、临期预警
    KEY idx_batch_no (batch_no),
    KEY idx_batch_expiry (expiry_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '批次';

CREATE TABLE wms_stock (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定库位',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号，未启用批次管理为空串',
    qty          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '数量，不会小于 0',
    frozen_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '冻结数量（待检、不合格），不能出库',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock (warehouse_id, location_id, material_id, batch_no, tenant_id),
    KEY idx_stock_material (material_id, batch_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存';

CREATE TABLE wms_stock_hold (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 wms_hold_status) PENDING待检 REJECTED不合格 RELEASED已放行 RETURNED已退货',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    hold_qty     DECIMAL(18, 4) NOT NULL COMMENT '冻结时的数量',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '仍冻结的数量',
    source_no    VARCHAR(32)             DEFAULT NULL COMMENT '采购入库单号',
    order_id     BIGINT                  DEFAULT NULL COMMENT '采购订单',
    partner_id   BIGINT                  DEFAULT NULL COMMENT '供应商',
    insp_no      VARCHAR(32)             DEFAULT NULL COMMENT '检验单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    release_by   BIGINT                  DEFAULT NULL COMMENT '放行人',
    release_time DATETIME                DEFAULT NULL COMMENT '放行时间',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_hold_source (source_no, material_id),
    KEY idx_hold_order (order_id, material_id),
    KEY idx_hold_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存冻结（来料待检、不合格）';

CREATE TABLE wms_reservation (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    warehouse_id   BIGINT         NOT NULL COMMENT '仓库',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '剩余预留数量（出库核销、释放后减少，0 表示已结束）',
    source_type    VARCHAR(16)    NOT NULL COMMENT '来源类型 SALES销售订单',
    source_id      BIGINT         NOT NULL COMMENT '来源单据',
    source_item_id BIGINT                  DEFAULT NULL COMMENT '来源明细',
    source_no      VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '预留人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reservation_slot (warehouse_id, material_id),
    KEY idx_reservation_source (source_type, source_id),
    KEY idx_reservation_item (source_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存预留：仓库中可用库存不能低于全部剩余预留';

CREATE TABLE wms_serial (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT      NOT NULL COMMENT '物料',
    serial_no    VARCHAR(64) NOT NULL COMMENT '序列号',
    warehouse_id BIGINT      NOT NULL COMMENT '当前（或最后所在）仓库',
    location_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '当前（或最后所在）库位，0 为未指定',
    batch_no     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    status       VARCHAR(16) NOT NULL COMMENT '状态(字典 wms_serial_status) IN_STOCK在库 OUT已出库',
    create_time  DATETIME             DEFAULT NULL COMMENT '首次入库时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_serial (material_id, serial_no, tenant_id),
    KEY idx_serial_no (serial_no),
    -- 出库、调拨时选择某个库存位置上的在库序列号
    KEY idx_serial_slot (warehouse_id, location_id, material_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '序列号';

CREATE TABLE wms_stock_flow (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_type     VARCHAR(16)    NOT NULL COMMENT '业务类型(字典 wms_biz_type) IN入库 OUT出库 TRANSFER调拨 CHECK盘点',
    doc_id       BIGINT         NOT NULL COMMENT '单据ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    batch_no     VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '变动数量，正数增加、负数减少',
    create_by    BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME                DEFAULT NULL COMMENT '发生时间',
    PRIMARY KEY (id),
    -- 批次追踪、物料台账
    KEY idx_flow_material (material_id, batch_no),
    KEY idx_flow_doc (biz_type, doc_id),
    KEY idx_flow_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存流水';

CREATE TABLE wms_serial_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id  BIGINT      NOT NULL COMMENT '物料',
    serial_no    VARCHAR(64) NOT NULL COMMENT '序列号',
    biz_type     VARCHAR(16) NOT NULL COMMENT '业务类型(字典 wms_biz_type)',
    doc_id       BIGINT      NOT NULL COMMENT '单据ID',
    doc_no       VARCHAR(32) NOT NULL COMMENT '单据号',
    direction    TINYINT     NOT NULL COMMENT '方向 1入 -1出',
    warehouse_id BIGINT      NOT NULL COMMENT '仓库',
    location_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号',
    create_by    BIGINT               DEFAULT NULL COMMENT '操作人',
    create_time  DATETIME             DEFAULT NULL COMMENT '发生时间',
    PRIMARY KEY (id),
    -- 序列号追踪：按物料 + 序列号查询完整轨迹
    KEY idx_serial_log (material_id, serial_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '序列号轨迹';

CREATE TABLE wms_stock_doc (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no          VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '单据种类 IN入库 OUT出库 TRANSFER调拨',
    biz_type        VARCHAR(32)             DEFAULT NULL COMMENT '入库类型(字典 wms_in_type) / 出库类型(字典 wms_out_type)，调拨为空',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库；调拨为调出仓库',
    to_warehouse_id BIGINT                  DEFAULT NULL COMMENT '调入仓库，仅调拨',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中 TRANSIT在途（在途调拨已调出未调入） DONE已完成',
    total_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    source_no       VARCHAR(32)             DEFAULT NULL COMMENT '来源单号：由采购入库、销售出库、工单领料等业务单据自动生成时填写',
    in_transit      TINYINT        NOT NULL DEFAULT 0 COMMENT '在途调拨 1是：确认时只调出，调入仓库收货后才入库',
    receive_by      BIGINT                  DEFAULT NULL COMMENT '调入收货人',
    receive_time    DATETIME                DEFAULT NULL COMMENT '调入收货时间',
    red_of          BIGINT                  DEFAULT NULL COMMENT '红字冲销单：被冲销的原单据',
    reversed        TINYINT        NOT NULL DEFAULT 0 COMMENT '已被冲销 1是',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例（发布了出入库审批流程时，手工单据确认前审批）',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_doc_no (doc_no, tenant_id),
    -- 各单据列表：按种类查询，按 id 倒序
    KEY idx_stock_doc_kind (kind, id),
    KEY idx_stock_doc_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库单（入库、出库、调拨）';

CREATE TABLE wms_stock_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位；调拨为调出库位，0 为未指定',
    to_location_id  BIGINT         NOT NULL DEFAULT 0 COMMENT '调入库位，仅调拨',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅入库新批次',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅入库新批次',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stock_doc_item_doc (doc_id),
    -- 删除物料前检查是否被单据引用
    KEY idx_stock_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库单明细';

CREATE TABLE wms_assembly (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_no     VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind            VARCHAR(16)    NOT NULL COMMENT '种类(字典 wms_assembly_kind) ASSEMBLE组装 DISASSEMBLE拆卸',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库',
    biz_date        DATE           NOT NULL COMMENT '业务日期',
    material_id     BIGINT         NOT NULL COMMENT '成套件（组装得到 / 拆卸的物料）',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '成套件库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '成套件批次号',
    production_date DATE                    DEFAULT NULL COMMENT '成套件生产日期（组装入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '成套件有效期至（组装入库新批次）',
    serial_nos      TEXT COMMENT '成套件序列号，逗号分隔',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '成套件数量',
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 DONE已完成',
    out_doc_id      BIGINT                  DEFAULT NULL COMMENT '生成的出库单（组装为子件出库，拆卸为成套件出库）',
    out_doc_no      VARCHAR(32)             DEFAULT NULL COMMENT '生成的出库单号',
    in_doc_id       BIGINT                  DEFAULT NULL COMMENT '生成的入库单（组装为成套件入库，拆卸为子件入库）',
    in_doc_no       VARCHAR(32)             DEFAULT NULL COMMENT '生成的入库单号',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    confirm_by      BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assembly_no (assembly_no, tenant_id),
    KEY idx_assembly_date (biz_date),
    KEY idx_assembly_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单：组装消耗子件得到成套件，拆卸相反；确认后生成出库、入库单（类型 ASSEMBLY），成本核算按子件成本计算成套件成本';

CREATE TABLE wms_assembly_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    assembly_id     BIGINT         NOT NULL COMMENT '组装拆卸单',
    material_id     BIGINT         NOT NULL COMMENT '子件',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期（拆卸入库新批次）',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至（拆卸入库新批次）',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_assembly_item_assembly (assembly_id),
    KEY idx_assembly_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组装拆卸单的子件';

CREATE TABLE wms_check (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)  NOT NULL COMMENT '单据号',
    warehouse_id BIGINT       NOT NULL COMMENT '盘点仓库',
    biz_date     DATE         NOT NULL COMMENT '盘点日期',
    status       VARCHAR(16)  NOT NULL COMMENT '状态(字典 wms_doc_status) DRAFT草稿 APPROVING审批中（盘点差异审批） DONE已完成',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    freeze       TINYINT      NOT NULL DEFAULT 0 COMMENT '冻结仓库 1是：盘点完成前该仓库不能出入库、调拨',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '差异审批流程实例',
    dept_id      BIGINT                DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                DEFAULT NULL COMMENT '制单人',
    confirm_by   BIGINT                DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME              DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_check_doc_no (doc_no, tenant_id),
    KEY idx_check_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '盘点单';

CREATE TABLE wms_check_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_id    BIGINT         NOT NULL COMMENT '盘点单ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    location_id BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no    VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    book_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '账面数量：草稿为保存时的库存，完成后为确认时的库存',
    actual_qty  DECIMAL(18, 4)          DEFAULT NULL COMMENT '实盘数量，草稿中可为空',
    serial_nos  TEXT COMMENT '实盘序列号，逗号分隔（序列号物料）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_check_item_check (check_id),
    KEY idx_check_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '盘点单明细';

CREATE TABLE wms_doc_sequence (
    seq_key   VARCHAR(32) NOT NULL COMMENT '单据号前缀 + 日期，如 RK20261014',
    seq_value BIGINT      NOT NULL COMMENT '当天已使用的最大流水号',
    tenant_id BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (seq_key, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '单据号流水';

-- ---------------------------------------------------------------- 供应链管理

CREATE TABLE scm_supplier (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    name         VARCHAR(128) NOT NULL COMMENT '供应商名称',
    level        VARCHAR(16)           DEFAULT NULL COMMENT '等级(字典 scm_supplier_level) A/B/C/D',
    contact      VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    phone        VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    email        VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    address      VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    tax_no       VARCHAR(32)           DEFAULT NULL COMMENT '纳税人识别号',
    bank_name    VARCHAR(128)          DEFAULT NULL COMMENT '开户行',
    bank_account VARCHAR(64)           DEFAULT NULL COMMENT '银行账号',
    payment_days INT          NOT NULL DEFAULT 0 COMMENT '账期（天），应付单到期日 = 单据日期 + 账期',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商';

CREATE TABLE scm_supplier_price (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supplier_id BIGINT         NOT NULL COMMENT '供应商',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate    DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    lead_days   INT            NOT NULL DEFAULT 0 COMMENT '采购提前期（天），MRP 按此倒推下单日期',
    min_qty     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最小起订量',
    is_default  TINYINT        NOT NULL DEFAULT 0 COMMENT '是否该物料的默认供应商 1是 0否，MRP 按默认供应商生成采购订单',
    valid_from  DATE                    DEFAULT NULL COMMENT '有效期起，为空不限',
    valid_to    DATE                    DEFAULT NULL COMMENT '有效期止，过期后 MRP、请购不再取该价格',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_price (supplier_id, material_id, tenant_id),
    KEY idx_supplier_price_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供货价格';

CREATE TABLE scm_supplier_price_tier (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    price_id    BIGINT         NOT NULL COMMENT '供货价格',
    min_qty     DECIMAL(18, 4) NOT NULL COMMENT '起订数量（达到该数量适用本档单价）',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_tier_price (price_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供货价格阶梯价';

CREATE TABLE scm_price_list (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    customer_id    BIGINT                  DEFAULT NULL COMMENT '客户专属价：指定客户时只适用该客户（优先于客户等级）',
    customer_level VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)，为空适用全部客户',
    min_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '起订数量',
    price          DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate       DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    valid_from     DATE                    DEFAULT NULL COMMENT '有效期起',
    valid_to       DATE                    DEFAULT NULL COMMENT '有效期止',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_price_list_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售价目表：取价时优先匹配客户专属价，其次客户等级，再取满足起订数量的最大一档';

CREATE TABLE scm_price_control (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id        BIGINT         NOT NULL COMMENT '物料',
    min_sale_price     DECIMAL(18, 4)          DEFAULT NULL COMMENT '最低销售价（含税，基本单位，本位币），为空不控制',
    max_purchase_price DECIMAL(18, 4)          DEFAULT NULL COMMENT '最高采购价（含税，基本单位，本位币），为空不控制',
    control_mode       VARCHAR(8)     NOT NULL DEFAULT 'WARN' COMMENT '控制方式(字典 scm_price_control_mode) WARN提示 BLOCK禁止保存',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_price_control (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格控制：销售订单单价不低于最低销售价，采购订单单价不高于最高采购价（手工录入的订单）';

CREATE TABLE scm_requisition (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    req_no           VARCHAR(32)  NOT NULL COMMENT '请购单号',
    req_date         DATE         NOT NULL COMMENT '请购日期',
    status           VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 scm_requisition_status) DRAFT草稿 APPROVING审批中 APPROVED已批准 ORDERED已下单 CLOSED已关闭',
    remark           VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例',
    approve_by       BIGINT                DEFAULT NULL COMMENT '批准人',
    approve_time     DATETIME              DEFAULT NULL COMMENT '批准时间',
    dept_id          BIGINT                DEFAULT NULL COMMENT '请购部门',
    create_by        BIGINT                DEFAULT NULL COMMENT '请购人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_requisition_no (req_no, tenant_id),
    KEY idx_requisition_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '请购单';

CREATE TABLE scm_requisition_item (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    requisition_id BIGINT         NOT NULL COMMENT '请购单',
    material_id    BIGINT         NOT NULL COMMENT '物料',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '请购数量',
    required_date  DATE                    DEFAULT NULL COMMENT '需求日期',
    supplier_id    BIGINT                  DEFAULT NULL COMMENT '建议供应商，为空时下单取默认供应商',
    order_id       BIGINT                  DEFAULT NULL COMMENT '生成的采购订单',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_requisition_item_req (requisition_id),
    KEY idx_requisition_item_material (material_id),
    KEY idx_requisition_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '请购单明细';

CREATE TABLE scm_inquiry (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inquiry_no   VARCHAR(32)  NOT NULL COMMENT '询价单号',
    inquiry_date DATE         NOT NULL COMMENT '询价日期',
    deadline     DATE                  DEFAULT NULL COMMENT '报价截止日期',
    status       VARCHAR(16)  NOT NULL DEFAULT 'QUOTING' COMMENT '状态(字典 scm_inquiry_status) QUOTING询价中 FINISHED已定标 CLOSED已关闭',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    dept_id      BIGINT                DEFAULT NULL COMMENT '部门',
    create_by    BIGINT                DEFAULT NULL COMMENT '询价人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inquiry_no (inquiry_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '询价单';

CREATE TABLE scm_inquiry_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inquiry_id        BIGINT         NOT NULL COMMENT '询价单',
    material_id       BIGINT         NOT NULL COMMENT '物料',
    qty               DECIMAL(18, 4) NOT NULL COMMENT '询价数量',
    required_date     DATE                    DEFAULT NULL COMMENT '需求日期',
    selected_quote_id BIGINT                  DEFAULT NULL COMMENT '中标报价',
    order_id          BIGINT                  DEFAULT NULL COMMENT '生成的采购订单',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_inquiry_item_inquiry (inquiry_id),
    KEY idx_inquiry_item_material (material_id),
    KEY idx_inquiry_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '询价单明细';

CREATE TABLE scm_inquiry_quote (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    inquiry_id  BIGINT         NOT NULL COMMENT '询价单',
    item_id     BIGINT         NOT NULL COMMENT '询价明细',
    supplier_id BIGINT         NOT NULL COMMENT '供应商',
    price       DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate    DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（%）',
    lead_days   INT                     DEFAULT NULL COMMENT '交期（天）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_inquiry_quote_inquiry (inquiry_id),
    KEY idx_inquiry_quote_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商报价';

CREATE TABLE scm_quotation (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    quote_no         VARCHAR(32)    NOT NULL COMMENT '报价单号',
    customer_id      BIGINT         NOT NULL COMMENT '客户',
    opportunity_id   BIGINT                  DEFAULT NULL COMMENT '商机（CRM）',
    quote_date       DATE           NOT NULL COMMENT '报价日期',
    valid_to         DATE                    DEFAULT NULL COMMENT '有效期至',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 scm_quotation_status) DRAFT草稿 APPROVING审批中 APPROVED已批准 ORDERED已成交 LOST已失效',
    total_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    lost_reason      VARCHAR(255)            DEFAULT NULL COMMENT '失效原因',
    order_id         BIGINT                  DEFAULT NULL COMMENT '成交生成的销售订单',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    approve_by       BIGINT                  DEFAULT NULL COMMENT '批准人',
    approve_time     DATETIME                DEFAULT NULL COMMENT '批准时间',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '部门',
    create_by        BIGINT                  DEFAULT NULL COMMENT '报价人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_quotation_no (quote_no, tenant_id),
    KEY idx_quotation_customer (customer_id),
    KEY idx_quotation_opportunity (opportunity_id),
    KEY idx_quotation_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售报价单';

CREATE TABLE scm_quotation_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    quotation_id BIGINT         NOT NULL COMMENT '报价单',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price        DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate     DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    list_price   DECIMAL(18, 4)          DEFAULT NULL COMMENT '价目表单价',
    amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_quotation_item_quotation (quotation_id),
    KEY idx_quotation_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售报价单明细';

CREATE TABLE scm_supplier_eval (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supplier_id    BIGINT        NOT NULL COMMENT '供应商',
    period         VARCHAR(16)   NOT NULL COMMENT '考核期间，如 2026-Q3、2026-09',
    quality_score  DECIMAL(5, 1) NOT NULL COMMENT '质量得分（0-100）',
    delivery_score DECIMAL(5, 1) NOT NULL COMMENT '交期得分',
    price_score    DECIMAL(5, 1) NOT NULL COMMENT '价格得分',
    service_score  DECIMAL(5, 1) NOT NULL COMMENT '服务得分',
    total_score    DECIMAL(5, 1) NOT NULL COMMENT '综合得分 = 质量40% + 交期30% + 价格20% + 服务10%',
    grade          VARCHAR(16)   NOT NULL COMMENT '评定等级(字典 scm_supplier_level)，按综合得分',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '评语',
    create_by      BIGINT                 DEFAULT NULL COMMENT '评估人',
    create_time    DATETIME               DEFAULT NULL COMMENT '评估时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_supplier_eval_supplier (supplier_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商评估';

CREATE TABLE scm_customer (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)    NOT NULL COMMENT '客户编码',
    name         VARCHAR(128)   NOT NULL COMMENT '客户名称',
    level        VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)',
    contact      VARCHAR(64)             DEFAULT NULL COMMENT '联系人',
    phone        VARCHAR(32)             DEFAULT NULL COMMENT '联系电话',
    address      VARCHAR(255)            DEFAULT NULL COMMENT '收货地址',
    tax_no       VARCHAR(32)             DEFAULT NULL COMMENT '纳税人识别号',
    credit_limit DECIMAL(18, 2)          DEFAULT NULL COMMENT '信用额度，审核销售订单时校验未完成订单金额',
    payment_days INT            NOT NULL DEFAULT 0 COMMENT '账期（天），应收单到期日 = 单据日期 + 账期',
    channel      VARCHAR(16)    NOT NULL DEFAULT 'DIRECT' COMMENT '渠道(字典 scm_channel) DIRECT直销 DEALER经销商',
    region       VARCHAR(128)            DEFAULT NULL COMMENT '经销商授权区域',
    owner_id     BIGINT                  DEFAULT NULL COMMENT '负责销售：经销商门户订单以其为制单人（按其数据权限可见）',
    status       TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户';

CREATE TABLE scm_order (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_no         VARCHAR(32)    NOT NULL COMMENT '订单号',
    kind             VARCHAR(16)    NOT NULL COMMENT '订单种类 PURCHASE采购 SALES销售',
    partner_id       BIGINT         NOT NULL COMMENT '供应商 / 客户',
    order_date       DATE           NOT NULL COMMENT '订单日期',
    delivery_date    DATE                    DEFAULT NULL COMMENT '交货日期',
    warehouse_id     BIGINT                  DEFAULT NULL COMMENT '默认收货 / 发货仓库',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_order_status)',
    total_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    source           VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 scm_order_source) MANUAL手工 MRP物料需求计划 CRM商机 KIT生产缺料',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    contract_id      BIGINT                  DEFAULT NULL COMMENT '引用的采购合同（按合同价下单）',
    supplier_confirm VARCHAR(16)             DEFAULT NULL COMMENT '供应商确认(字典 scm_supplier_confirm) ACCEPTED已接受 REJECTED已拒绝，为空未确认',
    promised_date    DATE                    DEFAULT NULL COMMENT '供应商承诺交期',
    confirm_remark   VARCHAR(255)            DEFAULT NULL COMMENT '供应商确认说明',
    confirm_time     DATETIME                DEFAULT NULL COMMENT '供应商确认时间',
    discount_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '促销优惠合计（原价 - 成交价），已从金额中扣除',
    rebate_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '使用返利抵扣的金额（按行金额分摊到单价），已从金额中扣除',
    external_no      VARCHAR(64)             DEFAULT NULL COMMENT '外部单号（电商 / 门店订单导入），同一租户不重复',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    version          INT            NOT NULL DEFAULT 1 COMMENT '版本：每次变更单生效后加 1',
    currency         VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种：订单单价、金额按此币种',
    exchange_rate    DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（1 外币 = 汇率 × 本位币），出入库、应收应付按此折算本位币',
    salesman_id      BIGINT                  DEFAULT NULL COMMENT '业务员（销售订单，计算销售提成）：默认客户的负责销售，没有时为制单人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    approve_by       BIGINT                  DEFAULT NULL COMMENT '审核人',
    approve_time     DATETIME                DEFAULT NULL COMMENT '审核时间',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no, tenant_id),
    KEY idx_order_kind (kind, id),
    KEY idx_order_partner (partner_id),
    KEY idx_order_dept (dept_id),
    KEY idx_order_contract (contract_id),
    UNIQUE KEY uk_order_external (external_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购 / 销售订单';

CREATE TABLE scm_order_change (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    change_no        VARCHAR(32)  NOT NULL COMMENT '变更单号',
    order_id         BIGINT       NOT NULL COMMENT '订单',
    version          INT          NOT NULL COMMENT '生效后的订单版本',
    reason           VARCHAR(255) NOT NULL COMMENT '变更原因',
    status           VARCHAR(16)  NOT NULL DEFAULT 'APPROVING' COMMENT '状态(字典 scm_change_status) APPROVING审批中 APPROVED已生效 REJECTED已驳回',
    old_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '变更前价税合计',
    new_amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '变更后价税合计',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例',
    approve_time     DATETIME              DEFAULT NULL COMMENT '生效时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '申请人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_change_no (change_no, tenant_id),
    KEY idx_order_change_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单变更单：已审核订单的数量、单价、交期变更，审批通过后生效并升版本';

CREATE TABLE scm_order_change_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    change_id         BIGINT         NOT NULL COMMENT '变更单',
    action            VARCHAR(8)     NOT NULL COMMENT '操作 ADD新增 MODIFY修改 REMOVE删除',
    order_item_id     BIGINT                  DEFAULT NULL COMMENT '订单明细（新增行为空，生效后回填）',
    material_id       BIGINT         NOT NULL COMMENT '物料',
    old_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '原数量',
    new_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '新数量',
    old_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '原单价',
    new_price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '新单价',
    tax_rate          DECIMAL(5, 2)           DEFAULT NULL COMMENT '税率（新增行）',
    old_delivery_date DATE                    DEFAULT NULL COMMENT '原交货日期',
    new_delivery_date DATE                    DEFAULT NULL COMMENT '新交货日期',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_change_item_change (change_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单变更明细';

CREATE TABLE scm_order_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id      BIGINT         NOT NULL COMMENT '订单ID',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '订购数量',
    unit_name     VARCHAR(16)             DEFAULT NULL COMMENT '录入单位（辅助单位），为空表示基本单位；数量、单价均按基本单位保存',
    unit_ratio    DECIMAL(18, 6)          DEFAULT NULL COMMENT '录入单位的换算率',
    price         DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate      DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '税率（%）',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount    DECIMAL(18, 2) NOT NULL COMMENT '税额',
    done_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已入库 / 已出库数量',
    return_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已退货数量',
    replenish_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '退货后需要补发（补收）的数量，计入未完成数量',
    orig_price    DECIMAL(18, 4)          DEFAULT NULL COMMENT '原价（促销、返利抵扣前的含税单价），没有优惠时为空',
    promotion_id  BIGINT                  DEFAULT NULL COMMENT '应用的折扣促销 / 赠品来源的买赠促销',
    gift          TINYINT        NOT NULL DEFAULT 0 COMMENT '1 赠品（单价为 0，由买赠促销自动生成）',
    delivery_date DATE                    DEFAULT NULL COMMENT '交货日期',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';

-- 促销：按期间、客户等级、物料 / 分类设置折扣、满减、买赠，手工新建 / 修改销售订单时自动应用（先折扣，再满减，再返利抵扣）
CREATE TABLE scm_promotion (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '促销名称',
    promo_type        VARCHAR(16)    NOT NULL COMMENT '类型(字典 scm_promo_type) DISCOUNT折扣 REDUCE满减 GIFT买赠',
    begin_date        DATE           NOT NULL COMMENT '开始日期（按订单日期匹配）',
    end_date          DATE           NOT NULL COMMENT '结束日期',
    customer_level    VARCHAR(16)             DEFAULT NULL COMMENT '客户等级(字典 scm_customer_level)，为空适用全部客户',
    material_id       BIGINT                  DEFAULT NULL COMMENT '适用物料，为空按分类',
    material_category VARCHAR(16)             DEFAULT NULL COMMENT '适用物料分类(字典 wms_material_category)，物料和分类都为空适用全部物料',
    min_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '折扣：明细数量达到时才打折',
    discount_rate     DECIMAL(5, 2)           DEFAULT NULL COMMENT '折扣：成交价 = 原价 × 折扣率%，如 90 为九折',
    threshold_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减：适用物料的金额（折扣后）达到此金额',
    reduce_amount     DECIMAL(18, 2)          DEFAULT NULL COMMENT '满减：减免金额，按行金额分摊到单价',
    buy_qty           DECIMAL(18, 4)          DEFAULT NULL COMMENT '买赠：适用物料每购买此数量',
    gift_material_id  BIGINT                  DEFAULT NULL COMMENT '买赠：赠品物料',
    gift_qty          DECIMAL(18, 4)          DEFAULT NULL COMMENT '买赠：每次赠送数量',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_by         BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_promotion_date (end_date, begin_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销活动';

CREATE TABLE scm_promotion_usage (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    promotion_id    BIGINT         NOT NULL COMMENT '促销',
    order_id        BIGINT         NOT NULL COMMENT '销售订单',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    gift_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '赠品数量',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_promotion_usage (promotion_id),
    KEY idx_promotion_usage_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '促销应用记录（订单保存时重算）';

-- 返利：客户 × 期间的销售额阶梯返利协议，期末按销售出库 - 退货的含税金额计算，达到的最高一档返利率 × 全部金额
CREATE TABLE scm_rebate (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    rebate_no     VARCHAR(32)    NOT NULL COMMENT '协议号',
    customer_id   BIGINT         NOT NULL COMMENT '客户',
    begin_date    DATE           NOT NULL COMMENT '期间开始',
    end_date      DATE           NOT NULL COMMENT '期间结束',
    settle_mode   VARCHAR(16)    NOT NULL COMMENT '兑付方式(字典 scm_rebate_settle) CREDIT_NOTE红字应收 NEXT_ORDER下次订单抵扣',
    status        VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_rebate_status) ACTIVE执行中 SETTLED已结算',
    sales_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '结算：期间销售额（出库 - 退货，含税）',
    rate          DECIMAL(5, 2)           DEFAULT NULL COMMENT '结算：返利率（%）',
    rebate_amount DECIMAL(18, 2)          DEFAULT NULL COMMENT '结算：返利金额',
    settle_date   DATE                    DEFAULT NULL COMMENT '结算日期',
    settle_by     BIGINT                  DEFAULT NULL COMMENT '结算人',
    bill_id       BIGINT                  DEFAULT NULL COMMENT '红字应收单（兑付方式为红字应收时）',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_by     BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rebate_no (rebate_no, tenant_id),
    KEY idx_rebate_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售返利协议';

CREATE TABLE scm_rebate_tier (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    rebate_id  BIGINT         NOT NULL COMMENT '返利协议',
    min_amount DECIMAL(18, 2) NOT NULL COMMENT '销售额达到',
    rate       DECIMAL(5, 2)  NOT NULL COMMENT '返利率（%）',
    PRIMARY KEY (id),
    KEY idx_rebate_tier (rebate_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '返利阶梯';

-- 渠道库存与销量上报：经销商在门户按月上报各物料的期末库存和当月销量，同一期间重复上报覆盖
CREATE TABLE scm_channel_report (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    customer_id BIGINT       NOT NULL COMMENT '经销商（客户）',
    period      INT          NOT NULL COMMENT '期间 yyyyMM',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    report_by   BIGINT                DEFAULT NULL COMMENT '上报人（经销商门户账号）',
    report_time DATETIME              DEFAULT NULL COMMENT '上报时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_report (customer_id, period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '渠道库存销量上报';

CREATE TABLE scm_channel_report_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    report_id   BIGINT         NOT NULL COMMENT '上报',
    material_id BIGINT         NOT NULL COMMENT '物料',
    sell_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '当月销量（卖给终端客户）',
    stock_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '期末渠道库存',
    PRIMARY KEY (id),
    KEY idx_channel_report_item (report_id),
    KEY idx_channel_report_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '渠道库存销量上报明细';

CREATE TABLE scm_trade_doc (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 scm_trade_kind) PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 SALES_OUT销售出库 SALES_RETURN销售退货',
    order_id     BIGINT         NOT NULL COMMENT '订单ID',
    partner_id   BIGINT         NOT NULL COMMENT '供应商 / 客户',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计',
    tax_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    currency     VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '订单币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率（取订单汇率）；单据金额为本位币',
    amount_fc    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '价税合计（原币）',
    wms_doc_id   BIGINT                  DEFAULT NULL COMMENT '生成的出入库单ID',
    wms_doc_no   VARCHAR(32)             DEFAULT NULL COMMENT '生成的出入库单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    replenish    TINYINT        NOT NULL DEFAULT 0 COMMENT '退货是否补发（补收） 1补发 0退款',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_doc_no (doc_no, tenant_id),
    KEY idx_trade_doc_kind (kind, id),
    KEY idx_trade_doc_order (order_id),
    KEY idx_trade_doc_date (biz_date),
    KEY idx_trade_doc_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 采购退货 / 销售出库 / 销售退货';

CREATE TABLE scm_trade_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    order_item_id   BIGINT         NOT NULL COMMENT '订单明细ID',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅采购入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅采购入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price           DECIMAL(18, 4) NOT NULL COMMENT '含税单价（取订单）',
    tax_rate        DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '税率（%）',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount      DECIMAL(18, 2) NOT NULL COMMENT '税额',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trade_doc_item_doc (doc_id),
    KEY idx_trade_doc_item_order_item (order_item_id),
    KEY idx_trade_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购入库 / 退货、销售出库 / 退货明细';

CREATE TABLE scm_shipment (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ship_no      VARCHAR(32)    NOT NULL COMMENT '发运单号',
    trade_doc_id BIGINT         NOT NULL COMMENT '销售出库单ID',
    customer_id  BIGINT         NOT NULL COMMENT '客户',
    carrier      VARCHAR(32)    NOT NULL COMMENT '承运商(字典 scm_carrier)',
    tracking_no  VARCHAR(64)             DEFAULT NULL COMMENT '运单号',
    freight      DECIMAL(18, 2)          DEFAULT NULL COMMENT '运费',
    carrier_supplier_id BIGINT              DEFAULT NULL COMMENT '承运商对应的供应商（运费应付）',
    freight_bill_id     BIGINT              DEFAULT NULL COMMENT '运费应付单',
    receiver     VARCHAR(64)             DEFAULT NULL COMMENT '收货人',
    phone        VARCHAR(32)             DEFAULT NULL COMMENT '收货电话',
    address      VARCHAR(255)            DEFAULT NULL COMMENT '收货地址',
    ship_date    DATE                    DEFAULT NULL COMMENT '发货日期',
    sign_date    DATE                    DEFAULT NULL COMMENT '签收日期',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_shipment_status) PENDING待发运 SHIPPED运输中 SIGNED已签收 EXCEPTION异常',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ship_no (ship_no, tenant_id),
    KEY idx_shipment_trade_doc (trade_doc_id),
    KEY idx_shipment_tracking (tracking_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物流发运单';

CREATE TABLE scm_delivery (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_no  VARCHAR(32)  NOT NULL COMMENT '发货通知单号',
    order_id     BIGINT       NOT NULL COMMENT '销售订单',
    customer_id  BIGINT       NOT NULL COMMENT '客户',
    warehouse_id BIGINT       NOT NULL COMMENT '发货仓库',
    ship_date    DATE         NOT NULL COMMENT '计划发货日期',
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态(字典 scm_delivery_status) PENDING待拣货 PICKED已拣货 SHIPPED已出库 CANCELLED已取消',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    trade_doc_id BIGINT                DEFAULT NULL COMMENT '生成的销售出库单',
    wave_id      BIGINT                DEFAULT NULL COMMENT '所属拣货波次',
    pick_by      BIGINT                DEFAULT NULL COMMENT '拣货人',
    pick_time    DATETIME              DEFAULT NULL COMMENT '拣货时间',
    dept_id      BIGINT                DEFAULT NULL COMMENT '部门',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_no (delivery_no, tenant_id),
    KEY idx_delivery_order (order_id),
    KEY idx_delivery_status (status),
    KEY idx_delivery_wave (wave_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货通知单';

-- 拣货波次：同一仓库的多张待拣货发货通知合并拣货，拣货单按库位路径（区域 → 排序 → 编码）汇总，再按单分拣
CREATE TABLE scm_wave (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    wave_no      VARCHAR(32)  NOT NULL COMMENT '波次号',
    warehouse_id BIGINT       NOT NULL COMMENT '仓库',
    status       VARCHAR(16)  NOT NULL COMMENT '状态(字典 scm_wave_status) PICKING拣货中 DONE已完成 CANCELLED已取消',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    pick_by      BIGINT                DEFAULT NULL COMMENT '完成拣货人',
    pick_time    DATETIME              DEFAULT NULL COMMENT '完成拣货时间',
    dept_id      BIGINT                DEFAULT NULL COMMENT '部门',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wave_no (wave_no, tenant_id),
    KEY idx_wave_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '拣货波次';

-- 装箱：已拣货的发货通知按箱录入箱号、重量、体积和箱内物料，每个通知明细装箱合计不超过通知数量
CREATE TABLE scm_delivery_package (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_id BIGINT         NOT NULL COMMENT '发货通知',
    box_no      VARCHAR(48)    NOT NULL COMMENT '箱号：发货通知号-序号',
    seq         INT            NOT NULL COMMENT '箱序号',
    weight      DECIMAL(10, 3)          DEFAULT NULL COMMENT '毛重（kg）',
    volume      DECIMAL(10, 4)          DEFAULT NULL COMMENT '体积（m³）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '装箱人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_package_box (box_no, tenant_id),
    KEY idx_delivery_package (delivery_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货装箱';

CREATE TABLE scm_delivery_package_item (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    package_id       BIGINT         NOT NULL COMMENT '箱',
    delivery_item_id BIGINT         NOT NULL COMMENT '发货通知明细',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    qty              DECIMAL(18, 4) NOT NULL COMMENT '数量',
    PRIMARY KEY (id),
    KEY idx_delivery_package_item (package_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '装箱明细';

CREATE TABLE scm_delivery_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_id   BIGINT         NOT NULL COMMENT '发货通知',
    order_item_id BIGINT         NOT NULL COMMENT '订单明细',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '通知数量',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_delivery_item_delivery (delivery_id),
    KEY idx_delivery_item_order_item (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发货通知明细';

CREATE TABLE scm_delivery_pick (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    delivery_id      BIGINT         NOT NULL COMMENT '发货通知',
    delivery_item_id BIGINT         NOT NULL COMMENT '通知明细',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    location_id      BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 表示未分库位',
    batch_no         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    qty              DECIMAL(18, 4) NOT NULL COMMENT '拣货数量',
    serial_nos       TEXT                    DEFAULT NULL COMMENT '序列号，逗号分隔',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_delivery_pick_delivery (delivery_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '拣货明细（待拣货时为先到期先出的建议）';

CREATE TABLE scm_shipment_track (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    shipment_id BIGINT       NOT NULL COMMENT '发运单ID',
    track_time  DATETIME     NOT NULL COMMENT '时间',
    status      VARCHAR(16)           DEFAULT NULL COMMENT '记录时的发运状态',
    content     VARCHAR(255) NOT NULL COMMENT '物流动态',
    create_by   BIGINT                DEFAULT NULL COMMENT '记录人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shipment_track (shipment_id, track_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物流跟踪记录';

-- ---------------------------------------------------------------- 生产制造

CREATE TABLE mes_work_center (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code          VARCHAR(32)    NOT NULL COMMENT '工作中心编码',
    name          VARCHAR(64)    NOT NULL COMMENT '工作中心名称，如 SMT 线、组装线',
    labor_rate    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '人工费率（元 / 工时），成本核算按报工工时计算直接人工',
    overhead_rate DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '制造费用率（元 / 工时），成本核算按报工工时分摊制造费用',
    daily_hours   DECIMAL(6, 2)  NOT NULL DEFAULT 8 COMMENT '每个资源每个工作日的可用工时',
    resources     INT            NOT NULL DEFAULT 1 COMMENT '资源数（设备台数 / 班组人数），日产能 = 每日工时 × 资源数',
    work_days     VARCHAR(16)    NOT NULL DEFAULT '1,2,3,4,5' COMMENT '工作日（1 周一 … 7 周日，逗号分隔），工厂日历可按日期调整',
    status        TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_center_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工作中心';

CREATE TABLE mes_calendar (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    work_center_id BIGINT        NOT NULL DEFAULT 0 COMMENT '工作中心，0 表示全厂',
    cal_date       DATE          NOT NULL COMMENT '日期',
    hours          DECIMAL(6, 2) NOT NULL COMMENT '当天每个资源的可用工时，0 表示休息',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '说明，如 国庆放假、周六加班',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_calendar (work_center_id, cal_date, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工厂日历：按日期调整工作日（放假、加班），工作中心的设置优先于全厂';

CREATE TABLE mes_bom (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT       NOT NULL COMMENT '产品（自制件）',
    version     VARCHAR(16)  NOT NULL COMMENT '版本号，如 V1.0',
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 BOM 1是 0否：MRP 展开、新建工单默认使用，每个产品只有一个',
    lead_days   INT          NOT NULL DEFAULT 0 COMMENT '生产提前期（天），MRP 按此倒推开工日期',
    ecn_id      BIGINT                DEFAULT NULL COMMENT '由工程变更创建的版本：变更生效前停用，只能在变更草稿时修改',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bom (material_id, version, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料清单（BOM）';

CREATE TABLE mes_bom_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    bom_id      BIGINT         NOT NULL COMMENT 'BOM ID',
    material_id BIGINT         NOT NULL COMMENT '子件物料',
    qty         DECIMAL(18, 6) NOT NULL COMMENT '单位用量：生产 1 个产品需要的数量',
    loss_rate   DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '损耗率（%），需求数量 = 用量 × (1 + 损耗率)',
    ref_des     VARCHAR(1000)           DEFAULT NULL COMMENT '位号，逗号分隔，连续的可写成区间 C1-C8',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bom_item_bom (bom_id),
    -- 反查物料被哪些 BOM 使用
    KEY idx_bom_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'BOM 子件';

CREATE TABLE mes_bom_op (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    bom_id         BIGINT         NOT NULL COMMENT 'BOM ID',
    seq            INT            NOT NULL COMMENT '工序号，如 10、20',
    name           VARCHAR(64)    NOT NULL COMMENT '工序名称',
    work_center_id BIGINT         NOT NULL COMMENT '工作中心',
    std_hours      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '标准工时（小时 / 个）',
    piece_rate     DECIMAL(18, 4)          DEFAULT NULL COMMENT '计件单价（元 / 合格品），为空不计件',
    first_article  TINYINT        NOT NULL DEFAULT 0 COMMENT '首件检验：工单该工序须首件检验合格后才能继续报工',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bom_op_bom (bom_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工艺路线（BOM 工序）';

CREATE TABLE mes_plan (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_no        VARCHAR(32)    NOT NULL COMMENT '计划号',
    material_id    BIGINT         NOT NULL COMMENT '产品',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '计划数量',
    due_date       DATE           NOT NULL COMMENT '需求日期',
    source         VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工 SALES销售订单',
    sales_order_id BIGINT                  DEFAULT NULL COMMENT '来源销售订单',
    sales_order_no VARCHAR(32)             DEFAULT NULL COMMENT '来源销售订单号',
    status         VARCHAR(16)    NOT NULL COMMENT '状态(字典 mes_plan_status) DRAFT草稿 CONFIRMED已确认 CLOSED已关闭',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id        BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by      BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_no (plan_no, tenant_id),
    KEY idx_plan_status (status, due_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '主生产计划';

CREATE TABLE mes_mrp_run (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    run_no       VARCHAR(32)  NOT NULL COMMENT '运算号',
    plan_nos     TEXT COMMENT '参与运算的计划号，逗号分隔',
    sources      VARCHAR(64)             DEFAULT NULL COMMENT '需求来源：生产计划、销售订单、销售预测',
    result_count INT          NOT NULL DEFAULT 0 COMMENT '结果行数',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                DEFAULT NULL COMMENT '运算人',
    create_time  DATETIME              DEFAULT NULL COMMENT '运算时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mrp_run_no (run_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'MRP 运算记录';

CREATE TABLE mes_forecast (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id BIGINT         NOT NULL COMMENT '物料',
    period      INT            NOT NULL COMMENT '月份 yyyyMM',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '预测数量',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_forecast (material_id, period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售预测';

CREATE TABLE mes_outsource_material (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id      BIGINT         NOT NULL COMMENT '委外采购订单',
    order_item_id BIGINT         NOT NULL COMMENT '订单明细（委外件）',
    product_id    BIGINT         NOT NULL COMMENT '委外件',
    material_id   BIGINT         NOT NULL COMMENT '子件',
    unit_qty      DECIMAL(18, 6) NOT NULL COMMENT '单位用量（含损耗）',
    required_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '需求数量',
    issued_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已发料',
    returned_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已退料',
    consumed_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '收货已核销',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_outsource_material_order (order_id),
    KEY idx_outsource_material_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '委外用料';

CREATE TABLE mes_outsource_consume (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_id     BIGINT         NOT NULL COMMENT '委外采购订单',
    trade_doc_id BIGINT         NOT NULL COMMENT '采购入库 / 退货单',
    product_id   BIGINT         NOT NULL COMMENT '委外件',
    material_id  BIGINT         NOT NULL COMMENT '子件',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '核销数量，退货为负数',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_outsource_consume_date (biz_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '委外收货核销用料';

CREATE TABLE mes_mrp_result (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    run_id        BIGINT         NOT NULL COMMENT '运算ID',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    level_no      INT            NOT NULL DEFAULT 0 COMMENT '低层码：0 为产品，越大越靠近原材料',
    supply_type   VARCHAR(16)    NOT NULL COMMENT '供应方式(字典 mes_supply_type) MAKE自制 BUY采购 OUTSOURCE委外 PHANTOM虚拟件',
    bom_id        BIGINT                  DEFAULT NULL COMMENT '自制件使用的 BOM',
    gross_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '毛需求',
    on_hand_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '现有库存',
    scheduled_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '在途：未完成的采购订单 / 生产工单',
    allocated_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已分配：未完成工单尚未领用的数量',
    safety_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '安全库存',
    net_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '净需求 = 毛需求 - (库存 + 在途 - 已分配 - 安全库存)，不小于 0',
    suggest_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '建议数量：净需求按整数（序列号物料）、最小起订量取整',
    required_date DATE                    DEFAULT NULL COMMENT '需求日期',
    release_date  DATE                    DEFAULT NULL COMMENT '建议下单 / 开工日期 = 需求日期 - 提前期',
    supplier_id   BIGINT                  DEFAULT NULL COMMENT '采购件的建议供应商（默认供应商）',
    price         DECIMAL(18, 4)          DEFAULT NULL COMMENT '采购件的含税单价（供货价格）',
    status        VARCHAR(16)    NOT NULL COMMENT '状态 NEW待处理 RELEASED已下达 NONE无需处理 EXPIRED已作废（之后又运算了 MRP）',
    release_no    VARCHAR(32)             DEFAULT NULL COMMENT '下达生成的工单号 / 采购订单号',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_mrp_result_run (run_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'MRP 运算结果';

CREATE TABLE mes_work_order (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    wo_no        VARCHAR(32)    NOT NULL COMMENT '工单号',
    material_id  BIGINT         NOT NULL COMMENT '产品',
    bom_id       BIGINT         NOT NULL COMMENT 'BOM',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '计划生产数量',
    wo_type      VARCHAR(16)    NOT NULL DEFAULT 'NORMAL' COMMENT '工单类型(字典 mes_wo_type) NORMAL普通 REWORK返工（用料为产品本身）',
    plan_start   DATE                    DEFAULT NULL COMMENT '计划开工日期',
    plan_end     DATE                    DEFAULT NULL COMMENT '计划完工日期',
    warehouse_id BIGINT                  DEFAULT NULL COMMENT '默认完工入库仓库',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 mes_wo_status) DRAFT草稿 RELEASED已下达 STARTED生产中 COMPLETED已完工 CLOSED已关闭',
    finished_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已完工入库数量',
    source       VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工 MRP PLAN计划转工单 KIT生产缺料 SPLIT拆分 REWORK返工',
    plan_id      BIGINT                  DEFAULT NULL COMMENT '来源生产计划',
    source_wo_id BIGINT                  DEFAULT NULL COMMENT '来源工单：拆分自、返工自',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单人部门（数据权限）',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    release_time DATETIME                DEFAULT NULL COMMENT '下达时间',
    finish_time  DATETIME                DEFAULT NULL COMMENT '完工时间',
    close_time   DATETIME                DEFAULT NULL COMMENT '关闭时间，成本核算在关闭当期转出剩余在产品',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wo_no (wo_no, tenant_id),
    KEY idx_wo_status (status),
    KEY idx_wo_material (material_id),
    KEY idx_wo_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '生产工单';

CREATE TABLE mes_wo_material (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    wo_id        BIGINT         NOT NULL COMMENT '工单ID',
    material_id  BIGINT         NOT NULL COMMENT '子件物料',
    unit_qty     DECIMAL(18, 6) NOT NULL COMMENT '单位用量（含损耗），下达时取自 BOM',
    required_qty DECIMAL(18, 4) NOT NULL COMMENT '需求数量 = 工单数量 × 单位用量',
    issued_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已领数量（含补料，扣除退料）',
    supplement_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已补料数量，领料上限 = 需求数量 × (1 + 允许超领比例) + 已补料',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wo_material (wo_id, material_id, tenant_id),
    KEY idx_wo_material_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单用料';

CREATE TABLE mes_wo_op (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    wo_id          BIGINT         NOT NULL COMMENT '工单ID',
    seq            INT            NOT NULL COMMENT '工序号',
    name           VARCHAR(64)    NOT NULL COMMENT '工序名称',
    work_center_id BIGINT         NOT NULL COMMENT '工作中心',
    std_hours      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '标准工时（小时 / 个）',
    piece_rate     DECIMAL(18, 4)          DEFAULT NULL COMMENT '计件单价（元 / 合格品）',
    first_article  TINYINT        NOT NULL DEFAULT 0 COMMENT '首件检验（来自工艺路线）',
    good_qty       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '累计合格数量',
    scrap_qty      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '累计报废数量',
    hours          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '累计实际工时',
    plan_start     DATE                    DEFAULT NULL COMMENT '排程开始日期',
    plan_end       DATE                    DEFAULT NULL COMMENT '排程结束日期',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_wo_op_wo (wo_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单工序';

CREATE TABLE mes_report (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    wo_id       BIGINT         NOT NULL COMMENT '工单ID',
    op_id       BIGINT         NOT NULL COMMENT '工单工序ID',
    report_date DATE           NOT NULL COMMENT '报工日期',
    good_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合格数量',
    scrap_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '报废数量',
    hours       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '实际工时（小时）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '报工人',
    operator_id BIGINT                  DEFAULT NULL COMMENT '操作员（计件工资归属）',
    piece_wage  DECIMAL(18, 2)          DEFAULT NULL COMMENT '计件工资 = 合格数 × 计件单价',
    equipment_id BIGINT                 DEFAULT NULL COMMENT '生产设备（设备 OEE 的产量和性能）',
    mold_id     BIGINT                  DEFAULT NULL COMMENT '使用的模具 / 工装',
    shots       INT                     DEFAULT NULL COMMENT '模次 = ⌈(合格 + 报废) / 模穴数⌉，累计到模具使用次数',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_report_wo (wo_id),
    KEY idx_report_date (report_date),
    KEY idx_report_operator (operator_id, report_date),
    KEY idx_report_equipment (equipment_id, report_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报工记录';

CREATE TABLE mes_wo_doc (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '单据号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类(字典 mes_wo_doc_kind) ISSUE领料 RETURN退料 FINISH完工入库 SUPPLEMENT补料 SCRAP报废入库',
    wo_id        BIGINT         NOT NULL COMMENT '工单ID',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    total_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    wms_doc_id   BIGINT                  DEFAULT NULL COMMENT '生成的出入库单ID',
    wms_doc_no   VARCHAR(32)             DEFAULT NULL COMMENT '生成的出入库单号',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wo_doc_no (doc_no, tenant_id),
    KEY idx_wo_doc_wo (wo_id),
    KEY idx_wo_doc_date (biz_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单领料 / 退料 / 完工入库 / 补料';

CREATE TABLE mes_wo_doc_item (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_id          BIGINT         NOT NULL COMMENT '单据ID',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    location_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '库位，0 为未指定',
    batch_no        VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '批次号',
    production_date DATE                    DEFAULT NULL COMMENT '生产日期，仅完工入库',
    expiry_date     DATE                    DEFAULT NULL COMMENT '有效期至，仅完工入库',
    qty             DECIMAL(18, 4) NOT NULL COMMENT '数量',
    serial_nos      TEXT COMMENT '序列号，逗号分隔',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_wo_doc_item_doc (doc_id),
    KEY idx_wo_doc_item_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单领料 / 退料 / 完工入库 / 补料明细';

CREATE TABLE mes_material_plan (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id     BIGINT         NOT NULL COMMENT '物料',
    supply_type     VARCHAR(16)             DEFAULT NULL COMMENT '物料属性(字典 mes_supply_type) MAKE自制 BUY采购 OUTSOURCE委外 PHANTOM虚拟件，为空时有默认 BOM 为自制、否则为采购',
    lead_days       INT                     DEFAULT NULL COMMENT '提前期（天），为空时自制件取 BOM、采购件取供货价格的提前期',
    min_lot         DECIMAL(18, 4)          DEFAULT NULL COMMENT '最小批量：MRP 建议数量不低于该值',
    lot_multiple    DECIMAL(18, 4)          DEFAULT NULL COMMENT '批量倍数：MRP 建议数量向上取整到该值的整数倍',
    over_issue_rate DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '允许超领比例（%），超出部分须走补料申请',
    planner         VARCHAR(64)             DEFAULT NULL COMMENT '计划员',
    fqc_required    TINYINT        NOT NULL DEFAULT 0 COMMENT '完工需成品检验 1是：完工入库累计不超过已确认的成品检验合格数',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_plan (material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料计划属性';

CREATE TABLE mes_material_substitute (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    material_id   BIGINT         NOT NULL COMMENT '主料',
    substitute_id BIGINT         NOT NULL COMMENT '替代料',
    ratio         DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '替代比例：替代 1 个主料需要的替代料数量',
    priority      INT            NOT NULL DEFAULT 1 COMMENT '优先级，越小越优先',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注，如适用条件',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_substitute (material_id, substitute_id, tenant_id),
    KEY idx_substitute_sub (substitute_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '替代料';

CREATE TABLE mes_wo_supplement (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supp_no          VARCHAR(32)    NOT NULL COMMENT '申请单号',
    wo_id            BIGINT         NOT NULL COMMENT '工单ID',
    reason           VARCHAR(16)    NOT NULL COMMENT '补料原因(字典 mes_supplement_reason)',
    total_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '申请数量合计',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 mes_supplement_status) APPROVING审批中 APPROVED待发料 REJECTED已拒绝 CANCELED已撤销 DONE已发料',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例，未走流程为空',
    approve_time     DATETIME                DEFAULT NULL COMMENT '审批完成时间',
    doc_id           BIGINT                  DEFAULT NULL COMMENT '发料生成的工单单据',
    doc_no           VARCHAR(32)             DEFAULT NULL COMMENT '发料单号',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '申请人部门',
    create_by        BIGINT                  DEFAULT NULL COMMENT '申请人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supp_no (supp_no, tenant_id),
    KEY idx_supp_wo (wo_id),
    KEY idx_supp_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '补料申请';

CREATE TABLE mes_wo_supplement_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    supp_id     BIGINT         NOT NULL COMMENT '补料申请ID',
    material_id BIGINT         NOT NULL COMMENT '物料，须为工单用料',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '申请数量',
    issued_qty  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '实发数量',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_supp_item_supp (supp_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '补料申请明细';

CREATE TABLE mes_inspection (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    insp_no       VARCHAR(32)    NOT NULL COMMENT '检验单号',
    type          VARCHAR(16)    NOT NULL COMMENT '检验类型(字典 mes_qc_type) IQC来料 IPQC过程 FQC成品 FAI首件',
    source_no     VARCHAR(32)             DEFAULT NULL COMMENT '来源单号：采购入库单 / 生产工单',
    supplier_id   BIGINT                  DEFAULT NULL COMMENT '供应商（来料检验）',
    wo_id         BIGINT                  DEFAULT NULL COMMENT '生产工单（过程、成品、首件检验）',
    wo_op_id      BIGINT                  DEFAULT NULL COMMENT '工单工序（首件检验）',
    plan_id       BIGINT                  DEFAULT NULL COMMENT '检验方案',
    sample_code   VARCHAR(4)              DEFAULT NULL COMMENT '样本量字码（AQL 抽样）',
    accept_qty    INT                     DEFAULT NULL COMMENT '允收不良数 Ac',
    reject_qty    INT                     DEFAULT NULL COMMENT '拒收不良数 Re',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    batch_no      VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    inspect_qty   DECIMAL(18, 4) NOT NULL COMMENT '送检数量',
    sample_qty    DECIMAL(18, 4) NOT NULL COMMENT '抽检数量',
    defect_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '不良数量',
    result        VARCHAR(16)    NOT NULL COMMENT '检验结论(字典 mes_qc_result) PASS合格 FAIL不合格 CONCESSION让步接收',
    defect_type   VARCHAR(32)             DEFAULT NULL COMMENT '主要不良类型(字典 mes_defect_type)',
    defect_desc   VARCHAR(255)            DEFAULT NULL COMMENT '不良描述',
    inspect_date  DATE           NOT NULL COMMENT '检验日期',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    status        VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 CONFIRMED已确认（不能修改删除）',
    confirm_by    BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time  DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_by     BIGINT                  DEFAULT NULL COMMENT '检验员',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_insp_no (insp_no, tenant_id),
    KEY idx_inspection_type_date (type, inspect_date),
    KEY idx_inspection_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '质量检验单';

CREATE TABLE mes_equipment (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code                VARCHAR(32)  NOT NULL COMMENT '设备编码',
    name                VARCHAR(64)  NOT NULL COMMENT '设备名称',
    model               VARCHAR(64)           DEFAULT NULL COMMENT '规格型号',
    work_center_id      BIGINT                DEFAULT NULL COMMENT '所属工作中心',
    location            VARCHAR(64)           DEFAULT NULL COMMENT '安装位置',
    purchase_date       DATE                  DEFAULT NULL COMMENT '购置日期',
    status              VARCHAR(16)  NOT NULL COMMENT '状态(字典 mes_equipment_status) RUNNING运行 IDLE闲置 REPAIR维修中 SCRAPPED报废',
    maintain_cycle_days INT                   DEFAULT NULL COMMENT '保养周期（天），为空不做定期保养',
    last_maintain_date  DATE                  DEFAULT NULL COMMENT '上次保养日期',
    next_maintain_date  DATE                  DEFAULT NULL COMMENT '下次保养日期 = 上次保养日期 + 保养周期',
    remark              VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time         DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_code (code, tenant_id),
    KEY idx_equipment_next_maintain (next_maintain_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备台账';

CREATE TABLE mes_maintenance (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    order_no     VARCHAR(32)    NOT NULL COMMENT '维护单号',
    equipment_id BIGINT         NOT NULL COMMENT '设备',
    type         VARCHAR(16)    NOT NULL COMMENT '类型(字典 mes_maintain_type) REPAIR故障维修 MAINTAIN定期保养',
    content      VARCHAR(255)   NOT NULL COMMENT '故障现象 / 保养内容',
    plan_date    DATE                    DEFAULT NULL COMMENT '计划日期',
    status       VARCHAR(16)    NOT NULL COMMENT '状态(字典 mes_maintain_status) PENDING待处理 IN_PROGRESS处理中 DONE已完成',
    prev_status  VARCHAR(16)             DEFAULT NULL COMMENT '开始维修时设备的状态，完成后恢复',
    handler      VARCHAR(64)             DEFAULT NULL COMMENT '处理人',
    start_time   DATETIME                DEFAULT NULL COMMENT '开始时间',
    finish_time  DATETIME                DEFAULT NULL COMMENT '完成时间',
    downtime     DECIMAL(10, 2)          DEFAULT NULL COMMENT '停机时长（小时），维修开始到完成',
    cost         DECIMAL(18, 2)          DEFAULT NULL COMMENT '费用',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '费用凭证',
    result       VARCHAR(255)            DEFAULT NULL COMMENT '处理结果',
    create_by    BIGINT                  DEFAULT NULL COMMENT '报修 / 制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_maintenance_no (order_no, tenant_id),
    KEY idx_maintenance_equipment (equipment_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备维修 / 保养单';

-- 设备停机：手工登记或维护单完成时自动记录；计划停机（保养、无生产计划）从计划时间中扣除，其余计入可用率损失
CREATE TABLE mes_equipment_stop (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    equipment_id   BIGINT        NOT NULL COMMENT '设备',
    start_time     DATETIME      NOT NULL COMMENT '开始时间',
    end_time       DATETIME      NOT NULL COMMENT '结束时间',
    hours          DECIMAL(10, 2) NOT NULL COMMENT '停机时长（小时）',
    reason         VARCHAR(16)   NOT NULL COMMENT '原因(字典 mes_stop_reason) BREAKDOWN故障 CHANGEOVER换型调机 MATERIAL待料 MAINTAIN计划保养 NO_ORDER无生产计划 OTHER其他',
    maintenance_id BIGINT                 DEFAULT NULL COMMENT '来源维护单（维护完成时自动记录）',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '说明',
    create_by      BIGINT                 DEFAULT NULL COMMENT '登记人',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_equipment_stop (equipment_id, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备停机记录';

-- 模具工装：寿命按模次计，报工时累计（模次 = 产出 / 模穴数），达到预警比例时提醒
CREATE TABLE mes_mold (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code         VARCHAR(32)   NOT NULL COMMENT '编码',
    name         VARCHAR(64)   NOT NULL COMMENT '名称',
    mold_type    VARCHAR(16)   NOT NULL COMMENT '类型(字典 mes_mold_type) MOLD模具 FIXTURE工装夹具 GAUGE检具',
    spec         VARCHAR(64)            DEFAULT NULL COMMENT '规格',
    cavities     INT           NOT NULL DEFAULT 1 COMMENT '模穴数（每模次产出数量）',
    design_life  INT                    DEFAULT NULL COMMENT '设计寿命（模次），为空不限',
    warn_ratio   DECIMAL(5, 2) NOT NULL DEFAULT 90 COMMENT '寿命预警比例（%），累计模次达到 寿命 × 比例 时提醒',
    used_count   INT           NOT NULL DEFAULT 0 COMMENT '累计模次',
    status       VARCHAR(16)   NOT NULL COMMENT '状态(字典 mes_mold_status) AVAILABLE在库 IN_USE使用中 REPAIR维修中 SCRAPPED报废',
    location     VARCHAR(64)            DEFAULT NULL COMMENT '存放位置',
    wo_id        BIGINT                 DEFAULT NULL COMMENT '领用的工单',
    equipment_id BIGINT                 DEFAULT NULL COMMENT '安装的设备',
    remark       VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    create_time  DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mold_code (code, tenant_id),
    KEY idx_mold_wo (wo_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模具工装';

CREATE TABLE mes_mold_product (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    mold_id     BIGINT NOT NULL COMMENT '模具',
    material_id BIGINT NOT NULL COMMENT '适用产品',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mold_product (mold_id, material_id, tenant_id),
    KEY idx_mold_product_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模具适用产品';

CREATE TABLE mes_mold_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    mold_id      BIGINT       NOT NULL COMMENT '模具',
    action       VARCHAR(16)  NOT NULL COMMENT '动作(字典 mes_mold_action) ISSUE领用 RETURN归还 USE使用 REPAIR送修 REPAIRED修复 SCRAP报废',
    log_date     DATE         NOT NULL COMMENT '日期',
    wo_id        BIGINT                DEFAULT NULL COMMENT '工单',
    equipment_id BIGINT                DEFAULT NULL COMMENT '设备',
    report_id    BIGINT                DEFAULT NULL COMMENT '报工记录（使用）',
    shots        INT                   DEFAULT NULL COMMENT '模次（使用）',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by    BIGINT                DEFAULT NULL COMMENT '经办人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_mold_log (mold_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模具履历';

-- ---------------------------------------------------------------- 财务管理

CREATE TABLE fin_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32)  NOT NULL COMMENT '科目编码：一级 4 位，下级在上级编码后加 2 位',
    name        VARCHAR(64)  NOT NULL COMMENT '科目名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级科目，0 为一级科目',
    category    VARCHAR(16)  NOT NULL COMMENT '类别(字典 fin_account_category) ASSET资产 LIABILITY负债 EQUITY权益 COST成本 PROFIT损益',
    direction   VARCHAR(8)   NOT NULL COMMENT '余额方向 DEBIT借 CREDIT贷',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    aux_items   VARCHAR(64)           DEFAULT NULL COMMENT '辅助核算项目（逗号分隔，可多选） DEPT部门 CUSTOMER客户 SUPPLIER供应商 PROJECT项目 MATERIAL存货；手工凭证使用该科目时必填',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_code (code, tenant_id),
    KEY idx_account_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会计科目';

CREATE TABLE fin_currency (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(8)  NOT NULL COMMENT '币种代码，如 USD',
    name        VARCHAR(32) NOT NULL COMMENT '名称',
    symbol      VARCHAR(8)           DEFAULT NULL COMMENT '符号',
    is_base     TINYINT     NOT NULL DEFAULT 0 COMMENT '本位币 1是（只有一个，CNY）',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_currency_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '币种';

CREATE TABLE fin_exchange_rate (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    currency    VARCHAR(8)     NOT NULL COMMENT '币种',
    period      INT            NOT NULL COMMENT '期间 yyyyMM：当月业务的记账汇率，期末调汇也取该月汇率',
    rate        DECIMAL(18, 6) NOT NULL COMMENT '汇率：1 外币 = rate 本位币',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exchange_rate (currency, period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '汇率（按月）';

CREATE TABLE fin_account_opening (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id  BIGINT         NOT NULL COMMENT '末级科目',
    debit       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初借方余额',
    credit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初贷方余额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_opening (account_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '科目期初余额';

CREATE TABLE fin_period (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period       INT         NOT NULL COMMENT '会计期间 yyyyMM，如 202610',
    status       VARCHAR(16) NOT NULL COMMENT '状态 OPEN未结账 CLOSED已结账',
    voucher_seq  INT         NOT NULL DEFAULT 0 COMMENT '本期已使用的最大凭证号',
    close_by     BIGINT               DEFAULT NULL COMMENT '结账人',
    close_time   DATETIME             DEFAULT NULL COMMENT '结账时间',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会计期间';

CREATE TABLE fin_voucher (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period           INT            NOT NULL COMMENT '会计期间 yyyyMM',
    voucher_seq      INT            NOT NULL COMMENT '期间内凭证号',
    voucher_no       VARCHAR(16)    NOT NULL COMMENT '凭证字号，如 记-0001',
    voucher_date     DATE           NOT NULL COMMENT '凭证日期',
    summary          VARCHAR(255)            DEFAULT NULL COMMENT '摘要（第一条分录）',
    total_debit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方合计',
    total_credit     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方合计',
    attachment_count INT            NOT NULL DEFAULT 0 COMMENT '附单据数',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_voucher_status) DRAFT未审核 APPROVING审批中 AUDITED已审核 POSTED已记账',
    source_type      VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源(字典 fin_voucher_source) MANUAL手工 AR应收 AP应付 RECEIPT收款 PAYMENT付款 DEPRECIATION折旧 ASSET资产处置 COST成本结转 CLOSE结转损益',
    source_no        VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    flow_instance_id VARCHAR(64)            DEFAULT NULL COMMENT '审批流程实例ID',
    reversal_of      BIGINT                  DEFAULT NULL COMMENT '红字冲销凭证：被冲销的原凭证',
    reversed         TINYINT        NOT NULL DEFAULT 0 COMMENT '已被红字冲销 1是',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    audit_by         BIGINT                  DEFAULT NULL COMMENT '审核人',
    audit_time       DATETIME                DEFAULT NULL COMMENT '审核时间',
    post_by          BIGINT                  DEFAULT NULL COMMENT '记账人',
    post_time        DATETIME                DEFAULT NULL COMMENT '记账时间',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_voucher_seq (period, voucher_seq, tenant_id),
    KEY idx_voucher_period_status (period, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '记账凭证';

CREATE TABLE fin_voucher_entry (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    voucher_id  BIGINT         NOT NULL COMMENT '凭证ID',
    seq         INT            NOT NULL COMMENT '分录行号',
    summary     VARCHAR(255)   NOT NULL COMMENT '摘要',
    account_id  BIGINT         NOT NULL COMMENT '科目（末级）',
    dept_id     BIGINT                  DEFAULT NULL COMMENT '部门（辅助核算，预算执行按部门统计）',
    project_id  BIGINT                  DEFAULT NULL COMMENT '项目（辅助核算）',
    customer_id BIGINT                  DEFAULT NULL COMMENT '客户（辅助核算）',
    supplier_id BIGINT                  DEFAULT NULL COMMENT '供应商（辅助核算）',
    material_id BIGINT                  DEFAULT NULL COMMENT '存货（辅助核算）',
    debit       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方金额',
    credit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_entry_voucher (voucher_id),
    KEY idx_entry_account (account_id),
    KEY idx_entry_project (project_id),
    KEY idx_entry_customer (customer_id),
    KEY idx_entry_supplier (supplier_id),
    KEY idx_entry_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证分录';

CREATE TABLE fin_bill (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    bill_no        VARCHAR(32)    NOT NULL COMMENT '单号',
    kind           VARCHAR(8)     NOT NULL COMMENT '种类 AR应收 AP应付',
    partner_id     BIGINT         NOT NULL COMMENT '客户 / 供应商',
    source_type    VARCHAR(16)    NOT NULL COMMENT '来源(字典 fin_bill_source) SALES_OUT销售出库 SALES_RETURN销售退货 PURCHASE_IN采购入库 PURCHASE_RETURN采购退货 MANUAL期初 / 手工',
    source_id      BIGINT                  DEFAULT NULL COMMENT '来源单据ID',
    source_no      VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    project_id     BIGINT                  DEFAULT NULL COMMENT '项目（辅助核算），项目开票时填写',
    expense_account_id BIGINT                DEFAULT NULL COMMENT '费用类应付单（运费等）的借方费用科目',
    dept_id        BIGINT                  DEFAULT NULL COMMENT '部门（费用的辅助核算）',
    bill_date      DATE           NOT NULL COMMENT '单据日期',
    due_date       DATE           NOT NULL COMMENT '到期日 = 单据日期 + 账期',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '价税合计，退货为负数（红字）',
    tax_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    settled_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已核销金额（与金额同号）',
    invoiced_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已开票 / 已收票金额（与金额同号）',
    bad_debt_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '坏账核销金额（计入已核销）',
    currency        VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种；金额、已核销等为本位币（按记账汇率折算）',
    exchange_rate   DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '记账汇率',
    amount_fc       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    settled_fc      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币已核销',
    status         VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_bill_status) UNSETTLED未核销 PARTIAL部分核销 SETTLED已核销',
    voucher_id     BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_no (bill_no, tenant_id),
    KEY idx_bill_partner (kind, partner_id, status),
    KEY idx_bill_source (source_type, source_id),
    KEY idx_bill_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应收 / 应付单';

CREATE TABLE fin_settlement (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settle_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind        VARCHAR(16)    NOT NULL COMMENT '种类 RECEIPT收款 PAYMENT付款',
    partner_id  BIGINT         NOT NULL COMMENT '客户 / 供应商',
    settle_date DATE           NOT NULL COMMENT '收付款日期',
    account_id  BIGINT         NOT NULL COMMENT '结算科目（库存现金 / 银行存款的末级科目）',
    method      VARCHAR(16)             DEFAULT NULL COMMENT '结算方式(字典 fin_settle_method)',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额 = 各核销金额合计',
    currency    VARCHAR(8)     NOT NULL DEFAULT 'CNY' COMMENT '币种',
    exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '收付款汇率；金额 = 原币 × 汇率（实收实付本位币）',
    amount_fc   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '原币金额',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    status       VARCHAR(16)    NOT NULL DEFAULT 'APPROVED' COMMENT '状态 APPROVED已生效 APPROVING审批中 REJECTED已拒绝（付款审批）',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '审批流程实例ID',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_settle_no (settle_no, tenant_id),
    KEY idx_settlement_partner (kind, partner_id),
    KEY idx_settlement_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收款 / 付款单';

CREATE TABLE fin_settlement_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    settlement_id BIGINT         NOT NULL COMMENT '收付款单ID',
    bill_id       BIGINT         NOT NULL COMMENT '核销的应收 / 应付单',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '核销金额（红字单据为负数）',
    amount_fc     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '核销原币金额（本位币单据与金额相同）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_settlement_item_settlement (settlement_id),
    KEY idx_settlement_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收付款核销明细';

CREATE TABLE fin_invoice (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind           VARCHAR(8)     NOT NULL COMMENT '种类 OUTPUT销项（开给客户） INPUT进项（供应商开来）',
    invoice_no     VARCHAR(32)    NOT NULL COMMENT '发票号码',
    invoice_type   VARCHAR(16)    NOT NULL DEFAULT 'SPECIAL' COMMENT '发票类型(字典 fin_invoice_type) SPECIAL增值税专用发票 NORMAL普通发票 E_SPECIAL数电票（专票） E_NORMAL数电票（普票）',
    partner_id     BIGINT         NOT NULL COMMENT '客户 / 供应商',
    invoice_date   DATE           NOT NULL COMMENT '开票日期',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '价税合计（红字发票为负数）',
    tax_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    matched_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已勾稽应收应付单的金额，导入的发票可以之后再勾稽',
    seller_tax_no  VARCHAR(32)             DEFAULT NULL COMMENT '销售方纳税人识别号',
    seller_name    VARCHAR(128)            DEFAULT NULL COMMENT '销售方名称',
    buyer_tax_no   VARCHAR(32)             DEFAULT NULL COMMENT '购买方纳税人识别号',
    buyer_name     VARCHAR(128)            DEFAULT NULL COMMENT '购买方名称',
    source         VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工登记 XML数电票文件导入 EXCEL发票清单导入',
    status         VARCHAR(16)    NOT NULL DEFAULT 'VALID' COMMENT '状态 VALID有效 VOID已作废',
    certify_period INT                     DEFAULT NULL COMMENT '进项发票认证（勾选抵扣）期间 yyyyMM',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (kind, invoice_no, tenant_id),
    KEY idx_invoice_partner (kind, partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票登记：与应收应付单勾稽，未开票 / 未收票的部分为暂估';

CREATE TABLE fin_invoice_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    bill_id     BIGINT         NOT NULL COMMENT '应收 / 应付单',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '勾稽金额（价税合计）',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_item_invoice (invoice_id),
    KEY idx_invoice_item_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票勾稽的应收应付单';

CREATE TABLE fin_invoice_line (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    invoice_id  BIGINT         NOT NULL COMMENT '发票',
    seq         INT            NOT NULL COMMENT '行号',
    item_name   VARCHAR(200)   NOT NULL COMMENT '项目名称（含税收分类简称）',
    spec        VARCHAR(128)            DEFAULT NULL COMMENT '规格型号',
    unit        VARCHAR(32)             DEFAULT NULL COMMENT '单位',
    qty         DECIMAL(18, 6)          DEFAULT NULL COMMENT '数量',
    price       DECIMAL(18, 8)          DEFAULT NULL COMMENT '单价（不含税）',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '金额（不含税）',
    tax_rate    DECIMAL(6, 4)           DEFAULT NULL COMMENT '税率，如 0.13',
    tax_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '税额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_invoice_line_invoice (invoice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票明细行：导入数电票时保存';

CREATE TABLE fin_advance (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    advance_no   VARCHAR(32)    NOT NULL COMMENT '单号',
    kind         VARCHAR(16)    NOT NULL COMMENT '种类 RECEIPT预收（客户） PAYMENT预付（供应商）',
    partner_id   BIGINT         NOT NULL COMMENT '客户 / 供应商',
    advance_date DATE           NOT NULL COMMENT '收付款日期',
    account_id   BIGINT         NOT NULL COMMENT '结算科目（库存现金 / 银行存款）',
    method       VARCHAR(16)             DEFAULT NULL COMMENT '结算方式(字典 fin_settle_method)',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '金额',
    used_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已冲抵应收应付的金额',
    voucher_id   BIGINT                  DEFAULT NULL COMMENT '收付款凭证',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_advance_no (advance_no, tenant_id),
    KEY idx_advance_partner (kind, partner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预收 / 预付款：收付时记预收账款 / 预付账款，冲抵应收应付单时转出';

CREATE TABLE fin_advance_apply (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    advance_id  BIGINT         NOT NULL COMMENT '预收 / 预付款',
    bill_id     BIGINT         NOT NULL COMMENT '冲抵的应收 / 应付单',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '冲抵金额',
    apply_date  DATE           NOT NULL COMMENT '冲抵日期',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '转账凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_advance_apply_advance (advance_id),
    KEY idx_advance_apply_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预收预付冲抵应收应付';

-- ---------------------------------------------------------------- 质量管理（代码在生产制造模块，表以 qms_ 开头）
-- 检验方案：按物料（优先）或物料分类定义检验项目与抽样方案，新建检验单时自动带出
CREATE TABLE qms_inspection_plan (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '方案名称',
    insp_type         VARCHAR(16)    NOT NULL COMMENT '检验类型(字典 mes_qc_type) IQC来料 IPQC过程 FQC成品 FAI首件',
    material_id       BIGINT                  DEFAULT NULL COMMENT '适用物料（与物料分类二选一，物料优先）',
    material_category VARCHAR(32)             DEFAULT NULL COMMENT '适用物料分类(字典 wms_material_category)',
    sampling_mode     VARCHAR(16)    NOT NULL COMMENT '抽样方式 FULL全检 FIXED固定数量 AQL按GB/T 2828.1',
    fixed_qty         DECIMAL(18, 4)          DEFAULT NULL COMMENT '固定抽样数量',
    accept_qty        INT                     DEFAULT NULL COMMENT '固定抽样的允收不良数',
    aql               DECIMAL(6, 3)           DEFAULT NULL COMMENT '接收质量限 AQL，如 0.65',
    inspection_level  VARCHAR(8)              DEFAULT NULL COMMENT '检验水平 S1~S4、I、II、III',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_qms_plan_material (insp_type, material_id),
    KEY idx_qms_plan_category (insp_type, material_category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检验方案';

CREATE TABLE qms_inspection_plan_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_id     BIGINT         NOT NULL COMMENT '检验方案',
    seq         INT            NOT NULL DEFAULT 0 COMMENT '顺序',
    name        VARCHAR(64)    NOT NULL COMMENT '检验项目',
    method      VARCHAR(16)    NOT NULL COMMENT '判定方式 QUALITATIVE定性（合格 / 不合格） QUANTITATIVE定量（实测值与上下限）',
    standard    VARCHAR(255)            DEFAULT NULL COMMENT '检验标准 / 方法说明',
    target      DECIMAL(18, 6)          DEFAULT NULL COMMENT '目标值（定量）',
    lower_limit DECIMAL(18, 6)          DEFAULT NULL COMMENT '下限（定量）',
    upper_limit DECIMAL(18, 6)          DEFAULT NULL COMMENT '上限（定量）',
    unit        VARCHAR(16)             DEFAULT NULL COMMENT '单位',
    critical    TINYINT        NOT NULL DEFAULT 0 COMMENT '关键项目：不合格即判整批不合格',
    PRIMARY KEY (id),
    KEY idx_qms_plan_item_plan (plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检验方案项目';

-- 检验单的逐项检验结果（按方案带出，定量项目记录实测值，用于 SPC）
CREATE TABLE qms_inspection_item (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    inspection_id  BIGINT         NOT NULL COMMENT '检验单',
    plan_item_id   BIGINT                  DEFAULT NULL COMMENT '方案项目',
    seq            INT            NOT NULL DEFAULT 0 COMMENT '顺序',
    name           VARCHAR(64)    NOT NULL COMMENT '检验项目',
    method         VARCHAR(16)    NOT NULL COMMENT '判定方式 QUALITATIVE / QUANTITATIVE',
    standard       VARCHAR(255)            DEFAULT NULL COMMENT '检验标准',
    lower_limit    DECIMAL(18, 6)          DEFAULT NULL COMMENT '下限',
    upper_limit    DECIMAL(18, 6)          DEFAULT NULL COMMENT '上限',
    unit           VARCHAR(16)             DEFAULT NULL COMMENT '单位',
    critical       TINYINT        NOT NULL DEFAULT 0 COMMENT '关键项目',
    measured       VARCHAR(2000)           DEFAULT NULL COMMENT '定量项目的实测值，逗号分隔',
    defect_count   INT            NOT NULL DEFAULT 0 COMMENT '不合格样本数（定量项目为超出上下限的个数）',
    result         VARCHAR(16)             DEFAULT NULL COMMENT '项目结论 PASS / FAIL',
    PRIMARY KEY (id),
    KEY idx_qms_insp_item_inspection (inspection_id),
    KEY idx_qms_insp_item_plan_item (plan_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检验项目结果';

-- 不合格品处理单（MRB）：检验不合格自动生成，也可手工登记（生产、客诉）；评审处置后联动库存与工单
CREATE TABLE qms_ncr (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    ncr_no           VARCHAR(32)    NOT NULL COMMENT '处理单号',
    source           VARCHAR(16)    NOT NULL COMMENT '来源(字典 qms_ncr_source) INSPECTION检验 PRODUCTION生产 CUSTOMER客诉 OTHER其他',
    inspection_id    BIGINT                  DEFAULT NULL COMMENT '来源检验单',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    batch_no         VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    qty              DECIMAL(18, 4) NOT NULL COMMENT '不合格数量',
    supplier_id      BIGINT                  DEFAULT NULL COMMENT '供应商（来料）',
    wo_id            BIGINT                  DEFAULT NULL COMMENT '生产工单（过程、成品）',
    source_no        VARCHAR(32)             DEFAULT NULL COMMENT '来源单号（采购入库单、工单）',
    defect_type      VARCHAR(32)             DEFAULT NULL COMMENT '不良类型(字典 mes_defect_type)',
    description      VARCHAR(500)            DEFAULT NULL COMMENT '不合格描述',
    disposition      VARCHAR(16)             DEFAULT NULL COMMENT '处置(字典 qms_disposition) RETURN退货 REWORK返工 SCRAP报废 CONCESSION让步接收 SORT挑选',
    responsibility   VARCHAR(16)             DEFAULT NULL COMMENT '责任方(字典 qms_responsibility) SUPPLIER供应商 PRODUCTION生产 DESIGN设计 OTHER其他',
    review_opinion   VARCHAR(500)            DEFAULT NULL COMMENT '评审意见',
    status           VARCHAR(16)    NOT NULL DEFAULT 'OPEN' COMMENT '状态(字典 qms_ncr_status) OPEN待评审 APPROVING审批中 DONE已处置 CANCELLED已取消',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    result_note      VARCHAR(255)            DEFAULT NULL COMMENT '处置执行结果（如返工工单号、报废出库单号）',
    rework_wo_id     BIGINT                  DEFAULT NULL COMMENT '返工工单',
    done_time        DATETIME                DEFAULT NULL COMMENT '处置完成时间',
    create_by        BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ncr_no (ncr_no, tenant_id),
    KEY idx_ncr_status (status),
    KEY idx_ncr_inspection (inspection_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '不合格品处理单';

-- 纠正预防措施（8D）：由不合格、客诉、内部发现、审核发起
CREATE TABLE qms_capa (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    capa_no       VARCHAR(32)   NOT NULL COMMENT '单号',
    source        VARCHAR(16)   NOT NULL COMMENT '来源(字典 qms_capa_source) NCR不合格 COMPLAINT客诉 INTERNAL内部发现 AUDIT审核',
    ncr_id        BIGINT                 DEFAULT NULL COMMENT '来源不合格品处理单',
    title         VARCHAR(128)  NOT NULL COMMENT '问题标题',
    description   VARCHAR(1000)          DEFAULT NULL COMMENT '问题描述',
    owner_id      BIGINT        NOT NULL COMMENT '负责人',
    due_date      DATE          NOT NULL COMMENT '要求完成日期',
    status        VARCHAR(16)   NOT NULL DEFAULT 'OPEN' COMMENT '状态(字典 qms_capa_status) OPEN处理中 VERIFYING待验证 CLOSED已关闭',
    verify_result VARCHAR(500)           DEFAULT NULL COMMENT '效果验证结论',
    verified_by   BIGINT                 DEFAULT NULL COMMENT '验证人',
    verify_time   DATETIME               DEFAULT NULL COMMENT '验证时间',
    create_by     BIGINT                 DEFAULT NULL COMMENT '发起人',
    create_time   DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_capa_no (capa_no, tenant_id),
    KEY idx_capa_status (status, due_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '纠正预防措施';

CREATE TABLE qms_capa_step (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    capa_id     BIGINT        NOT NULL COMMENT '纠正预防单',
    step        VARCHAR(4)    NOT NULL COMMENT '步骤 D1~D8',
    content     VARCHAR(2000)          DEFAULT NULL COMMENT '内容',
    owner_id    BIGINT                 DEFAULT NULL COMMENT '责任人',
    done        TINYINT       NOT NULL DEFAULT 0 COMMENT '已完成',
    done_date   DATE                   DEFAULT NULL COMMENT '完成日期',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_capa_step (capa_id, step, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '8D 步骤';

-- 供应商质量索赔：确认后生成红字应付单冲减应付（财务模块监听事件生成）
CREATE TABLE qms_supplier_claim (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    claim_no     VARCHAR(32)    NOT NULL COMMENT '索赔单号',
    supplier_id  BIGINT         NOT NULL COMMENT '供应商',
    ncr_id       BIGINT                  DEFAULT NULL COMMENT '来源不合格品处理单',
    claim_date   DATE           NOT NULL COMMENT '索赔日期',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '索赔金额',
    reason       VARCHAR(500)   NOT NULL COMMENT '索赔原因',
    status       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 qms_claim_status) DRAFT待确认 CONFIRMED已确认 CANCELLED已取消',
    bill_id      BIGINT                  DEFAULT NULL COMMENT '冲减应付的红字应付单',
    confirm_time DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_by    BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qms_claim_no (claim_no, tenant_id),
    KEY idx_claim_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商质量索赔';

-- 采购合同 / 框架协议：审批生效后采购订单可引用，按合同价下单并校验数量、金额上限
CREATE TABLE scm_purchase_contract (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_no      VARCHAR(32)    NOT NULL COMMENT '合同编号',
    name             VARCHAR(128)   NOT NULL COMMENT '合同名称',
    supplier_id      BIGINT         NOT NULL COMMENT '供应商',
    contract_type    VARCHAR(16)    NOT NULL COMMENT '类型(字典 scm_contract_type) FRAMEWORK框架协议 FIXED固定数量合同',
    sign_date        DATE           NOT NULL COMMENT '签订日期',
    start_date       DATE           NOT NULL COMMENT '生效日期',
    end_date         DATE           NOT NULL COMMENT '到期日期',
    amount_limit     DECIMAL(18, 2)          DEFAULT NULL COMMENT '金额上限（价税合计），为空不限',
    payment_days     INT                     DEFAULT NULL COMMENT '付款天数',
    payment_terms    VARCHAR(255)            DEFAULT NULL COMMENT '付款条款',
    status           VARCHAR(16)    NOT NULL COMMENT '状态(字典 scm_contract_status) DRAFT草稿 APPROVING审批中 ACTIVE生效 CLOSED已关闭',
    tender_id        BIGINT                  DEFAULT NULL COMMENT '来源招标',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    approve_time     DATETIME                DEFAULT NULL COMMENT '生效时间',
    close_reason     VARCHAR(255)            DEFAULT NULL COMMENT '关闭原因',
    remark           VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '部门',
    create_by        BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_contract_no (contract_no, tenant_id),
    KEY idx_purchase_contract_supplier (supplier_id),
    KEY idx_purchase_contract_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购合同';

CREATE TABLE scm_purchase_contract_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_id BIGINT         NOT NULL COMMENT '采购合同',
    material_id BIGINT         NOT NULL COMMENT '物料',
    price       DECIMAL(18, 4) NOT NULL COMMENT '合同含税单价',
    tax_rate    DECIMAL(5, 2)  NOT NULL COMMENT '税率（%）',
    qty_limit   DECIMAL(18, 4)          DEFAULT NULL COMMENT '数量上限，为空不限',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_purchase_contract_item (contract_id),
    KEY idx_purchase_contract_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购合同明细';

-- 招标：邀请供应商在门户投标（也可由采购代录），截止后开标比价，定标后生成采购合同或采购订单
CREATE TABLE scm_tender (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    tender_no         VARCHAR(32)  NOT NULL COMMENT '招标编号',
    title             VARCHAR(128) NOT NULL COMMENT '招标标题',
    deadline          DATETIME     NOT NULL COMMENT '投标截止时间',
    status            VARCHAR(16)  NOT NULL COMMENT '状态(字典 scm_tender_status) DRAFT草稿 BIDDING投标中 OPENED已开标 AWARDED已定标 CANCELLED已取消',
    open_time         DATETIME              DEFAULT NULL COMMENT '开标时间',
    award_supplier_id BIGINT                DEFAULT NULL COMMENT '中标供应商',
    award_reason      VARCHAR(255)          DEFAULT NULL COMMENT '定标说明',
    contract_id       BIGINT                DEFAULT NULL COMMENT '生成的采购合同',
    order_id          BIGINT                DEFAULT NULL COMMENT '生成的采购订单',
    remark            VARCHAR(500)          DEFAULT NULL COMMENT '招标说明',
    dept_id           BIGINT                DEFAULT NULL COMMENT '部门',
    create_by         BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tender_no (tender_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '招标';

CREATE TABLE scm_tender_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    tender_id     BIGINT         NOT NULL COMMENT '招标',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '数量',
    required_date DATE                    DEFAULT NULL COMMENT '需求日期',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '要求',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tender_item (tender_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '招标明细';

CREATE TABLE scm_tender_supplier (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT   NOT NULL DEFAULT 1 COMMENT '租户ID',
    tender_id   BIGINT   NOT NULL COMMENT '招标',
    supplier_id BIGINT   NOT NULL COMMENT '邀请的供应商',
    create_time DATETIME          DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tender_supplier (tender_id, supplier_id, tenant_id),
    KEY idx_tender_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '招标邀请的供应商';

CREATE TABLE scm_bid (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    tender_id     BIGINT         NOT NULL COMMENT '招标',
    supplier_id   BIGINT         NOT NULL COMMENT '投标供应商',
    total_amount  DECIMAL(18, 2) NOT NULL COMMENT '投标总价（价税合计）',
    delivery_days INT                     DEFAULT NULL COMMENT '交货期（天）',
    remark        VARCHAR(500)            DEFAULT NULL COMMENT '投标说明',
    submit_by     BIGINT                  DEFAULT NULL COMMENT '提交人（供应商门户账号或代录的采购员）',
    submit_time   DATETIME                DEFAULT NULL COMMENT '提交时间',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bid (tender_id, supplier_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '投标';

CREATE TABLE scm_bid_item (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    bid_id         BIGINT         NOT NULL COMMENT '投标',
    tender_item_id BIGINT         NOT NULL COMMENT '招标明细',
    price          DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate       DECIMAL(5, 2)  NOT NULL COMMENT '税率（%）',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bid_item (bid_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '投标明细';

-- 送货通知（ASN）：供应商在门户按采购订单发起，仓库按送货通知收货（生成采购入库）
CREATE TABLE scm_asn (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    asn_no        VARCHAR(32) NOT NULL COMMENT '送货单号',
    supplier_id   BIGINT      NOT NULL COMMENT '供应商',
    order_id      BIGINT      NOT NULL COMMENT '采购订单',
    ship_date     DATE        NOT NULL COMMENT '发货日期',
    expected_date DATE                 DEFAULT NULL COMMENT '预计到货日期',
    carrier       VARCHAR(64)          DEFAULT NULL COMMENT '承运商',
    tracking_no   VARCHAR(64)          DEFAULT NULL COMMENT '运单号',
    status        VARCHAR(16) NOT NULL COMMENT '状态(字典 scm_asn_status) SUBMITTED待收货 RECEIVED已收货 CANCELLED已取消',
    trade_doc_id  BIGINT               DEFAULT NULL COMMENT '收货生成的采购入库单',
    received_time DATETIME             DEFAULT NULL COMMENT '收货时间',
    remark        VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    create_by     BIGINT               DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asn_no (asn_no, tenant_id),
    KEY idx_asn_supplier (supplier_id),
    KEY idx_asn_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '送货通知';

CREATE TABLE scm_asn_item (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    asn_id        BIGINT         NOT NULL COMMENT '送货通知',
    order_item_id BIGINT         NOT NULL COMMENT '采购订单明细',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    qty           DECIMAL(18, 4) NOT NULL COMMENT '送货数量',
    batch_no      VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_asn_item (asn_id),
    KEY idx_asn_item_order (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '送货通知明细';

-- 供应商对账单：按期间汇总供应商的应付单，发送后供应商在门户确认或提出异议，并填写发票信息
CREATE TABLE fin_supplier_statement (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    statement_no    VARCHAR(32)    NOT NULL COMMENT '对账单号',
    supplier_id     BIGINT         NOT NULL COMMENT '供应商',
    begin_date      DATE           NOT NULL COMMENT '对账期间起',
    end_date        DATE           NOT NULL COMMENT '对账期间止',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '对账金额（应付单价税合计）',
    bill_count      INT            NOT NULL DEFAULT 0 COMMENT '应付单数',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_statement_status) DRAFT草稿 SENT待确认 CONFIRMED已确认 DISPUTED有异议',
    supplier_remark VARCHAR(500)            DEFAULT NULL COMMENT '供应商意见',
    confirm_time    DATETIME                DEFAULT NULL COMMENT '供应商确认时间',
    invoice_no      VARCHAR(64)             DEFAULT NULL COMMENT '发票号码',
    invoice_amount  DECIMAL(18, 2)          DEFAULT NULL COMMENT '发票金额',
    invoice_date    DATE                    DEFAULT NULL COMMENT '开票日期',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_statement_no (statement_no, tenant_id),
    KEY idx_statement_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商对账单';

CREATE TABLE fin_supplier_statement_line (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    statement_id BIGINT         NOT NULL COMMENT '对账单',
    bill_id      BIGINT         NOT NULL COMMENT '应付单',
    bill_no      VARCHAR(32)    NOT NULL COMMENT '应付单号',
    source_type  VARCHAR(16)    NOT NULL COMMENT '来源(字典 fin_bill_source)',
    source_no    VARCHAR(32)             DEFAULT NULL COMMENT '来源单号',
    bill_date    DATE           NOT NULL COMMENT '单据日期',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '价税合计',
    tax_amount   DECIMAL(18, 2) NOT NULL COMMENT '税额',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_statement_line (statement_id),
    KEY idx_statement_line_bill (bill_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商对账单明细';

-- 工程变更（ECN）：复制基准 BOM 为新版本修改，审批后按生效日期切换默认 BOM
CREATE TABLE mes_ecn (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    ecn_no           VARCHAR(32)  NOT NULL COMMENT '变更单号',
    title            VARCHAR(128) NOT NULL COMMENT '变更标题',
    material_id      BIGINT       NOT NULL COMMENT '产品',
    from_bom_id      BIGINT       NOT NULL COMMENT '变更前 BOM',
    to_bom_id        BIGINT                DEFAULT NULL COMMENT '变更后 BOM（新版本，生效前停用）',
    reason           VARCHAR(16)  NOT NULL COMMENT '变更原因(字典 mes_ecn_reason)',
    description      VARCHAR(1000)         DEFAULT NULL COMMENT '变更说明',
    effective_date   DATE         NOT NULL COMMENT '生效日期',
    sync_draft_wo    TINYINT      NOT NULL DEFAULT 1 COMMENT '生效时同步草稿工单的用料和工序',
    status           VARCHAR(16)  NOT NULL COMMENT '状态(字典 mes_ecn_status) DRAFT草稿 APPROVING审批中 APPROVED待生效 EFFECTIVE已生效 CANCELLED已取消',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例ID',
    approve_time     DATETIME              DEFAULT NULL COMMENT '批准时间',
    effect_time      DATETIME              DEFAULT NULL COMMENT '生效时间',
    result_note      VARCHAR(255)          DEFAULT NULL COMMENT '生效结果',
    dept_id          BIGINT                DEFAULT NULL COMMENT '部门',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecn_no (ecn_no, tenant_id),
    KEY idx_ecn_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工程变更';

-- 技术文档：图纸、规格书、作业指导书等，按版本发布，发布新版本时旧版本作废；文件在附件中
CREATE TABLE mes_tech_doc (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    doc_no       VARCHAR(32)  NOT NULL COMMENT '文档编号（各版本相同）',
    name         VARCHAR(128) NOT NULL COMMENT '文档名称',
    doc_type     VARCHAR(16)  NOT NULL COMMENT '类型(字典 mes_doc_type)',
    material_id  BIGINT                DEFAULT NULL COMMENT '关联物料',
    version      VARCHAR(16)  NOT NULL COMMENT '版本',
    status       VARCHAR(16)  NOT NULL COMMENT '状态(字典 mes_doc_status) DRAFT草稿 RELEASED已发布 OBSOLETE已作废',
    prev_id      BIGINT                DEFAULT NULL COMMENT '上一版本',
    ecn_id       BIGINT                DEFAULT NULL COMMENT '关联的工程变更',
    description  VARCHAR(500)          DEFAULT NULL COMMENT '说明 / 变更内容',
    release_by   BIGINT                DEFAULT NULL COMMENT '发布人',
    release_time DATETIME              DEFAULT NULL COMMENT '发布时间',
    create_by    BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tech_doc_version (doc_no, version, tenant_id),
    KEY idx_tech_doc_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '技术文档';

-- 资产变动履历：领用、归还、调拨（部门 / 使用人 / 存放地点），资料修改中改了使用信息的也记录
CREATE TABLE fin_asset_move (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    asset_id      BIGINT       NOT NULL COMMENT '资产',
    move_type     VARCHAR(16)  NOT NULL COMMENT '类型(字典 fin_asset_move_type) ASSIGN领用 RETURN归还 TRANSFER调拨',
    move_date     DATE         NOT NULL COMMENT '日期',
    from_dept_id  BIGINT                DEFAULT NULL COMMENT '原部门',
    to_dept_id    BIGINT                DEFAULT NULL COMMENT '新部门',
    from_user_id  BIGINT                DEFAULT NULL COMMENT '原使用人',
    to_user_id    BIGINT                DEFAULT NULL COMMENT '新使用人',
    from_location VARCHAR(128)          DEFAULT NULL COMMENT '原存放地点',
    to_location   VARCHAR(128)          DEFAULT NULL COMMENT '新存放地点',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by     BIGINT                DEFAULT NULL COMMENT '经办人',
    create_time   DATETIME              DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_asset_move (asset_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资产变动';

-- 资产盘点：按部门 / 地点生成盘点清单，逐项确认，完成时盘亏的处置、位置变化的更新卡片
CREATE TABLE fin_asset_check (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_no    VARCHAR(32)  NOT NULL COMMENT '盘点单号',
    check_date  DATE         NOT NULL COMMENT '盘点日期',
    dept_id     BIGINT                DEFAULT NULL COMMENT '盘点范围：部门，为空全部',
    location    VARCHAR(128)          DEFAULT NULL COMMENT '盘点范围：存放地点（包含），为空全部',
    status      VARCHAR(16)  NOT NULL COMMENT '状态(字典 fin_asset_check_status) CHECKING盘点中 DONE已完成',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                DEFAULT NULL COMMENT '创建人',
    finish_time DATETIME              DEFAULT NULL COMMENT '完成时间',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_check_no (check_no, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资产盘点';

CREATE TABLE fin_asset_check_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    check_id        BIGINT       NOT NULL COMMENT '盘点单',
    asset_id        BIGINT                DEFAULT NULL COMMENT '资产，盘盈的为空',
    book_dept_id    BIGINT                DEFAULT NULL COMMENT '账面部门',
    book_user_id    BIGINT                DEFAULT NULL COMMENT '账面使用人',
    book_location   VARCHAR(128)          DEFAULT NULL COMMENT '账面存放地点',
    result          VARCHAR(16)  NOT NULL COMMENT '结果(字典 fin_asset_check_result) PENDING待盘 NORMAL相符 MOVED位置变化 LOSS盘亏 GAIN盘盈',
    actual_dept_id  BIGINT                DEFAULT NULL COMMENT '实际部门（位置变化）',
    actual_user_id  BIGINT                DEFAULT NULL COMMENT '实际使用人（位置变化）',
    actual_location VARCHAR(128)          DEFAULT NULL COMMENT '实际存放地点（位置变化）',
    gain_name       VARCHAR(64)           DEFAULT NULL COMMENT '盘盈资产名称',
    remark          VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    check_by        BIGINT                DEFAULT NULL COMMENT '盘点人',
    check_time      DATETIME              DEFAULT NULL COMMENT '盘点时间',
    PRIMARY KEY (id),
    KEY idx_asset_check_item (check_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资产盘点明细';

-- 低值易耗品领用：部门从仓库领用（生成部门领用出库单），月末成本核算按实际单价一次摊销计入部门费用
CREATE TABLE fin_consumable_use (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    use_no             VARCHAR(32)  NOT NULL COMMENT '领用单号',
    dept_id            BIGINT       NOT NULL COMMENT '领用部门（费用归属）',
    user_id            BIGINT                DEFAULT NULL COMMENT '领用人',
    warehouse_id       BIGINT       NOT NULL COMMENT '仓库',
    use_date           DATE         NOT NULL COMMENT '领用日期',
    expense_account_id BIGINT       NOT NULL COMMENT '费用科目（管理费用、制造费用、销售费用等）',
    status             VARCHAR(16)  NOT NULL COMMENT '状态(字典 fin_consumable_status) DRAFT草稿 CONFIRMED已出库',
    wms_doc_id         BIGINT                DEFAULT NULL COMMENT '部门领用出库单',
    remark             VARCHAR(255)          DEFAULT NULL COMMENT '用途',
    create_by          BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_consumable_use_no (use_no, tenant_id),
    KEY idx_consumable_use_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '低值易耗品领用';

CREATE TABLE fin_consumable_use_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    use_id      BIGINT         NOT NULL COMMENT '领用单',
    material_id BIGINT         NOT NULL COMMENT '物料（低值易耗品）',
    location_id BIGINT                  DEFAULT NULL COMMENT '库位',
    batch_no    VARCHAR(64)             DEFAULT NULL COMMENT '批次号',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '数量',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_consumable_use_item (use_id),
    KEY idx_consumable_use_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '低值易耗品领用明细';




-- 票据：应收票据（收到的承兑汇票）与应付票据（开出的承兑汇票）。收票、背书、开票通过收付款单核销往来，
-- 贴现、托收、拒付、兑付直接生成凭证；每次操作记录在 fin_note_log
CREATE TABLE fin_note (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    kind          VARCHAR(16)    NOT NULL COMMENT '种类 RECEIVABLE应收票据 PAYABLE应付票据',
    note_type     VARCHAR(16)    NOT NULL COMMENT '票据类型(字典 fin_note_type) BANK银行承兑 COMMERCIAL商业承兑',
    note_no       VARCHAR(64)    NOT NULL COMMENT '票据号码',
    electronic    TINYINT        NOT NULL DEFAULT 1 COMMENT '电子票据 1是 0纸质',
    partner_id    BIGINT         NOT NULL COMMENT '收票时的客户 / 开票时的供应商',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '票面金额',
    issue_date    DATE           NOT NULL COMMENT '出票日期',
    due_date      DATE           NOT NULL COMMENT '到期日期',
    drawer        VARCHAR(128)            DEFAULT NULL COMMENT '出票人',
    acceptor      VARCHAR(128)            DEFAULT NULL COMMENT '承兑人（承兑银行或企业）',
    status        VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_note_status)',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by     BIGINT                  DEFAULT NULL COMMENT '登记人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_no (kind, note_no, tenant_id),
    KEY idx_note_due (status, due_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '票据';

CREATE TABLE fin_note_log (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    note_id       BIGINT         NOT NULL COMMENT '票据',
    action        VARCHAR(16)    NOT NULL COMMENT '操作(字典 fin_note_action) RECEIVE收票 ENDORSE背书 DISCOUNT贴现 COLLECT托收 DISHONOR拒付 ISSUE开票 PAY兑付',
    action_date   DATE           NOT NULL COMMENT '操作日期',
    partner_id    BIGINT                  DEFAULT NULL COMMENT '往来单位（收票客户、背书或开票的供应商）',
    account_id    BIGINT                  DEFAULT NULL COMMENT '银行科目（贴现、托收、兑付）',
    amount        DECIMAL(18, 2) NOT NULL COMMENT '金额',
    interest      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贴现息',
    settlement_id BIGINT                  DEFAULT NULL COMMENT '收付款单（收票、背书、开票）',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '凭证（贴现、托收、拒付、兑付）',
    bill_id       BIGINT                  DEFAULT NULL COMMENT '拒付时转回的应收单',
    remark        VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by     BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_note_log_note (note_id),
    KEY idx_note_log_settlement (settlement_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '票据操作记录';

-- 账户调拨：银行账户之间、银行与现金之间转账，生成凭证（借 转入账户、财务费用，贷 转出账户）
CREATE TABLE fin_fund_transfer (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    transfer_no     VARCHAR(32)    NOT NULL COMMENT '调拨单号',
    transfer_date   DATE           NOT NULL COMMENT '调拨日期',
    from_account_id BIGINT         NOT NULL COMMENT '转出科目',
    to_account_id   BIGINT         NOT NULL COMMENT '转入科目',
    amount          DECIMAL(18, 2) NOT NULL COMMENT '调拨金额',
    fee             DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手续费（由转出账户承担，计入财务费用）',
    voucher_id      BIGINT                  DEFAULT NULL COMMENT '凭证',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_no (transfer_no, tenant_id),
    KEY idx_transfer_date (transfer_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '账户调拨';

-- 资金计划的手工计划项（预计的其他收入、支出），与应收应付、票据、报销一起汇总为资金计划
CREATE TABLE fin_fund_plan_item (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT         NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_date   DATE           NOT NULL COMMENT '预计日期',
    direction   VARCHAR(8)     NOT NULL COMMENT '方向 IN流入 OUT流出',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    description VARCHAR(128)   NOT NULL COMMENT '说明，如 房租、借款到期',
    create_by   BIGINT                  DEFAULT NULL COMMENT '录入人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_fund_plan_date (plan_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资金计划项';

CREATE TABLE fin_bank_statement (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    account_id  BIGINT         NOT NULL COMMENT '银行存款科目（末级）',
    txn_date    DATE           NOT NULL COMMENT '交易日期',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额：收入为正、支出为负',
    summary     VARCHAR(255)            DEFAULT NULL COMMENT '摘要',
    ref_no      VARCHAR(64)             DEFAULT NULL COMMENT '银行流水号',
    entry_id    BIGINT                  DEFAULT NULL COMMENT '勾对的凭证分录',
    batch_no    VARCHAR(32)             DEFAULT NULL COMMENT '导入批次',
    create_by   BIGINT                  DEFAULT NULL COMMENT '导入人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_bank_statement_account (account_id, txn_date),
    KEY idx_bank_statement_entry (entry_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '银行对账单';

CREATE TABLE fin_expense_claim (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    claim_no         VARCHAR(32)    NOT NULL COMMENT '报销单号',
    user_id          BIGINT         NOT NULL COMMENT '报销人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '费用部门',
    claim_date       DATE           NOT NULL COMMENT '报销日期',
    amount           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '报销金额',
    loan_id          BIGINT                  DEFAULT NULL COMMENT '冲抵的借款',
    loan_offset      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '冲借款金额，实付 = 报销金额 - 冲借款',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_claim_status) DRAFT草稿 APPROVING审批中 APPROVED待付款 PAID已付款',
    pay_account_id   BIGINT                  DEFAULT NULL COMMENT '付款科目',
    pay_date         DATE                    DEFAULT NULL COMMENT '付款日期',
    voucher_id       BIGINT                  DEFAULT NULL COMMENT '付款凭证',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    remark           VARCHAR(255)            DEFAULT NULL COMMENT '事由',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_claim_no (claim_no, tenant_id),
    KEY idx_claim_user (user_id),
    KEY idx_claim_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '费用报销单';

CREATE TABLE fin_expense_claim_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    claim_id     BIGINT         NOT NULL COMMENT '报销单',
    expense_date DATE                    DEFAULT NULL COMMENT '费用日期',
    account_id   BIGINT         NOT NULL COMMENT '费用科目（末级）',
    project_id   BIGINT                  DEFAULT NULL COMMENT '项目（辅助核算）',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '金额',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_claim_item_claim (claim_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '费用报销明细';

CREATE TABLE fin_loan (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    loan_no          VARCHAR(32)    NOT NULL COMMENT '借款单号',
    user_id          BIGINT         NOT NULL COMMENT '借款人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '部门',
    loan_date        DATE           NOT NULL COMMENT '借款日期',
    amount           DECIMAL(18, 2) NOT NULL COMMENT '借款金额',
    repaid_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已还款（含报销冲抵）',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 fin_loan_status) DRAFT草稿 APPROVING审批中 APPROVED待付款 PAID已付款（未还清） CLEARED已还清',
    pay_account_id   BIGINT                  DEFAULT NULL COMMENT '付款科目',
    voucher_id       BIGINT                  DEFAULT NULL COMMENT '付款凭证',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    purpose          VARCHAR(255)            DEFAULT NULL COMMENT '借款事由',
    create_by        BIGINT                  DEFAULT NULL COMMENT '制单人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_loan_no (loan_no, tenant_id),
    KEY idx_loan_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工借款（备用金）：付款记其他应收款，还款或报销冲抵';

CREATE TABLE fin_loan_repay (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    loan_id     BIGINT         NOT NULL COMMENT '借款',
    repay_date  DATE           NOT NULL COMMENT '还款日期',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额',
    method      VARCHAR(16)    NOT NULL COMMENT '方式 CASH现金还款 CLAIM报销冲抵',
    claim_id    BIGINT                  DEFAULT NULL COMMENT '冲抵的报销单',
    account_id  BIGINT                  DEFAULT NULL COMMENT '收款科目（现金还款）',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '操作人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_loan_repay_loan (loan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '借款还款记录';

CREATE TABLE fin_asset (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code               VARCHAR(32)    NOT NULL COMMENT '资产编码',
    name               VARCHAR(64)    NOT NULL COMMENT '资产名称',
    category           VARCHAR(32)    NOT NULL COMMENT '类别(字典 fin_asset_category)',
    model              VARCHAR(64)             DEFAULT NULL COMMENT '规格型号',
    dept_id            BIGINT                  DEFAULT NULL COMMENT '使用部门',
    custodian_id       BIGINT                  DEFAULT NULL COMMENT '使用人（保管人）',
    location           VARCHAR(128)            DEFAULT NULL COMMENT '存放地点',
    use_date           DATE           NOT NULL COMMENT '开始使用日期，次月开始计提折旧',
    original_value     DECIMAL(18, 2) NOT NULL COMMENT '原值',
    residual_rate      DECIMAL(5, 2)  NOT NULL DEFAULT 5 COMMENT '残值率（%）',
    useful_months      INT            NOT NULL COMMENT '使用年限（月）',
    depreciated_months INT            NOT NULL DEFAULT 0 COMMENT '已计提月数',
    accumulated        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '累计折旧',
    expense_account_id BIGINT         NOT NULL COMMENT '折旧费用科目，如管理费用-折旧费、制造费用',
    status             VARCHAR(16)    NOT NULL COMMENT '状态(字典 fin_asset_status) IN_USE在用 IDLE闲置 DISPOSED已处置',
    dispose_date       DATE                    DEFAULT NULL COMMENT '处置日期',
    dispose_voucher_id BIGINT              DEFAULT NULL COMMENT '处置凭证，删除后资产恢复处置前的状态',
    prev_status        VARCHAR(16)         DEFAULT NULL COMMENT '处置前的状态',
    remark             VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time        DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time        DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '固定资产卡片';

CREATE TABLE fin_depreciation (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT            NOT NULL COMMENT '会计期间 yyyyMM',
    asset_id    BIGINT         NOT NULL COMMENT '资产',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '本期折旧额',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '生成的凭证',
    create_by   BIGINT                  DEFAULT NULL COMMENT '计提人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_depreciation (period, asset_id, tenant_id),
    KEY idx_depreciation_asset (asset_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '折旧计提记录';

CREATE TABLE fin_cost_run (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT          NOT NULL COMMENT '会计期间 yyyyMM',
    voucher_id  BIGINT                DEFAULT NULL COMMENT '生成的成本结转凭证',
    kind        VARCHAR(16)  NOT NULL DEFAULT 'RUN' COMMENT '类型 RUN成本核算 OPENING存货期初（启用期间的上一期）',
    overhead_variance DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '转入主营业务成本的制造费用差异',
    create_by   BIGINT                DEFAULT NULL COMMENT '核算人',
    create_time DATETIME              DEFAULT NULL COMMENT '核算时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_run_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '成本核算记录';

CREATE TABLE fin_material_cost (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period           INT            NOT NULL COMMENT '会计期间',
    material_id      BIGINT         NOT NULL COMMENT '物料',
    open_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '期初数量',
    open_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初金额',
    purchase_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '采购入库（扣除退货）',
    purchase_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '采购金额（不含税）',
    make_qty         DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '生产入库',
    make_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '生产成本',
    other_in_qty     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '其他入库（盘盈等）',
    other_in_amount  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他入库金额',
    issue_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '生产领用（扣除退料）',
    issue_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '生产领用金额',
    sales_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '销售出库（扣除退货）',
    sales_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售成本',
    other_out_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '其他出库（盘亏等）',
    other_out_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他出库金额',
    close_qty        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '期末数量',
    close_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期末金额',
    unit_cost        DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '本期出库单价（月末一次加权平均为加权平均单价，其他计价方法为本期出库的平均单价）',
    cost_method      VARCHAR(16)    NOT NULL DEFAULT 'WEIGHTED' COMMENT '本期使用的计价方法(字典 wms_cost_method)',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_cost (period, material_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '存货成本（按物料的计价方法核算）';

CREATE TABLE fin_cost_layer (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period      INT            NOT NULL COMMENT '会计期间',
    material_id BIGINT         NOT NULL COMMENT '物料',
    seq         INT            NOT NULL COMMENT '批次顺序，越小入库越早',
    in_date     DATE           NOT NULL COMMENT '入库日期',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '结存数量',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '结存金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_cost_layer (period, material_id, seq)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '先进先出的期末结存批次：下一期核算的期初批次';

CREATE TABLE fin_wo_cost (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period          INT            NOT NULL COMMENT '会计期间',
    wo_id           BIGINT         NOT NULL COMMENT '生产工单',
    wip_open        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期初在产品成本',
    material_cost   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期直接材料',
    labor_cost      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期直接人工 = 工时 × 人工费率',
    overhead_cost   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期制造费用 = 工时 × 制造费用率',
    hours           DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '本期报工工时',
    finished_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '本期完工入库数量',
    finished_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本期完工产品成本',
    unit_cost       DECIMAL(18, 6)          DEFAULT NULL COMMENT '本期完工单位成本',
    wip_close       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '期末在产品成本',
    loss_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '关闭工单转出的在产品成本（生产损失）',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wo_cost (period, wo_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单成本';

-- 标准成本：按版本保存（生效期间），采购件手工维护单价，自制件按默认 BOM 和工艺路线卷算；成本差异分析取所在期间生效的版本
CREATE TABLE fin_std_cost_version (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name             VARCHAR(64)  NOT NULL COMMENT '版本名称，如 2026 年标准成本',
    effective_period INT          NOT NULL COMMENT '生效期间 yyyyMM：期间 P 的差异分析使用生效期间不晚于 P 的最近一个已发布版本',
    status           VARCHAR(16)  NOT NULL COMMENT '状态(字典 fin_std_cost_status) DRAFT草稿 RELEASED已发布',
    rollup_time      DATETIME              DEFAULT NULL COMMENT '最近卷算时间',
    release_time     DATETIME              DEFAULT NULL COMMENT '发布时间',
    remark           VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_std_cost_version_name (name, tenant_id),
    KEY idx_std_cost_version_period (effective_period)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '标准成本版本';

CREATE TABLE fin_std_cost (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    version_id    BIGINT         NOT NULL COMMENT '标准成本版本',
    material_id   BIGINT         NOT NULL COMMENT '物料',
    make          TINYINT        NOT NULL DEFAULT 0 COMMENT '1 自制件（按 BOM 卷算） 0 采购件（手工维护）',
    bom_id        BIGINT                  DEFAULT NULL COMMENT '卷算使用的 BOM',
    material_cost DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '单位材料成本：采购件为标准单价，自制件为子件标准成本 × 用量（含损耗）',
    labor_cost    DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '单位人工成本 = 标准工时 × 人工费率',
    overhead_cost DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '单位制造费用 = 标准工时 × 制造费用率',
    unit_cost     DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '单位标准成本 = 材料 + 人工 + 制造费用',
    std_hours     DECIMAL(18, 6) NOT NULL DEFAULT 0 COMMENT '单位标准工时（本级工序合计）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_std_cost (version_id, material_id, tenant_id),
    KEY idx_std_cost_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料标准成本';

CREATE TABLE fin_budget (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    budget_year INT            NOT NULL COMMENT '预算年度',
    dept_id     BIGINT         NOT NULL DEFAULT 0 COMMENT '部门，0 为全公司',
    account_id  BIGINT         NOT NULL COMMENT '科目（可以是上级科目，执行数按下级汇总）',
    m01         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '1 月预算',
    m02         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '2 月预算',
    m03         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '3 月预算',
    m04         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '4 月预算',
    m05         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '5 月预算',
    m06         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '6 月预算',
    m07         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '7 月预算',
    m08         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '8 月预算',
    m09         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '9 月预算',
    m10         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '10 月预算',
    m11         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '11 月预算',
    m12         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '12 月预算',
    total       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '全年预算 = 各月合计',
    control_mode VARCHAR(8)       NOT NULL DEFAULT 'NONE' COMMENT '预算控制(字典 fin_budget_control) NONE不控制 WARN超预算提示 BLOCK超预算禁止（费用报销）',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget (budget_year, dept_id, account_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预算';

CREATE TABLE fin_commission_rule (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name              VARCHAR(64)    NOT NULL COMMENT '规则名称',
    basis             VARCHAR(16)    NOT NULL COMMENT '提成依据(字典 fin_commission_basis) SALES销售额（不含税） PROFIT毛利 RECEIPT回款',
    salesman_id       BIGINT                  DEFAULT NULL COMMENT '适用业务员，为空适用全部',
    material_category VARCHAR(32)             DEFAULT NULL COMMENT '适用物料分类(字典 wms_material_category)，为空适用全部；回款不区分分类',
    threshold         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '业绩门槛：业务员当月该依据的业绩达到此金额时适用（阶梯提成）',
    rate              DECIMAL(6, 3)  NOT NULL COMMENT '提成比例（%）',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成规则：同一业务员、分类、依据取最具体（指定业务员 > 指定分类）且门槛最高的已达成规则';

CREATE TABLE fin_commission (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period       INT            NOT NULL COMMENT '期间 yyyyMM',
    status       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 CONFIRMED已确认（计入工资表）',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '提成合计（含调整）',
    remark       VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by    BIGINT                  DEFAULT NULL COMMENT '计算人',
    confirm_by   BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time  DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_commission_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成表：按期间计算各业务员的业绩和提成';

CREATE TABLE fin_commission_line (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    commission_id     BIGINT         NOT NULL COMMENT '提成表',
    salesman_id       BIGINT         NOT NULL COMMENT '业务员',
    sales_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售额（不含税，扣除退货）',
    profit_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '毛利（销售额 - 成本）',
    receipt_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '回款',
    commission_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '按规则计算的提成',
    adjust_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '手工调整',
    final_amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '应发提成 = 提成 + 调整',
    detail            TEXT COMMENT '计算明细（JSON）：依据、分类、业绩、规则、比例、提成',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注（调整原因）',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_commission_line (commission_id, salesman_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售提成表明细：每个业务员一行';

CREATE TABLE fin_account_rule (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    role        VARCHAR(32) NOT NULL COMMENT '用途（AccountRole），如 RECEIVABLE 应收账款、INVENTORY 存货',
    dim_value   VARCHAR(32) NOT NULL DEFAULT '' COMMENT '维度取值：物料分类 / 职位类别，空串为该用途的默认科目',
    account_id  BIGINT      NOT NULL COMMENT '科目（末级）',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_rule (role, dim_value, tenant_id),
    KEY idx_account_rule_account (account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '凭证科目设置：业务单据自动生成凭证时各用途使用的科目，没有设置的用途取预置科目';

-- ---------------------------------------------------------------- 人力资源

CREATE TABLE hr_position (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code        VARCHAR(32) NOT NULL COMMENT '职位编码',
    name        VARCHAR(64) NOT NULL COMMENT '职位名称',
    category    VARCHAR(16)          DEFAULT NULL COMMENT '职位类别(字典 hr_position_category)',
    grade       VARCHAR(16)          DEFAULT NULL COMMENT '职级，如 P5、M2',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    headcount   INT                  DEFAULT NULL COMMENT '编制人数，为空不限；招聘需求审核时校验',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark      VARCHAR(255)         DEFAULT NULL COMMENT '职责说明',
    create_time DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_position_code (code, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '职位';

CREATE TABLE hr_employee (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    emp_no            VARCHAR(32)  NOT NULL COMMENT '工号',
    name              VARCHAR(64)  NOT NULL COMMENT '姓名',
    gender            CHAR(1)               DEFAULT NULL COMMENT '性别 M男 F女',
    birth_date        DATE                  DEFAULT NULL COMMENT '出生日期',
    id_card           VARCHAR(32)           DEFAULT NULL COMMENT '身份证号（列表脱敏）',
    phone             VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
    email             VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    education         VARCHAR(16)           DEFAULT NULL COMMENT '学历(字典 hr_education)',
    dept_id           BIGINT       NOT NULL COMMENT '部门',
    position_id       BIGINT                DEFAULT NULL COMMENT '职位',
    user_id           BIGINT                DEFAULT NULL COMMENT '关联的登录账号，员工自助使用',
    shift_id          BIGINT                DEFAULT NULL COMMENT '班次，为空使用默认班次',
    emp_type          VARCHAR(16)  NOT NULL DEFAULT 'FULL_TIME' COMMENT '用工类型(字典 hr_emp_type)',
    status            VARCHAR(16)  NOT NULL COMMENT '状态(字典 hr_emp_status) PROBATION试用 REGULAR正式 LEFT离职',
    entry_date        DATE         NOT NULL COMMENT '入职日期',
    work_start_date   DATE                  DEFAULT NULL COMMENT '参加工作日期（法定年假按累计工作年限），为空按入职日期',
    regular_date      DATE                  DEFAULT NULL COMMENT '转正日期（试用期为计划转正日期）',
    leave_date        DATE                  DEFAULT NULL COMMENT '离职日期',
    leave_reason      VARCHAR(255)          DEFAULT NULL COMMENT '离职原因',
    address           VARCHAR(255)          DEFAULT NULL COMMENT '住址',
    emergency_contact VARCHAR(64)           DEFAULT NULL COMMENT '紧急联系人',
    emergency_phone   VARCHAR(32)           DEFAULT NULL COMMENT '紧急联系电话',
    remark            VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time       DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_no (emp_no, tenant_id),
    UNIQUE KEY uk_emp_user (user_id, tenant_id),
    KEY idx_emp_dept (dept_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工档案';

CREATE TABLE hr_employee_change (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    employee_id      BIGINT         NOT NULL COMMENT '员工',
    type             VARCHAR(16)    NOT NULL COMMENT '异动类型(字典 hr_change_type) ENTRY入职 REGULAR转正 TRANSFER调岗 SALARY调薪 LEAVE离职',
    change_date      DATE           NOT NULL COMMENT '生效日期',
    from_dept_id     BIGINT                  DEFAULT NULL COMMENT '原部门',
    to_dept_id       BIGINT                  DEFAULT NULL COMMENT '新部门',
    from_position_id BIGINT                  DEFAULT NULL COMMENT '原职位',
    to_position_id   BIGINT                  DEFAULT NULL COMMENT '新职位',
    from_salary      DECIMAL(18, 2)          DEFAULT NULL COMMENT '原基本工资',
    to_salary        DECIMAL(18, 2)          DEFAULT NULL COMMENT '新基本工资',
    reason           VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DONE' COMMENT '状态 DONE已生效 APPROVING审批中 REJECTED已拒绝 CANCELED已撤回',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    handover_user_id BIGINT                  DEFAULT NULL COMMENT '离职交接人',
    create_by        BIGINT                  DEFAULT NULL COMMENT '办理人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_emp_change (employee_id, change_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '人事异动';

CREATE TABLE hr_contract (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_no      VARCHAR(32)  NOT NULL COMMENT '合同编号',
    employee_id      BIGINT       NOT NULL COMMENT '员工',
    contract_type    VARCHAR(16)  NOT NULL COMMENT '合同类型(字典 hr_contract_type) FIXED固定期限 OPEN无固定期限 TASK以完成一定工作任务为期限 INTERN实习协议 LABOR劳务协议',
    sign_date        DATE         NOT NULL COMMENT '签订日期',
    start_date       DATE         NOT NULL COMMENT '开始日期',
    end_date         DATE                  DEFAULT NULL COMMENT '结束日期，无固定期限为空',
    probation_months INT          NOT NULL DEFAULT 0 COMMENT '试用期（月）',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(字典 hr_contract_status) ACTIVE生效中 EXPIRED已到期 RENEWED已续签 TERMINATED已终止',
    prev_contract_id BIGINT                DEFAULT NULL COMMENT '续签前的合同',
    terminate_date   DATE                  DEFAULT NULL COMMENT '终止日期',
    terminate_reason VARCHAR(255)          DEFAULT NULL COMMENT '终止原因',
    remark           VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_no (contract_no, tenant_id),
    KEY idx_contract_employee (employee_id),
    KEY idx_contract_end (status, end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '劳动合同';

CREATE TABLE hr_recruit (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    recruit_no       VARCHAR(32)   NOT NULL COMMENT '需求单号',
    title            VARCHAR(128)  NOT NULL COMMENT '招聘岗位名称',
    dept_id          BIGINT        NOT NULL COMMENT '用人部门',
    position_id      BIGINT                 DEFAULT NULL COMMENT '职位',
    headcount        INT           NOT NULL COMMENT '招聘人数',
    hired_count      INT           NOT NULL DEFAULT 0 COMMENT '已录用人数',
    salary_range     VARCHAR(64)            DEFAULT NULL COMMENT '薪资范围',
    requirement      VARCHAR(1000)          DEFAULT NULL COMMENT '任职要求',
    reason           VARCHAR(255)           DEFAULT NULL COMMENT '招聘原因',
    deadline         DATE                   DEFAULT NULL COMMENT '期望到岗日期',
    status           VARCHAR(16)   NOT NULL COMMENT '状态(字典 hr_recruit_status) DRAFT草稿 APPROVING审批中 OPEN招聘中 CLOSED已关闭',
    flow_instance_id VARCHAR(64)            DEFAULT NULL COMMENT '审批流程实例',
    create_by        BIGINT                 DEFAULT NULL COMMENT '申请人',
    create_time      DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_recruit_no (recruit_no, tenant_id),
    KEY idx_recruit_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '招聘需求';

CREATE TABLE hr_candidate (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    recruit_id      BIGINT         NOT NULL COMMENT '招聘需求',
    name            VARCHAR(64)    NOT NULL COMMENT '姓名',
    gender          CHAR(1)                 DEFAULT NULL COMMENT '性别 M男 F女',
    phone           VARCHAR(32)             DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(128)            DEFAULT NULL COMMENT '邮箱',
    education       VARCHAR(16)             DEFAULT NULL COMMENT '学历(字典 hr_education)',
    school          VARCHAR(128)            DEFAULT NULL COMMENT '毕业院校',
    work_years      INT                     DEFAULT NULL COMMENT '工作年限',
    source          VARCHAR(16)             DEFAULT NULL COMMENT '简历来源(字典 hr_candidate_source)',
    expected_salary DECIMAL(18, 2)          DEFAULT NULL COMMENT '期望月薪',
    stage           VARCHAR(16)    NOT NULL COMMENT '阶段(字典 hr_candidate_stage) NEW新简历 INTERVIEW面试中 OFFER已发offer HIRED已入职 REJECTED已淘汰',
    resume          VARCHAR(2000)           DEFAULT NULL COMMENT '简历摘要',
    employee_id     BIGINT                  DEFAULT NULL COMMENT '入职后的员工',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by       BIGINT                  DEFAULT NULL COMMENT '录入人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_candidate_recruit (recruit_id, stage)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应聘者';

CREATE TABLE hr_interview (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    candidate_id   BIGINT        NOT NULL COMMENT '应聘者',
    round          INT           NOT NULL COMMENT '轮次',
    interview_time DATETIME      NOT NULL COMMENT '面试时间',
    interviewer_id BIGINT                 DEFAULT NULL COMMENT '面试官',
    method         VARCHAR(16)            DEFAULT NULL COMMENT '方式 ONSITE现场 VIDEO视频 PHONE电话',
    score          INT                    DEFAULT NULL COMMENT '评分（0-100）',
    result         VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '结果(字典 hr_interview_result) PENDING待面试 PASS通过 FAIL未通过',
    feedback       VARCHAR(1000)          DEFAULT NULL COMMENT '面试评价',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_interview_candidate (candidate_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试记录';

CREATE TABLE hr_training (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    title       VARCHAR(128)   NOT NULL COMMENT '培训主题',
    category    VARCHAR(16)             DEFAULT NULL COMMENT '类别(字典 hr_training_category)',
    trainer     VARCHAR(64)             DEFAULT NULL COMMENT '讲师',
    start_date  DATE           NOT NULL COMMENT '开始日期',
    end_date    DATE           NOT NULL COMMENT '结束日期',
    hours       DECIMAL(8, 1)  NOT NULL DEFAULT 0 COMMENT '课时（小时）',
    location    VARCHAR(128)            DEFAULT NULL COMMENT '地点',
    cost        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '培训费用',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '培训费凭证',
    status      VARCHAR(16)    NOT NULL COMMENT '状态(字典 hr_training_status) PLANNED计划中 ONGOING进行中 FINISHED已结束 CANCELED已取消',
    content     VARCHAR(1000)           DEFAULT NULL COMMENT '培训内容',
    create_by   BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_training_date (start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训计划';

CREATE TABLE hr_training_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    training_id BIGINT        NOT NULL COMMENT '培训',
    employee_id BIGINT        NOT NULL COMMENT '学员',
    attended    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否出勤 1是 0否',
    score       DECIMAL(5, 1)          DEFAULT NULL COMMENT '考核成绩',
    passed      TINYINT                DEFAULT NULL COMMENT '是否通过 1是 0否',
    feedback    VARCHAR(255)           DEFAULT NULL COMMENT '评价',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_record (training_id, employee_id, tenant_id),
    KEY idx_training_record_emp (employee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '参训记录';

CREATE TABLE hr_shift (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name         VARCHAR(64)  NOT NULL COMMENT '班次名称',
    start_time   TIME         NOT NULL COMMENT '上班时间',
    end_time     TIME         NOT NULL COMMENT '下班时间',
    flex_minutes INT          NOT NULL DEFAULT 0 COMMENT '迟到 / 早退宽限（分钟）',
    work_hours   DECIMAL(4, 1) NOT NULL DEFAULT 8 COMMENT '标准工时（小时），请假按此折算天数',
    is_default   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认班次，未指定班次的员工使用',
    remark       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time  DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班次';

CREATE TABLE hr_holiday (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    holiday_date DATE        NOT NULL COMMENT '日期',
    name         VARCHAR(64) NOT NULL COMMENT '名称，如 国庆节',
    type         VARCHAR(8)  NOT NULL COMMENT '类型(字典 hr_holiday_type) HOLIDAY法定节假日 WORKDAY调休上班',
    create_time  DATETIME             DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_date (holiday_date, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '节假日（工作日 = 周一至周五 + 调休上班 - 法定节假日）';

CREATE TABLE hr_attendance (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    employee_id   BIGINT        NOT NULL COMMENT '员工',
    work_date     DATE          NOT NULL COMMENT '日期',
    check_in      DATETIME               DEFAULT NULL COMMENT '上班打卡',
    check_out     DATETIME               DEFAULT NULL COMMENT '下班打卡',
    status        VARCHAR(16)   NOT NULL COMMENT '状态(字典 hr_attendance_status) NORMAL正常 LATE迟到 EARLY早退 LATE_EARLY迟到早退 MISSING缺卡',
    late_minutes  INT           NOT NULL DEFAULT 0 COMMENT '迟到分钟',
    early_minutes INT           NOT NULL DEFAULT 0 COMMENT '早退分钟',
    work_hours    DECIMAL(5, 2) NOT NULL DEFAULT 0 COMMENT '出勤时长（小时）',
    source        VARCHAR(16)   NOT NULL DEFAULT 'CLOCK' COMMENT '来源 CLOCK打卡 MANUAL补录',
    remark        VARCHAR(255)           DEFAULT NULL COMMENT '补录说明',
    create_time   DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance (employee_id, work_date, tenant_id),
    KEY idx_attendance_date (work_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考勤记录（每人每天一条）';

CREATE TABLE hr_leave (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    leave_no         VARCHAR(32)  NOT NULL COMMENT '申请单号',
    employee_id      BIGINT       NOT NULL COMMENT '员工',
    kind             VARCHAR(16)  NOT NULL COMMENT '种类 LEAVE请假 OVERTIME加班',
    type             VARCHAR(16)           DEFAULT NULL COMMENT '请假：假期类型(字典 hr_leave_type)；加班：补偿方式(字典 hr_overtime_comp) PAY加班费 TO_LEAVE转调休',
    start_time       DATETIME     NOT NULL COMMENT '开始时间',
    end_time         DATETIME     NOT NULL COMMENT '结束时间',
    hours            DECIMAL(6, 1) NOT NULL COMMENT '时长（小时）',
    reason           VARCHAR(255)          DEFAULT NULL COMMENT '事由',
    status           VARCHAR(16)  NOT NULL COMMENT '状态(字典 hr_leave_status) APPROVING审批中 APPROVED已通过 REJECTED已拒绝 CANCELED已撤销',
    flow_instance_id VARCHAR(64)           DEFAULT NULL COMMENT '审批流程实例，为空时由人事审批',
    approve_by       BIGINT                DEFAULT NULL COMMENT '审批人',
    approve_time     DATETIME              DEFAULT NULL COMMENT '审批时间',
    approve_comment  VARCHAR(255)          DEFAULT NULL COMMENT '审批意见',
    dept_id          BIGINT                DEFAULT NULL COMMENT '员工部门（数据权限）',
    create_by        BIGINT                DEFAULT NULL COMMENT '申请人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_leave_no (leave_no, tenant_id),
    KEY idx_leave_emp (employee_id, start_time),
    KEY idx_leave_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '请假 / 加班申请';

CREATE TABLE hr_salary (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    employee_id     BIGINT         NOT NULL COMMENT '员工',
    base_salary     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '基本工资（月）',
    post_allowance  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '岗位津贴',
    other_allowance DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他补贴（餐补、交通等，不参与缺勤扣款）',
    perf_bonus_base DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '绩效奖金基数，实发 = 基数 × 绩效系数',
    social_base     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '社保缴费基数',
    housing_base    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '公积金缴费基数',
    housing_rate    DECIMAL(5, 2)  NOT NULL DEFAULT 7 COMMENT '公积金比例（%，个人与单位相同）',
    scheme_id       BIGINT                  DEFAULT NULL COMMENT '社保方案，为空按默认比例（个人 10.5%、单位 26%）',
    bank_name       VARCHAR(128)            DEFAULT NULL COMMENT '工资卡开户行',
    bank_account    VARCHAR(64)             DEFAULT NULL COMMENT '工资卡账号',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_salary_emp (employee_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '薪资档案';

CREATE TABLE hr_social_scheme (
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id             BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name                  VARCHAR(64)    NOT NULL COMMENT '方案名称，如 上海社保',
    city                  VARCHAR(32)             DEFAULT NULL COMMENT '缴纳城市',
    base_min              DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '社保基数下限',
    base_max              DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '社保基数上限，0 表示不限',
    pension_personal      DECIMAL(5, 2)  NOT NULL DEFAULT 8 COMMENT '养老保险个人比例（%）',
    pension_company       DECIMAL(5, 2)  NOT NULL DEFAULT 16 COMMENT '养老保险单位比例（%）',
    medical_personal      DECIMAL(5, 2)  NOT NULL DEFAULT 2 COMMENT '医疗保险个人比例（%）',
    medical_company       DECIMAL(5, 2)  NOT NULL DEFAULT 8 COMMENT '医疗保险（含生育）单位比例（%）',
    unemployment_personal DECIMAL(5, 2)  NOT NULL DEFAULT 0.5 COMMENT '失业保险个人比例（%）',
    unemployment_company  DECIMAL(5, 2)  NOT NULL DEFAULT 0.5 COMMENT '失业保险单位比例（%）',
    injury_company        DECIMAL(5, 2)  NOT NULL DEFAULT 0.2 COMMENT '工伤保险单位比例（%）',
    housing_min           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '公积金基数下限',
    housing_max           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '公积金基数上限，0 表示不限',
    status                TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    remark                VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time           DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time           DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社保方案：缴纳城市的基数上下限和各险种比例';

CREATE TABLE hr_tax_deduction (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    employee_id    BIGINT         NOT NULL COMMENT '员工',
    type           VARCHAR(16)    NOT NULL COMMENT '扣除项目(字典 hr_deduction_type) CHILD_EDU子女教育 CONTINUING_EDU继续教育 HOUSING_LOAN住房贷款利息 HOUSING_RENT住房租金 ELDERLY赡养老人 INFANT婴幼儿照护',
    monthly_amount DECIMAL(18, 2) NOT NULL COMMENT '每月扣除金额',
    start_period   INT            NOT NULL COMMENT '开始月份 yyyyMM',
    end_period     INT                     DEFAULT NULL COMMENT '结束月份 yyyyMM，为空表示持续',
    remark         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tax_deduction_emp (employee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '个税专项附加扣除';

CREATE TABLE hr_salary_history (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    employee_id     BIGINT         NOT NULL COMMENT '员工',
    effective_date  DATE           NOT NULL COMMENT '生效日期',
    base_salary     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    post_allowance  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '岗位津贴',
    other_allowance DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他补贴',
    perf_bonus_base DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '绩效奖金基数',
    social_base     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '社保缴费基数',
    housing_base    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '公积金缴费基数',
    housing_rate    DECIMAL(5, 2)  NOT NULL DEFAULT 7 COMMENT '公积金比例（%）',
    scheme_id       BIGINT                  DEFAULT NULL COMMENT '社保方案',
    remark          VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_salary_history (employee_id, effective_date, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '薪资历史（按生效日期，工资表按月末生效的薪资计算）';

CREATE TABLE hr_payroll (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    payroll_no        VARCHAR(32)    NOT NULL COMMENT '工资表号',
    period            INT            NOT NULL COMMENT '工资月份 yyyyMM',
    status            VARCHAR(16)    NOT NULL COMMENT '状态(字典 hr_payroll_status) DRAFT草稿 APPROVING审批中 CONFIRMED已确认 PAID已发放',
    headcount         INT            NOT NULL DEFAULT 0 COMMENT '人数',
    gross_total       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '应发合计',
    personal_total    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '个人社保公积金合计',
    tax_total         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '个税合计',
    net_total         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '实发合计',
    company_total     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '单位社保公积金合计',
    pay_date          DATE                    DEFAULT NULL COMMENT '发放日期',
    accrue_voucher_id BIGINT                  DEFAULT NULL COMMENT '计提凭证',
    flow_instance_id  VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例ID',
    pay_voucher_id    BIGINT                  DEFAULT NULL COMMENT '发放凭证',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by         BIGINT                  DEFAULT NULL COMMENT '制表人',
    confirm_by        BIGINT                  DEFAULT NULL COMMENT '确认人',
    confirm_time      DATETIME                DEFAULT NULL COMMENT '确认时间',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payroll_no (payroll_no, tenant_id),
    UNIQUE KEY uk_payroll_period (period, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工资表';

CREATE TABLE hr_payroll_item (
    id                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    payroll_id        BIGINT         NOT NULL COMMENT '工资表',
    employee_id       BIGINT         NOT NULL COMMENT '员工',
    dept_id           BIGINT                  DEFAULT NULL COMMENT '部门（计提凭证按部门辅助核算）',
    should_days       DECIMAL(5, 1)  NOT NULL DEFAULT 0 COMMENT '本月应出勤天数（在职期间的工作日）',
    month_days        DECIMAL(5, 1)  NOT NULL DEFAULT 0 COMMENT '本月工作日天数',
    absent_days       DECIMAL(5, 1)  NOT NULL DEFAULT 0 COMMENT '旷工天数',
    late_count        INT            NOT NULL DEFAULT 0 COMMENT '迟到早退次数',
    personal_hours    DECIMAL(6, 1)  NOT NULL DEFAULT 0 COMMENT '事假小时',
    sick_hours        DECIMAL(6, 1)  NOT NULL DEFAULT 0 COMMENT '病假小时',
    overtime_hours    DECIMAL(6, 1)  NOT NULL DEFAULT 0 COMMENT '加班小时',
    base_salary       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '基本工资（按在职天数折算）',
    allowance         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '津贴补贴',
    overtime_pay      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '加班费',
    piece_wage        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '计件工资',
    commission        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售提成（取已确认的销售提成表）',
    perf_grade        VARCHAR(4)              DEFAULT NULL COMMENT '绩效等级',
    perf_bonus        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '绩效奖金',
    other_bonus       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他奖金（手工调整）',
    attendance_deduct DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '考勤扣款（旷工、事假、病假、迟到早退）',
    gross_pay         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '应发工资',
    social_personal   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '个人社保',
    housing_personal  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '个人公积金',
    special_deduct    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '专项附加扣除（本月）',
    income_tax        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '个人所得税',
    cum_taxable       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本年累计应纳税所得额（累计预扣法，含本月）',
    cum_tax           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本年累计已预扣个税（含本月）',
    other_deduct      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '其他扣款（手工调整）',
    net_pay           DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '实发工资',
    social_company    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '单位社保',
    housing_company   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '单位公积金',
    remark            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time       DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time       DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payroll_item (payroll_id, employee_id, tenant_id),
    KEY idx_payroll_item_emp (employee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工资明细';

CREATE TABLE hr_perf_plan (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(128) NOT NULL COMMENT '考核名称，如 2026年第三季度绩效考核',
    period_type VARCHAR(8)   NOT NULL COMMENT '周期 MONTH月度 QUARTER季度 YEAR年度',
    start_date  DATE         NOT NULL COMMENT '考核开始日期',
    end_date    DATE         NOT NULL COMMENT '考核结束日期',
    pay_period  INT                   DEFAULT NULL COMMENT '绩效奖金计入的工资月份 yyyyMM，为空不发放',
    status      VARCHAR(16)  NOT NULL COMMENT '状态(字典 hr_perf_status) DRAFT草稿 SCORING考核中 FINISHED已归档',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '绩效考核方案';

CREATE TABLE hr_perf_indicator (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_id     BIGINT        NOT NULL COMMENT '考核方案',
    name        VARCHAR(64)   NOT NULL COMMENT '指标名称',
    weight      DECIMAL(5, 2) NOT NULL COMMENT '权重（%），合计 100',
    description VARCHAR(255)           DEFAULT NULL COMMENT '评分标准',
    sort        INT           NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_indicator_plan (plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考核指标';

CREATE TABLE hr_perf_appraisal (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    plan_id        BIGINT        NOT NULL COMMENT '考核方案',
    employee_id    BIGINT        NOT NULL COMMENT '被考核人',
    dept_id        BIGINT                 DEFAULT NULL COMMENT '部门',
    reviewer_id    BIGINT                 DEFAULT NULL COMMENT '评分人（部门负责人）',
    self_score     DECIMAL(5, 1)          DEFAULT NULL COMMENT '自评总分（加权）',
    score          DECIMAL(5, 1)          DEFAULT NULL COMMENT '最终得分（加权）',
    grade          VARCHAR(4)             DEFAULT NULL COMMENT '等级(字典 hr_perf_grade) S A B C D',
    status         VARCHAR(16)   NOT NULL COMMENT '状态(字典 hr_appraisal_status) SELF待自评 REVIEW待评分 DONE已完成',
    self_comment   VARCHAR(500)           DEFAULT NULL COMMENT '自评总结',
    review_comment VARCHAR(500)           DEFAULT NULL COMMENT '评语',
    review_time    DATETIME               DEFAULT NULL COMMENT '评分时间',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appraisal (plan_id, employee_id, tenant_id),
    KEY idx_appraisal_reviewer (reviewer_id, status),
    KEY idx_appraisal_emp (employee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考核表';

CREATE TABLE hr_perf_score (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    appraisal_id BIGINT        NOT NULL COMMENT '考核表',
    indicator_id BIGINT        NOT NULL COMMENT '指标',
    self_score   DECIMAL(5, 1)          DEFAULT NULL COMMENT '自评分（0-100）',
    score        DECIMAL(5, 1)          DEFAULT NULL COMMENT '评分（0-100）',
    create_time  DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perf_score (appraisal_id, indicator_id, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '指标得分';

-- ---------------------------------------------------------------- 客户关系管理

CREATE TABLE crm_customer (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    customer_id      BIGINT       NOT NULL COMMENT '客户（scm_customer），一对一',
    owner_id         BIGINT                DEFAULT NULL COMMENT '负责人，为空表示在公海',
    dept_id          BIGINT                DEFAULT NULL COMMENT '负责人部门（数据权限）',
    industry         VARCHAR(32)           DEFAULT NULL COMMENT '行业(字典 crm_industry)',
    source           VARCHAR(16)           DEFAULT NULL COMMENT '来源(字典 crm_source)',
    stage            VARCHAR(16)  NOT NULL DEFAULT 'POTENTIAL' COMMENT '客户阶段(字典 crm_customer_stage) POTENTIAL潜在 INTENT意向 DEAL成交 LOST流失',
    region           VARCHAR(64)           DEFAULT NULL COMMENT '所在地区',
    website          VARCHAR(128)          DEFAULT NULL COMMENT '网站',
    last_follow_time DATETIME              DEFAULT NULL COMMENT '最近跟进时间',
    next_follow_time DATETIME              DEFAULT NULL COMMENT '下次跟进时间',
    claim_time       DATETIME              DEFAULT NULL COMMENT '领取 / 分配时间',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_crm_customer (customer_id, tenant_id),
    KEY idx_crm_customer_owner (owner_id),
    KEY idx_crm_customer_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户经营信息（客户主数据在 scm_customer）';

CREATE TABLE crm_contact (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    customer_id BIGINT       NOT NULL COMMENT '客户',
    name        VARCHAR(64)  NOT NULL COMMENT '姓名',
    title       VARCHAR(64)           DEFAULT NULL COMMENT '职务',
    phone       VARCHAR(32)           DEFAULT NULL COMMENT '电话',
    email       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    wechat      VARCHAR(64)           DEFAULT NULL COMMENT '微信',
    is_primary  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否首要联系人',
    birthday    DATE                  DEFAULT NULL COMMENT '生日',
    remark      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    create_time DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_contact_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '联系人';

CREATE TABLE crm_lead (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    lead_no          VARCHAR(32)  NOT NULL COMMENT '线索编号',
    company          VARCHAR(128) NOT NULL COMMENT '公司名称',
    contact_name     VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
    title            VARCHAR(64)           DEFAULT NULL COMMENT '职务',
    phone            VARCHAR(32)           DEFAULT NULL COMMENT '电话',
    email            VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    industry         VARCHAR(32)           DEFAULT NULL COMMENT '行业(字典 crm_industry)',
    source           VARCHAR(16)           DEFAULT NULL COMMENT '来源(字典 crm_source)',
    campaign_id      BIGINT                DEFAULT NULL COMMENT '来源市场活动',
    region           VARCHAR(64)           DEFAULT NULL COMMENT '地区',
    requirement      VARCHAR(500)          DEFAULT NULL COMMENT '需求描述',
    owner_id         BIGINT                DEFAULT NULL COMMENT '负责人',
    dept_id          BIGINT                DEFAULT NULL COMMENT '负责人部门（数据权限）',
    status           VARCHAR(16)  NOT NULL COMMENT '状态(字典 crm_lead_status) NEW待跟进 FOLLOWING跟进中 CONVERTED已转化 INVALID无效',
    customer_id      BIGINT                DEFAULT NULL COMMENT '转化后的客户',
    convert_time     DATETIME              DEFAULT NULL COMMENT '转化时间',
    invalid_reason   VARCHAR(255)          DEFAULT NULL COMMENT '无效原因',
    last_follow_time DATETIME              DEFAULT NULL COMMENT '最近跟进时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lead_no (lead_no, tenant_id),
    KEY idx_lead_owner (owner_id, status),
    KEY idx_lead_campaign (campaign_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售线索';

CREATE TABLE crm_followup (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    target_type VARCHAR(16)   NOT NULL COMMENT '跟进对象 CUSTOMER客户 LEAD线索 OPPORTUNITY商机',
    target_id   BIGINT        NOT NULL COMMENT '对象ID',
    customer_id BIGINT                 DEFAULT NULL COMMENT '所属客户（商机的客户），客户详情汇总使用',
    method      VARCHAR(16)   NOT NULL COMMENT '方式(字典 crm_follow_method)',
    content     VARCHAR(1000) NOT NULL COMMENT '跟进内容',
    follow_time DATETIME      NOT NULL COMMENT '跟进时间',
    next_time   DATETIME               DEFAULT NULL COMMENT '下次跟进时间',
    next_plan   VARCHAR(255)           DEFAULT NULL COMMENT '下次跟进计划',
    dept_id     BIGINT                 DEFAULT NULL COMMENT '跟进人部门（数据权限）',
    create_by   BIGINT                 DEFAULT NULL COMMENT '跟进人',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_followup_target (target_type, target_id),
    KEY idx_followup_customer (customer_id),
    KEY idx_followup_user (create_by, follow_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '跟进记录';

CREATE TABLE crm_opportunity (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    opp_no         VARCHAR(32)    NOT NULL COMMENT '商机编号',
    name           VARCHAR(128)   NOT NULL COMMENT '商机名称',
    customer_id    BIGINT         NOT NULL COMMENT '客户',
    contact_id     BIGINT                  DEFAULT NULL COMMENT '联系人',
    amount         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '预计成交金额（含税），有产品明细时为明细合计',
    stage          VARCHAR(16)    NOT NULL COMMENT '销售阶段(字典 crm_opp_stage) INITIAL初步接洽 NEEDS需求确认 PROPOSAL方案报价 NEGOTIATION商务谈判 WON赢单 LOST输单',
    probability    INT            NOT NULL DEFAULT 10 COMMENT '赢单概率（%），默认按阶段',
    expected_date  DATE                    DEFAULT NULL COMMENT '预计成交日期',
    source         VARCHAR(16)             DEFAULT NULL COMMENT '来源(字典 crm_source)',
    campaign_id    BIGINT                  DEFAULT NULL COMMENT '来源市场活动',
    lead_id        BIGINT                  DEFAULT NULL COMMENT '来源线索',
    owner_id       BIGINT                  DEFAULT NULL COMMENT '负责人',
    dept_id        BIGINT                  DEFAULT NULL COMMENT '负责人部门（数据权限）',
    close_date     DATE                    DEFAULT NULL COMMENT '赢单 / 输单日期',
    lost_reason    VARCHAR(255)            DEFAULT NULL COMMENT '输单原因',
    sales_order_id BIGINT                  DEFAULT NULL COMMENT '生成的销售订单',
    sales_order_no VARCHAR(32)             DEFAULT NULL COMMENT '销售订单号',
    remark         VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    create_by      BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_opp_no (opp_no, tenant_id),
    KEY idx_opp_customer (customer_id),
    KEY idx_opp_owner (owner_id, stage),
    KEY idx_opp_campaign (campaign_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售机会';

CREATE TABLE crm_contract (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_no      VARCHAR(32)    NOT NULL COMMENT '合同编号',
    name             VARCHAR(128)   NOT NULL COMMENT '合同名称',
    customer_id      BIGINT         NOT NULL COMMENT '客户（scm_customer）',
    opportunity_id   BIGINT                  DEFAULT NULL COMMENT '商机',
    quotation_id     BIGINT                  DEFAULT NULL COMMENT '报价单',
    order_id         BIGINT                  DEFAULT NULL COMMENT '关联的销售订单（回款按该订单出库形成的应收单的核销金额计算）',
    sign_date        DATE           NOT NULL COMMENT '签订日期',
    start_date       DATE                    DEFAULT NULL COMMENT '开始日期',
    end_date         DATE                    DEFAULT NULL COMMENT '结束日期',
    amount           DECIMAL(18, 2) NOT NULL COMMENT '合同金额（含税）',
    status           VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态(字典 crm_contract_status) DRAFT草稿 APPROVING审批中 ACTIVE执行中 COMPLETED已完成 TERMINATED已终止',
    owner_id         BIGINT                  DEFAULT NULL COMMENT '负责人',
    dept_id          BIGINT                  DEFAULT NULL COMMENT '负责人部门（数据权限）',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '审批流程实例',
    approve_time     DATETIME                DEFAULT NULL COMMENT '生效时间',
    terminate_reason VARCHAR(255)            DEFAULT NULL COMMENT '终止原因',
    remark           VARCHAR(500)            DEFAULT NULL COMMENT '主要条款 / 备注',
    create_by        BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_crm_contract_no (contract_no, tenant_id),
    KEY idx_crm_contract_customer (customer_id),
    KEY idx_crm_contract_opportunity (opportunity_id),
    KEY idx_crm_contract_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售合同';

CREATE TABLE crm_collection_plan (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    contract_id BIGINT         NOT NULL COMMENT '合同',
    seq         INT            NOT NULL COMMENT '期次',
    plan_date   DATE           NOT NULL COMMENT '计划回款日期',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '计划回款金额',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '说明，如 预付款、验收款、质保金',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_collection_plan_contract (contract_id),
    KEY idx_collection_plan_date (plan_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合同回款计划：实际回款按计划日期先后分摊';

CREATE TABLE crm_opportunity_item (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    opportunity_id BIGINT         NOT NULL COMMENT '商机',
    material_id    BIGINT         NOT NULL COMMENT '产品',
    qty            DECIMAL(18, 4) NOT NULL COMMENT '数量',
    price          DECIMAL(18, 4) NOT NULL COMMENT '含税单价',
    tax_rate       DECIMAL(5, 2)  NOT NULL DEFAULT 13 COMMENT '税率（%）',
    amount         DECIMAL(18, 2) NOT NULL COMMENT '金额 = 数量 × 单价',
    create_time    DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_opp_item (opportunity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商机产品明细';

CREATE TABLE crm_stage_log (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    opportunity_id BIGINT      NOT NULL COMMENT '商机',
    from_stage     VARCHAR(16)          DEFAULT NULL COMMENT '原阶段，新建时为空',
    to_stage       VARCHAR(16) NOT NULL COMMENT '新阶段',
    user_id        BIGINT               DEFAULT NULL COMMENT '操作人',
    create_time    DATETIME             DEFAULT NULL COMMENT '变更时间',
    update_time    DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stage_log_opp (opportunity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商机阶段变更记录（转化率分析）';

CREATE TABLE crm_campaign (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name          VARCHAR(128)   NOT NULL COMMENT '活动名称',
    type          VARCHAR(16)    NOT NULL COMMENT '类型(字典 crm_campaign_type)',
    start_date    DATE           NOT NULL COMMENT '开始日期',
    end_date      DATE           NOT NULL COMMENT '结束日期',
    budget        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '预算',
    actual_cost   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '实际花费',
    target_leads  INT                     DEFAULT NULL COMMENT '目标线索数',
    status        VARCHAR(16)    NOT NULL COMMENT '状态(字典 crm_campaign_status) PLANNED计划中 ACTIVE进行中 FINISHED已结束 CANCELED已取消',
    owner_id      BIGINT                  DEFAULT NULL COMMENT '负责人',
    description   VARCHAR(1000)           DEFAULT NULL COMMENT '活动说明',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '市场活动';

CREATE TABLE crm_ticket (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id           BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ticket_no           VARCHAR(32)   NOT NULL COMMENT '工单号',
    customer_id         BIGINT        NOT NULL COMMENT '客户',
    contact_id          BIGINT                 DEFAULT NULL COMMENT '联系人',
    type                VARCHAR(16)   NOT NULL COMMENT '类型(字典 crm_ticket_type)',
    priority            VARCHAR(16)   NOT NULL COMMENT '优先级(字典 crm_ticket_priority) URGENT紧急 HIGH高 MEDIUM中 LOW低，决定处理时限',
    channel             VARCHAR(16)            DEFAULT NULL COMMENT '受理渠道(字典 crm_ticket_channel)',
    title               VARCHAR(128)  NOT NULL COMMENT '标题',
    description         VARCHAR(2000)          DEFAULT NULL COMMENT '问题描述',
    material_id         BIGINT                 DEFAULT NULL COMMENT '相关产品',
    serial_no           VARCHAR(64)            DEFAULT NULL COMMENT '产品序列号',
    assignee_id         BIGINT                 DEFAULT NULL COMMENT '处理人',
    status              VARCHAR(16)   NOT NULL COMMENT '状态(字典 crm_ticket_status) OPEN待处理 PROCESSING处理中 RESOLVED已解决 CLOSED已关闭',
    due_time            DATETIME      NOT NULL COMMENT '处理时限 = 受理时间 + 优先级时限',
    resolved_time       DATETIME               DEFAULT NULL COMMENT '解决时间',
    closed_time         DATETIME               DEFAULT NULL COMMENT '关闭时间',
    solution            VARCHAR(2000)          DEFAULT NULL COMMENT '解决方案',
    satisfaction        TINYINT                DEFAULT NULL COMMENT '客户满意度 1-5',
    fee_amount    DECIMAL(18, 2)          DEFAULT NULL COMMENT '服务收费金额（含税）',
    fee_bill_id   BIGINT                  DEFAULT NULL COMMENT '服务收费应收单',
    dept_id             BIGINT                 DEFAULT NULL COMMENT '受理人部门（数据权限）',
    create_by           BIGINT                 DEFAULT NULL COMMENT '受理人',
    create_time         DATETIME               DEFAULT NULL COMMENT '受理时间',
    update_time         DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no, tenant_id),
    KEY idx_ticket_customer (customer_id),
    KEY idx_ticket_status (status, due_time),
    KEY idx_ticket_assignee (assignee_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '服务工单';

CREATE TABLE crm_ticket_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ticket_id   BIGINT        NOT NULL COMMENT '工单',
    action      VARCHAR(16)   NOT NULL COMMENT '动作 CREATE受理 ASSIGN指派 PROCESS处理 RESOLVE解决 CLOSE关闭 REOPEN重新打开',
    content     VARCHAR(1000)          DEFAULT NULL COMMENT '内容',
    user_id     BIGINT                 DEFAULT NULL COMMENT '操作人',
    create_time DATETIME               DEFAULT NULL COMMENT '时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ticket_log (ticket_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单处理记录';

CREATE TABLE crm_ticket_part (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    ticket_id   BIGINT         NOT NULL COMMENT '服务工单',
    wms_doc_id  BIGINT         NOT NULL COMMENT '服务领用出库单',
    doc_no      VARCHAR(32)    NOT NULL COMMENT '出库单号',
    warehouse_id BIGINT        NOT NULL COMMENT '仓库',
    biz_date    DATE           NOT NULL COMMENT '领用日期',
    total_qty   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_by   BIGINT                  DEFAULT NULL COMMENT '领用人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ticket_part (ticket_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '服务工单备件领用';

-- ---------------------------------------------------------------- 项目管理

CREATE TABLE pm_project (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    code            VARCHAR(32)    NOT NULL COMMENT '项目编号',
    name            VARCHAR(128)   NOT NULL COMMENT '项目名称',
    type            VARCHAR(16)    NOT NULL COMMENT '类型(字典 pm_project_type)',
    customer_id     BIGINT                  DEFAULT NULL COMMENT '客户（对外项目）',
    opportunity_id  BIGINT                  DEFAULT NULL COMMENT '来源商机',
    sales_order_id  BIGINT                  DEFAULT NULL COMMENT '关联的销售订单',
    budget_control  TINYINT        NOT NULL DEFAULT 0 COMMENT '超预算控制 1是：费用、领料超过成本预算时不能录入',
    manager_id      BIGINT         NOT NULL COMMENT '项目经理',
    dept_id         BIGINT                  DEFAULT NULL COMMENT '负责部门（数据权限）',
    priority        VARCHAR(8)     NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级(字典 pm_priority)',
    status          VARCHAR(16)    NOT NULL COMMENT '状态(字典 pm_project_status) PLANNING规划中 APPROVING立项审批中 ACTIVE进行中 SUSPENDED已暂停 FINISHED已完成 CLOSED已关闭',
    plan_start      DATE           NOT NULL COMMENT '计划开始',
    plan_end        DATE           NOT NULL COMMENT '计划完成',
    actual_start    DATE                    DEFAULT NULL COMMENT '实际开始',
    actual_end      DATE                    DEFAULT NULL COMMENT '实际完成',
    contract_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '合同金额（含税）',
    tax_rate        DECIMAL(5, 2)  NOT NULL DEFAULT 6 COMMENT '税率（%）',
    budget_amount   DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '成本预算合计（各成本类型预算之和）',
    progress        DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '完成进度（%），按任务计划工时加权',
    description     VARCHAR(1000)           DEFAULT NULL COMMENT '项目说明',
    flow_instance_id VARCHAR(64)             DEFAULT NULL COMMENT '立项审批流程实例ID',
    create_by       BIGINT                  DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (code, tenant_id),
    KEY idx_project_status (status),
    KEY idx_project_manager (manager_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目';

CREATE TABLE pm_member (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id  BIGINT         NOT NULL COMMENT '项目',
    user_id     BIGINT         NOT NULL COMMENT '成员',
    role        VARCHAR(16)    NOT NULL COMMENT '角色(字典 pm_member_role)',
    allocation  INT            NOT NULL DEFAULT 100 COMMENT '投入比例（%）',
    cost_rate   DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '小时成本（元），审批工时时计入人工成本',
    rate_manual   TINYINT        NOT NULL DEFAULT 0 COMMENT '小时成本是否手工指定 1是 0否（按薪资档案折算，审批工时时取最新）',
    join_date   DATE                    DEFAULT NULL COMMENT '加入日期',
    leave_date  DATE                    DEFAULT NULL COMMENT '退出日期',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member (project_id, user_id, tenant_id),
    KEY idx_member_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目成员';

CREATE TABLE pm_task (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id     BIGINT        NOT NULL COMMENT '项目',
    parent_id      BIGINT        NOT NULL DEFAULT 0 COMMENT '上级任务，0 为顶级',
    name           VARCHAR(128)  NOT NULL COMMENT '任务名称',
    assignee_id    BIGINT                 DEFAULT NULL COMMENT '负责人',
    plan_start     DATE          NOT NULL COMMENT '计划开始',
    plan_end       DATE          NOT NULL COMMENT '计划完成',
    plan_hours     DECIMAL(8, 1) NOT NULL DEFAULT 0 COMMENT '计划工时，上级任务为下级合计',
    actual_hours   DECIMAL(8, 1) NOT NULL DEFAULT 0 COMMENT '实际工时（已审批的工时）',
    progress       INT           NOT NULL DEFAULT 0 COMMENT '进度（%），上级任务按下级计划工时加权',
    status         VARCHAR(16)   NOT NULL DEFAULT 'TODO' COMMENT '状态(字典 pm_task_status) TODO未开始 DOING进行中 DONE已完成',
    milestone      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否里程碑',
    predecessor_id BIGINT                 DEFAULT NULL COMMENT '前置任务（完成后才能开始）',
    actual_end     DATE                   DEFAULT NULL COMMENT '实际完成日期',
    sort           INT           NOT NULL DEFAULT 0 COMMENT '排序',
    description    VARCHAR(500)           DEFAULT NULL COMMENT '说明',
    create_time    DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time    DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_task_project (project_id, parent_id),
    KEY idx_task_assignee (assignee_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目任务（WBS）';

CREATE TABLE pm_timesheet (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id    BIGINT         NOT NULL COMMENT '项目',
    task_id       BIGINT                  DEFAULT NULL COMMENT '任务',
    user_id       BIGINT         NOT NULL COMMENT '填报人',
    work_date     DATE           NOT NULL COMMENT '工作日期',
    hours         DECIMAL(4, 1)  NOT NULL COMMENT '工时',
    content       VARCHAR(255)            DEFAULT NULL COMMENT '工作内容',
    status        VARCHAR(16)    NOT NULL COMMENT '状态(字典 pm_timesheet_status) SUBMITTED待审批 APPROVED已通过 REJECTED已驳回',
    cost_rate     DECIMAL(10, 2)          DEFAULT NULL COMMENT '审批时的小时成本',
    cost          DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '人工成本 = 工时 × 小时成本（审批通过后）',
    approve_by    BIGINT                  DEFAULT NULL COMMENT '审批人',
    approve_time  DATETIME                DEFAULT NULL COMMENT '审批时间',
    reject_reason VARCHAR(255)            DEFAULT NULL COMMENT '驳回原因',
    voucher_id    BIGINT                  DEFAULT NULL COMMENT '项目月结凭证（已入总账）',
    create_time   DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_timesheet_project (project_id, status),
    KEY idx_timesheet_user (user_id, work_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工时';

CREATE TABLE pm_budget (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id  BIGINT         NOT NULL COMMENT '项目',
    cost_type   VARCHAR(16)    NOT NULL COMMENT '成本类型(字典 pm_cost_type)',
    amount      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '预算金额',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm_budget (project_id, cost_type, tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目成本预算';

CREATE TABLE pm_cost (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id  BIGINT         NOT NULL COMMENT '项目',
    cost_type   VARCHAR(16)    NOT NULL COMMENT '成本类型(字典 pm_cost_type)，人工成本来自工时，不在此录入',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额（不含税）',
    cost_date   DATE           NOT NULL COMMENT '发生日期',
    description VARCHAR(255)            DEFAULT NULL COMMENT '说明',
    source      VARCHAR(16)    NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL手工 ISSUE项目领料',
    status      VARCHAR(16)    NOT NULL DEFAULT 'APPROVED' COMMENT '状态 APPROVED已生效 APPROVING审批中 REJECTED已拒绝',
    flow_instance_id VARCHAR(64)        DEFAULT NULL COMMENT '审批流程实例ID',
    voucher_id  BIGINT                  DEFAULT NULL COMMENT '项目月结凭证（已入总账）',
    source_no   VARCHAR(32)             DEFAULT NULL COMMENT '来源单号（领料生成的出库单号）',
    create_by   BIGINT                  DEFAULT NULL COMMENT '录入人',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_pm_cost (project_id, cost_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目费用';

CREATE TABLE pm_revenue (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id      BIGINT         NOT NULL COMMENT '项目',
    period          INT            NOT NULL COMMENT '期间 yyyyMM',
    completion_rate DECIMAL(5, 2)           DEFAULT NULL COMMENT '完工进度（%）',
    cum_revenue     DECIMAL(18, 2)          DEFAULT NULL COMMENT '累计应确认收入（不含税）',
    revenue         DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本次确认的收入',
    cost            DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本次结转的成本',
    labor_cost      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本次入账的人工成本',
    expense_cost    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '本次入账的费用',
    voucher_id      BIGINT                  DEFAULT NULL COMMENT '月结凭证',
    create_by       BIGINT                  DEFAULT NULL COMMENT '月结人',
    create_time     DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time     DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_revenue_project (project_id, period),
    KEY idx_revenue_voucher (voucher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目月结（收入确认、成本结转）';

CREATE TABLE pm_billing (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    project_id  BIGINT         NOT NULL COMMENT '项目',
    name        VARCHAR(64)    NOT NULL COMMENT '款项名称，如 预付款、验收款',
    task_id     BIGINT                  DEFAULT NULL COMMENT '关联里程碑',
    plan_date   DATE           NOT NULL COMMENT '计划开票日期',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '金额（含税）',
    status      VARCHAR(16)    NOT NULL COMMENT '状态(字典 pm_billing_status) PLANNED待开票 BILLED已开票',
    bill_id     BIGINT                  DEFAULT NULL COMMENT '生成的应收单',
    bill_no     VARCHAR(32)             DEFAULT NULL COMMENT '应收单号',
    bill_date   DATE                    DEFAULT NULL COMMENT '开票日期',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_billing_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目收款计划';

-- ---------------------------------------------------------------- 商务智能（数据仓库的事实表由同步任务全量重建）

CREATE TABLE bi_fact_sales (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_date    DATE           NOT NULL COMMENT '业务日期',
    period      INT            NOT NULL COMMENT '月份 yyyyMM',
    doc_no      VARCHAR(32)    NOT NULL COMMENT '出库 / 退货单号',
    customer_id BIGINT         NOT NULL COMMENT '客户',
    owner_id    BIGINT                  DEFAULT NULL COMMENT '客户负责人（CRM）',
    material_id BIGINT         NOT NULL COMMENT '物料',
    category    VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    warehouse_id BIGINT        NOT NULL COMMENT '仓库',
    dept_id     BIGINT                  DEFAULT NULL COMMENT '制单部门',
    qty         DECIMAL(18, 4) NOT NULL COMMENT '数量，退货为负',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '销售额（不含税），退货为负',
    cost        DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '销售成本 = 数量 × 存货单位成本',
    profit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '毛利',
    PRIMARY KEY (id),
    KEY idx_fact_sales_period (period),
    KEY idx_fact_sales_customer (customer_id),
    KEY idx_fact_sales_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '销售事实表';

CREATE TABLE bi_fact_purchase (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    biz_date     DATE           NOT NULL COMMENT '业务日期',
    period       INT            NOT NULL COMMENT '月份 yyyyMM',
    doc_no       VARCHAR(32)    NOT NULL COMMENT '入库 / 退货单号',
    supplier_id  BIGINT         NOT NULL COMMENT '供应商',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    category     VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '数量，退货为负',
    amount       DECIMAL(18, 2) NOT NULL COMMENT '采购额（不含税），退货为负',
    PRIMARY KEY (id),
    KEY idx_fact_purchase_period (period),
    KEY idx_fact_purchase_supplier (supplier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '采购事实表';

CREATE TABLE bi_fact_inventory (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    snap_date    DATE           NOT NULL COMMENT '快照日期（同步当天）',
    period       INT            NOT NULL COMMENT '月份 yyyyMM',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库',
    material_id  BIGINT         NOT NULL COMMENT '物料',
    category     VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    qty          DECIMAL(18, 4) NOT NULL COMMENT '库存数量',
    amount       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '库存金额 = 数量 × 存货单位成本',
    PRIMARY KEY (id),
    KEY idx_fact_inventory_date (snap_date),
    KEY idx_fact_inventory_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存快照事实表（每天一份，保留历史）';

CREATE TABLE bi_fact_production (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    wo_no        VARCHAR(32)    NOT NULL COMMENT '工单号',
    period       INT            NOT NULL COMMENT '月份（计划完工日期所在月）',
    material_id  BIGINT         NOT NULL COMMENT '产品',
    category     VARCHAR(64)             DEFAULT NULL COMMENT '物料分类',
    dept_id      BIGINT                  DEFAULT NULL COMMENT '制单部门',
    status       VARCHAR(16)    NOT NULL COMMENT '工单状态',
    plan_qty     DECIMAL(18, 4) NOT NULL COMMENT '计划数量',
    finished_qty DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '完工数量',
    scrap_qty    DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '报废数量',
    hours        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '报工工时',
    late         TINYINT        NOT NULL DEFAULT 0 COMMENT '是否延期（完工晚于计划完工日期，或未完工且已超期）',
    PRIMARY KEY (id),
    KEY idx_fact_production_period (period)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '生产事实表';

CREATE TABLE bi_fact_finance (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period     INT            NOT NULL COMMENT '会计期间',
    account_id BIGINT         NOT NULL COMMENT '科目（末级）',
    category   VARCHAR(16)    NOT NULL COMMENT '科目类别',
    dept_id    BIGINT                  DEFAULT NULL COMMENT '辅助核算部门',
    debit      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '借方发生额',
    credit     DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '贷方发生额',
    PRIMARY KEY (id),
    KEY idx_fact_finance_period (period, account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '财务事实表（已记账凭证按期间、科目、部门汇总，不含结转损益凭证）';

CREATE TABLE bi_fact_hr (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period     INT            NOT NULL COMMENT '月份 yyyyMM',
    dept_id    BIGINT                  DEFAULT NULL COMMENT '部门',
    category   VARCHAR(16)             DEFAULT NULL COMMENT '岗位类别',
    headcount  INT            NOT NULL DEFAULT 0 COMMENT '月末在职人数',
    hires      INT            NOT NULL DEFAULT 0 COMMENT '入职人数',
    leaves     INT            NOT NULL DEFAULT 0 COMMENT '离职人数',
    gross_pay  DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '应发工资',
    labor_cost DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '人工成本（应发 + 单位社保公积金）',
    PRIMARY KEY (id),
    KEY idx_fact_hr_period (period)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '人力事实表（按月份、部门、岗位类别）';

CREATE TABLE bi_fact_crm (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period     INT            NOT NULL COMMENT '月份 yyyyMM',
    owner_id   BIGINT                  DEFAULT NULL COMMENT '负责人',
    dept_id    BIGINT                  DEFAULT NULL COMMENT '负责人部门',
    leads      INT            NOT NULL DEFAULT 0 COMMENT '新增线索',
    followups  INT            NOT NULL DEFAULT 0 COMMENT '跟进次数',
    opps       INT            NOT NULL DEFAULT 0 COMMENT '新增商机',
    won        INT            NOT NULL DEFAULT 0 COMMENT '赢单',
    lost       INT            NOT NULL DEFAULT 0 COMMENT '输单',
    won_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '赢单金额',
    PRIMARY KEY (id),
    KEY idx_fact_crm_period (period)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户经营事实表（按月份、负责人）';

CREATE TABLE bi_fact_project (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    period     INT            NOT NULL COMMENT '月份 yyyyMM',
    project_id BIGINT         NOT NULL COMMENT '项目',
    dept_id    BIGINT                  DEFAULT NULL COMMENT '负责部门',
    manager_id BIGINT                  DEFAULT NULL COMMENT '项目经理',
    hours      DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '已审批工时',
    labor_cost DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '人工成本',
    expense    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '费用（含领料）',
    revenue    DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '月结确认的收入',
    cost       DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT '月结结转的成本',
    PRIMARY KEY (id),
    KEY idx_fact_project_period (period, project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目事实表（按月份、项目）';

CREATE TABLE bi_etl_log (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    trigger_type VARCHAR(16)   NOT NULL COMMENT '触发方式 MANUAL手动 SCHEDULE定时',
    start_time   DATETIME      NOT NULL COMMENT '开始时间',
    end_time     DATETIME               DEFAULT NULL COMMENT '结束时间',
    status       VARCHAR(16)   NOT NULL COMMENT '状态(字典 bi_etl_status) RUNNING运行中 SUCCESS成功 FAILED失败',
    row_counts   VARCHAR(500)           DEFAULT NULL COMMENT '各事实表行数，如 sales=120,purchase=80',
    message      VARCHAR(1000)          DEFAULT NULL COMMENT '失败原因',
    create_by    BIGINT                 DEFAULT NULL COMMENT '操作人，定时任务为空',
    create_time  DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time  DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据同步日志';

CREATE TABLE bi_report (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)   NOT NULL COMMENT '报表名称',
    dataset     VARCHAR(32)   NOT NULL COMMENT '数据集（分析主题，BiCube）',
    config      VARCHAR(4000) NOT NULL COMMENT '分析配置 JSON：行维度、列维度、指标、筛选、图表类型',
    shared      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否共享给所有人 1是 0仅自己',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_report_owner (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义报表';

CREATE TABLE bi_dashboard (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)   NOT NULL COMMENT '仪表盘名称',
    widgets     VARCHAR(4000) NOT NULL COMMENT '组件 JSON：[{reportId, span, chart}]',
    shared      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否共享给所有人',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '说明',
    create_by   BIGINT                 DEFAULT NULL COMMENT '创建人',
    create_time DATETIME               DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME               DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dashboard_owner (create_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义仪表盘';

CREATE TABLE bi_screen_share (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name             VARCHAR(64)  NOT NULL COMMENT '名称，如 一车间电视',
    token            CHAR(32)     NOT NULL COMMENT '访问令牌（128 位随机数），链接中使用',
    screens          VARCHAR(128) NOT NULL COMMENT '大屏（逗号分隔） FACTORY生产车间 WAREHOUSE仓储物流 SALES销售经营 QUALITY质量管理，多个时轮播',
    interval_seconds INT          NOT NULL DEFAULT 60 COMMENT '轮播间隔（秒）',
    work_center_id   BIGINT                DEFAULT NULL COMMENT '生产车间大屏只显示该工作中心，为空显示全部',
    expire_time      DATETIME              DEFAULT NULL COMMENT '有效期至，为空长期有效',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    last_access_time DATETIME              DEFAULT NULL COMMENT '最近访问时间',
    create_by        BIGINT                DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_screen_share_token (token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大屏投屏链接：车间、仓库的电视免登录查看指定的大屏（只读）';

CREATE TABLE bi_alert_rule (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    name        VARCHAR(64)    NOT NULL COMMENT '规则名称',
    metric      VARCHAR(32)    NOT NULL COMMENT '监控指标（BiAlertMetric）',
    operator    VARCHAR(4)     NOT NULL COMMENT '比较 GT大于 GE不小于 LT小于 LE不大于',
    threshold   DECIMAL(18, 4) NOT NULL COMMENT '阈值',
    level       VARCHAR(16)    NOT NULL COMMENT '级别(字典 bi_alert_level) INFO提示 WARN警告 DANGER严重',
    enabled     TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark      VARCHAR(255)            DEFAULT NULL COMMENT '处理建议',
    create_time DATETIME                DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME                DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预警规则';

INSERT INTO sys_tenant (id, name, admin_user_id, status, remark, create_time, update_time) VALUES
(1, '柠檬科技', 1, 1, '平台租户（运营方），不受套餐限制', NOW(), NOW());

INSERT INTO sys_dept (id, parent_id, name, sort, leader_user_id, status, create_time, update_time) VALUES
(1,   0,  '柠檬科技', 1, 1,    1, NOW(), NOW()),
(10,  1,  '研发部',   1, NULL, 1, NOW(), NOW()),
(11,  1,  '市场部',   2, NULL, 1, NOW(), NOW()),
(12,  1,  '财务部',   3, NULL, 1, NOW(), NOW()),
(100, 10, '前端组',   1, NULL, 1, NOW(), NOW()),
(101, 10, '后端组',   2, NULL, 1, NOW(), NOW());

INSERT INTO sys_user (id, dept_id, username, password, nickname, status, create_time, update_time) VALUES
(1, 1,  'admin', '$2a$10$1KjM59xYPcOlGbBaXNIRGOKMOubiCF1YaqU7b47kEXtfe159apzGW', '超级管理员', 1, NOW(), NOW()),
(2, 10, 'test',  '$2a$10$1KjM59xYPcOlGbBaXNIRGOKMOubiCF1YaqU7b47kEXtfe159apzGW', '测试用户', 1, NOW(), NOW());

INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(1, '超级管理员', 'admin', 1, 1, 1, '拥有全部权限', NOW(), NOW()),
(2, '普通用户', 'common', 2, 4, 1, '只读权限，只能查看本部门及以下的数据', NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1,   0, '系统管理', 'M', '/system', NULL, NULL, 'SettingOutlined', 10, 1, 1, NOW(), NOW()),
(100, 1, '用户管理', 'C', 'user', 'system/user/index', 'sys:user:list', 'UserOutlined', 1, 1, 1, NOW(), NOW()),
(104, 1, '部门管理', 'C', 'dept', 'system/dept/index', 'sys:dept:list', 'ApartmentOutlined', 2, 1, 1, NOW(), NOW()),
(101, 1, '角色管理', 'C', 'role', 'system/role/index', 'sys:role:list', 'TeamOutlined', 3, 1, 1, NOW(), NOW()),
(102, 1, '菜单管理', 'C', 'menu', 'system/menu/index', 'sys:menu:list', 'MenuOutlined', 4, 1, 1, NOW(), NOW()),
(103, 1, '字典管理', 'C', 'dict', 'system/dict/index', 'sys:dict:list', 'BookOutlined', 5, 1, 1, NOW(), NOW()),
(105, 1, '租户管理', 'C', 'tenant', 'system/tenant/index', 'sys:tenant:list', 'ShopOutlined', 6, 1, 1, NOW(), NOW()),
(106, 1, '租户套餐', 'C', 'tenant-package', 'system/tenant-package/index', 'sys:tenantPackage:list', 'GiftOutlined', 7, 1, 1, NOW(), NOW()),
(107, 1, '编码规则', 'C', 'code-rule', 'system/code-rule/index', 'sys:codeRule:list', 'NumberOutlined', 8, 1, 1, NOW(), NOW()),
(108, 1, '打印模板', 'C', 'print-template', 'system/print-template/index', 'sys:printTemplate:list', 'PrinterOutlined', 9, 1, 1, NOW(), NOW()),
(109, 1, '参数设置', 'C', 'config', 'system/config/index', 'sys:config:list', 'ControlOutlined', 10, 1, 1, NOW(), NOW()),
(1000, 100, '用户新增', 'F', NULL, NULL, 'sys:user:add', NULL, 1, 1, 1, NOW(), NOW()),
(1001, 100, '用户修改', 'F', NULL, NULL, 'sys:user:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1002, 100, '用户删除', 'F', NULL, NULL, 'sys:user:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1003, 100, '重置密码', 'F', NULL, NULL, 'sys:user:resetPwd', NULL, 4, 1, 1, NOW(), NOW()),
(1010, 101, '角色新增', 'F', NULL, NULL, 'sys:role:add', NULL, 1, 1, 1, NOW(), NOW()),
(1011, 101, '角色修改', 'F', NULL, NULL, 'sys:role:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1012, 101, '角色删除', 'F', NULL, NULL, 'sys:role:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1013, 101, '数据权限', 'F', NULL, NULL, 'sys:role:dataScope', NULL, 4, 1, 1, NOW(), NOW()),
(1014, 101, '分配用户', 'F', NULL, NULL, 'sys:role:assignUser', NULL, 5, 1, 1, NOW(), NOW()),
(1020, 102, '菜单新增', 'F', NULL, NULL, 'sys:menu:add', NULL, 1, 1, 1, NOW(), NOW()),
(1021, 102, '菜单修改', 'F', NULL, NULL, 'sys:menu:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1022, 102, '菜单删除', 'F', NULL, NULL, 'sys:menu:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1030, 103, '字典新增', 'F', NULL, NULL, 'sys:dict:add', NULL, 1, 1, 1, NOW(), NOW()),
(1031, 103, '字典修改', 'F', NULL, NULL, 'sys:dict:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1032, 103, '字典删除', 'F', NULL, NULL, 'sys:dict:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1040, 104, '部门新增', 'F', NULL, NULL, 'sys:dept:add', NULL, 1, 1, 1, NOW(), NOW()),
(1041, 104, '部门修改', 'F', NULL, NULL, 'sys:dept:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1042, 104, '部门删除', 'F', NULL, NULL, 'sys:dept:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1050, 105, '租户新增', 'F', NULL, NULL, 'sys:tenant:add', NULL, 1, 1, 1, NOW(), NOW()),
(1051, 105, '租户修改', 'F', NULL, NULL, 'sys:tenant:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1052, 105, '租户删除', 'F', NULL, NULL, 'sys:tenant:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1060, 106, '套餐新增', 'F', NULL, NULL, 'sys:tenantPackage:add', NULL, 1, 1, 1, NOW(), NOW()),
(1061, 106, '套餐修改', 'F', NULL, NULL, 'sys:tenantPackage:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1062, 106, '套餐删除', 'F', NULL, NULL, 'sys:tenantPackage:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1070, 107, '规则修改', 'F', NULL, NULL, 'sys:codeRule:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1080, 108, '模板修改', 'F', NULL, NULL, 'sys:printTemplate:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1090, 109, '参数修改', 'F', NULL, NULL, 'sys:config:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2,   0, '系统监控', 'M', '/monitor', NULL, NULL, 'FundProjectionScreenOutlined', 11, 1, 1, NOW(), NOW()),
(110, 2, '在线用户', 'C', 'online', 'monitor/online/index', 'monitor:online:list', 'UsergroupAddOutlined', 1, 1, 1, NOW(), NOW()),
(111, 2, '操作日志', 'C', 'operlog', 'monitor/operlog/index', 'monitor:operlog:list', 'FileTextOutlined', 2, 1, 1, NOW(), NOW()),
(112, 2, '登录日志', 'C', 'loginlog', 'monitor/loginlog/index', 'monitor:loginlog:list', 'LoginOutlined', 3, 1, 1, NOW(), NOW()),
(113, 2, '服务监控', 'C', 'server', 'monitor/server/index', 'monitor:server:list', 'CloudServerOutlined', 4, 1, 1, NOW(), NOW()),
(114, 2, '缓存监控', 'C', 'cache', 'monitor/cache/index', 'monitor:cache:list', 'DatabaseOutlined', 5, 1, 1, NOW(), NOW()),
(115, 2, '定时任务', 'C', 'job', 'monitor/job/index', 'monitor:job:list', 'FieldTimeOutlined', 6, 1, 1, NOW(), NOW()),
(1100, 110, '强制退出', 'F', NULL, NULL, 'monitor:online:kick', NULL, 1, 1, 1, NOW(), NOW()),
(1110, 111, '日志删除', 'F', NULL, NULL, 'monitor:operlog:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1120, 112, '日志删除', 'F', NULL, NULL, 'monitor:loginlog:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1140, 114, '清除缓存', 'F', NULL, NULL, 'monitor:cache:remove', NULL, 1, 1, 1, NOW(), NOW()),
(1150, 115, '任务修改', 'F', NULL, NULL, 'monitor:job:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1151, 115, '立即执行', 'F', NULL, NULL, 'monitor:job:run', NULL, 2, 1, 1, NOW(), NOW()),
(3,   0, '流程中心', 'M', '/workflow', NULL, NULL, 'NodeIndexOutlined', 9, 1, 1, NOW(), NOW()),
(120, 961, '发起申请', 'C', 'start', 'workflow/start/index', 'wf:start:list', 'SendOutlined', 2, 1, 1, NOW(), NOW()),
(121, 961, '我的待办', 'C', 'todo', 'workflow/todo/index', 'wf:task:todo', 'ScheduleOutlined', 1, 1, 1, NOW(), NOW()),
(122, 961, '我的已办', 'C', 'done', 'workflow/done/index', 'wf:task:done', 'CheckSquareOutlined', 4, 1, 1, NOW(), NOW()),
(123, 961, '我的申请', 'C', 'mine', 'workflow/mine/index', 'wf:instance:mine', 'SolutionOutlined', 3, 1, 1, NOW(), NOW()),
(124, 961, '抄送我的', 'C', 'copy', 'workflow/copy/index', 'wf:copy:list', 'CopyOutlined', 5, 1, 1, NOW(), NOW()),
(125, 962, '流程模型', 'C', 'model', 'workflow/model/index', 'wf:model:list', 'PartitionOutlined', 1, 1, 1, NOW(), NOW()),
(126, 962, '流程实例', 'C', 'instance', 'workflow/instance/index', 'wf:instance:list', 'DeploymentUnitOutlined', 2, 1, 1, NOW(), NOW()),
(1250, 125, '模型新增', 'F', NULL, NULL, 'wf:model:add', NULL, 1, 1, 1, NOW(), NOW()),
(1251, 125, '模型修改', 'F', NULL, NULL, 'wf:model:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1252, 125, '模型发布', 'F', NULL, NULL, 'wf:model:deploy', NULL, 3, 1, 1, NOW(), NOW()),
(1253, 125, '模型删除', 'F', NULL, NULL, 'wf:model:remove', NULL, 4, 1, 1, NOW(), NOW()),
(1260, 126, '终止流程', 'F', NULL, NULL, 'wf:instance:cancel', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(1, '通用状态', 'sys_common_status', 1, '启用/停用', NOW(), NOW()),
(2, '显示状态', 'sys_show_hide', 1, '菜单显示/隐藏', NOW(), NOW()),
(3, '菜单类型', 'sys_menu_type', 1, '目录/菜单/按钮', NOW(), NOW()),
(4, '操作类型', 'sys_oper_type', 1, '操作日志的操作类型', NOW(), NOW()),
(5, '成功失败', 'sys_success_fail', 1, '日志结果', NOW(), NOW()),
(6, '数据权限', 'sys_data_scope', 1, '角色的数据权限范围', NOW(), NOW()),
(7, '流程分类', 'wf_category', 1, '流程模型的分类', NOW(), NOW()),
(8, '流程状态', 'wf_status', 1, '流程实例的状态', NOW(), NOW()),
(9, '审批动作', 'wf_task_action', 1, '审批记录中的处理结果', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_common_status', '启用', '1', 'green', 1, 1, NOW(), NOW()),
('sys_common_status', '停用', '0', 'red', 2, 1, NOW(), NOW()),
('sys_show_hide', '显示', '1', 'blue', 1, 1, NOW(), NOW()),
('sys_show_hide', '隐藏', '0', 'default', 2, 1, NOW(), NOW()),
('sys_menu_type', '目录', 'M', 'blue', 1, 1, NOW(), NOW()),
('sys_menu_type', '菜单', 'C', 'green', 2, 1, NOW(), NOW()),
('sys_menu_type', '按钮', 'F', 'orange', 3, 1, NOW(), NOW()),
('sys_oper_type', '其他', '0', 'default', 0, 1, NOW(), NOW()),
('sys_oper_type', '新增', '1', 'green', 1, 1, NOW(), NOW()),
('sys_oper_type', '修改', '2', 'blue', 2, 1, NOW(), NOW()),
('sys_oper_type', '删除', '3', 'red', 3, 1, NOW(), NOW()),
('sys_oper_type', '授权', '4', 'purple', 4, 1, NOW(), NOW()),
('sys_oper_type', '导出', '5', 'cyan', 5, 1, NOW(), NOW()),
('sys_oper_type', '强退', '6', 'orange', 6, 1, NOW(), NOW()),
('sys_oper_type', '清空', '7', 'red', 7, 1, NOW(), NOW()),
('sys_oper_type', '导入', '8', 'geekblue', 8, 1, NOW(), NOW()),
('sys_success_fail', '成功', '1', 'green', 1, 1, NOW(), NOW()),
('sys_success_fail', '失败', '0', 'red', 2, 1, NOW(), NOW()),
('sys_data_scope', '全部数据', '1', 'red', 1, 1, NOW(), NOW()),
('sys_data_scope', '自定义数据', '2', 'purple', 2, 1, NOW(), NOW()),
('sys_data_scope', '本部门数据', '3', 'blue', 3, 1, NOW(), NOW()),
('sys_data_scope', '本部门及以下数据', '4', 'cyan', 4, 1, NOW(), NOW()),
('sys_data_scope', '仅本人数据', '5', 'default', 5, 1, NOW(), NOW()),
('wf_category', '采购', 'PURCHASE', 'blue', 1, 1, NOW(), NOW()),
('wf_category', '销售', 'SALES', 'green', 2, 1, NOW(), NOW()),
('wf_category', '库存', 'INVENTORY', 'cyan', 3, 1, NOW(), NOW()),
('wf_category', '财务', 'FINANCE', 'gold', 4, 1, NOW(), NOW()),
('wf_category', '其他', 'OTHER', 'default', 5, 1, NOW(), NOW()),
('wf_status', '审批中', 'RUNNING', 'processing', 1, 1, NOW(), NOW()),
('wf_status', '已通过', 'APPROVED', 'green', 2, 1, NOW(), NOW()),
('wf_status', '已拒绝', 'REJECTED', 'red', 3, 1, NOW(), NOW()),
('wf_status', '已撤销', 'CANCELED', 'default', 4, 1, NOW(), NOW()),
('wf_task_action', '同意', 'APPROVE', 'green', 1, 1, NOW(), NOW()),
('wf_task_action', '拒绝', 'REJECT', 'red', 2, 1, NOW(), NOW()),
('wf_task_action', '转交', 'TRANSFER', 'blue', 3, 1, NOW(), NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- 普通用户：只能查看各管理页面，没有任何按钮权限；可以使用流程中心发起、审批自己的流程
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 100), (2, 104), (2, 101), (2, 102), (2, 103), (2, 109),
(2, 2), (2, 110), (2, 111), (2, 112), (2, 113), (2, 114), (2, 115),
(2, 3), (2, 120), (2, 121), (2, 122), (2, 123), (2, 124), (2, 125), (2, 126);

-- ---------------------------------------------------------------- 仓库管理：菜单、字典、示例数据

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(4,   0, '仓库管理', 'M', '/wms', NULL, NULL, 'ShopOutlined', 2, 1, 1, NOW(), NOW()),
(130, 911, '入库单', 'C', 'in', 'wms/in/index', 'wms:in:list', 'ImportOutlined', 1, 1, 1, NOW(), NOW()),
(131, 911, '出库单', 'C', 'out', 'wms/out/index', 'wms:out:list', 'ExportOutlined', 2, 1, 1, NOW(), NOW()),
(132, 911, '调拨单', 'C', 'transfer', 'wms/transfer/index', 'wms:transfer:list', 'SwapOutlined', 3, 1, 1, NOW(), NOW()),
(133, 911, '盘点单', 'C', 'check', 'wms/check/index', 'wms:check:list', 'AuditOutlined', 4, 1, 1, NOW(), NOW()),
(134, 912, '库存查询', 'C', 'stock', 'wms/stock/index', 'wms:stock:list', 'DatabaseOutlined', 1, 1, 1, NOW(), NOW()),
(135, 912, '库存流水', 'C', 'flow', 'wms/flow/index', 'wms:flow:list', 'HistoryOutlined', 2, 1, 1, NOW(), NOW()),
(136, 912, '批次与序列号', 'C', 'trace', 'wms/trace/index', 'wms:trace:list', 'BarcodeOutlined', 3, 1, 1, NOW(), NOW()),
(137, 914, '物料管理', 'C', 'material', 'wms/material/index', 'wms:material:list', 'AppstoreOutlined', 1, 1, 1, NOW(), NOW()),
(138, 914, '仓库管理', 'C', 'warehouse', 'wms/warehouse/index', 'wms:warehouse:list', 'HomeOutlined', 2, 1, 1, NOW(), NOW()),
(139, 914, '库位管理', 'C', 'location', 'wms/location/index', 'wms:location:list', 'EnvironmentOutlined', 3, 1, 1, NOW(), NOW()),
(266, 913, '库存冻结', 'C', 'hold', 'wms/hold/index', 'wms:hold:list', 'LockOutlined', 2, 1, 1, NOW(), NOW()),
(2660, 266, '冻结放行', 'F', NULL, NULL, 'wms:hold:release', NULL, 1, 1, 1, NOW(), NOW()),
(1300, 130, '入库单新增', 'F', NULL, NULL, 'wms:in:add', NULL, 1, 1, 1, NOW(), NOW()),
(1301, 130, '入库单修改', 'F', NULL, NULL, 'wms:in:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1302, 130, '入库单删除', 'F', NULL, NULL, 'wms:in:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1303, 130, '入库确认', 'F', NULL, NULL, 'wms:in:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1310, 131, '出库单新增', 'F', NULL, NULL, 'wms:out:add', NULL, 1, 1, 1, NOW(), NOW()),
(1311, 131, '出库单修改', 'F', NULL, NULL, 'wms:out:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1312, 131, '出库单删除', 'F', NULL, NULL, 'wms:out:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1313, 131, '出库确认', 'F', NULL, NULL, 'wms:out:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1320, 132, '调拨单新增', 'F', NULL, NULL, 'wms:transfer:add', NULL, 1, 1, 1, NOW(), NOW()),
(1321, 132, '调拨单修改', 'F', NULL, NULL, 'wms:transfer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1322, 132, '调拨单删除', 'F', NULL, NULL, 'wms:transfer:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1323, 132, '调拨确认', 'F', NULL, NULL, 'wms:transfer:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1330, 133, '盘点单新增', 'F', NULL, NULL, 'wms:check:add', NULL, 1, 1, 1, NOW(), NOW()),
(1331, 133, '盘点单修改', 'F', NULL, NULL, 'wms:check:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1332, 133, '盘点单删除', 'F', NULL, NULL, 'wms:check:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1333, 133, '盘点确认', 'F', NULL, NULL, 'wms:check:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1370, 137, '物料新增', 'F', NULL, NULL, 'wms:material:add', NULL, 1, 1, 1, NOW(), NOW()),
(1371, 137, '物料修改', 'F', NULL, NULL, 'wms:material:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1372, 137, '物料删除', 'F', NULL, NULL, 'wms:material:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1380, 138, '仓库新增', 'F', NULL, NULL, 'wms:warehouse:add', NULL, 1, 1, 1, NOW(), NOW()),
(1381, 138, '仓库修改', 'F', NULL, NULL, 'wms:warehouse:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1382, 138, '仓库删除', 'F', NULL, NULL, 'wms:warehouse:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1390, 139, '库位新增', 'F', NULL, NULL, 'wms:location:add', NULL, 1, 1, 1, NOW(), NOW()),
(1391, 139, '库位修改', 'F', NULL, NULL, 'wms:location:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1392, 139, '库位删除', 'F', NULL, NULL, 'wms:location:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(10, '物料分类', 'wms_material_category', 1, '物料的分类', NOW(), NOW()),
(11, '入库类型', 'wms_in_type', 1, '入库单的业务类型', NOW(), NOW()),
(12, '出库类型', 'wms_out_type', 1, '出库单的业务类型', NOW(), NOW()),
(13, '单据状态', 'wms_doc_status', 1, '仓库单据的状态', NOW(), NOW()),
(14, '库存业务类型', 'wms_biz_type', 1, '库存流水的业务类型', NOW(), NOW()),
(15, '序列号状态', 'wms_serial_status', 1, '序列号是否在库', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_material_category', '原材料', 'RAW', 'blue', 1, 1, NOW(), NOW()),
('wms_material_category', '半成品', 'SEMI', 'cyan', 2, 1, NOW(), NOW()),
('wms_material_category', '成品', 'FINISHED', 'green', 3, 1, NOW(), NOW()),
('wms_material_category', '包装材料', 'PACKING', 'gold', 4, 1, NOW(), NOW()),
('wms_material_category', '其他', 'OTHER', 'default', 5, 1, NOW(), NOW()),
('wms_in_type', '采购入库', 'PURCHASE', 'blue', 1, 1, NOW(), NOW()),
('wms_in_type', '生产入库', 'PRODUCTION', 'cyan', 2, 1, NOW(), NOW()),
('wms_in_type', '销售退货', 'SALES_RETURN', 'orange', 3, 1, NOW(), NOW()),
('wms_in_type', '其他入库', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('wms_out_type', '销售出库', 'SALES', 'green', 1, 1, NOW(), NOW()),
('wms_out_type', '生产领料', 'PRODUCTION', 'cyan', 2, 1, NOW(), NOW()),
('wms_out_type', '采购退货', 'PURCHASE_RETURN', 'orange', 3, 1, NOW(), NOW()),
('wms_out_type', '其他出库', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('wms_doc_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('wms_doc_status', '已完成', 'DONE', 'green', 2, 1, NOW(), NOW()),
('wms_biz_type', '入库', 'IN', 'green', 1, 1, NOW(), NOW()),
('wms_biz_type', '出库', 'OUT', 'orange', 2, 1, NOW(), NOW()),
('wms_biz_type', '调拨', 'TRANSFER', 'blue', 3, 1, NOW(), NOW()),
('wms_biz_type', '盘点', 'CHECK', 'purple', 4, 1, NOW(), NOW()),
('wms_serial_status', '在库', 'IN_STOCK', 'green', 1, 1, NOW(), NOW()),
('wms_serial_status', '已出库', 'OUT', 'default', 2, 1, NOW(), NOW());

-- 普通用户：仓库管理各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 4), (2, 130), (2, 131), (2, 132), (2, 133), (2, 134), (2, 135), (2, 136), (2, 137), (2, 138), (2, 139), (2, 266);

-- 示例基础资料（没有库存，库存只能通过单据产生）
INSERT INTO wms_warehouse (id, code, name, address, contact, sort, status, create_time, update_time) VALUES
(1, 'WH01', '总部成品仓', '上海市浦东新区 1 号库', '张三', 1, 1, NOW(), NOW()),
(2, 'WH02', '原材料仓', '上海市浦东新区 2 号库', '李四', 2, 1, NOW(), NOW());

INSERT INTO wms_location (warehouse_id, code, name, sort, status, create_time, update_time) VALUES
(1, 'A-01', 'A 区 1 号货架', 1, 1, NOW(), NOW()),
(1, 'A-02', 'A 区 2 号货架', 2, 1, NOW(), NOW()),
(2, 'R-01', '原料区 1 排', 1, 1, NOW(), NOW());

INSERT INTO wms_material (code, name, spec, unit, category, batch_enabled, serial_enabled, safety_stock, status, create_time, update_time) VALUES
('M0001', '柠檬原浆', '5L/桶', '桶', 'RAW', 1, 0, 20, 1, NOW(), NOW()),
('M0002', '包装纸箱', '40×30×20cm', '个', 'PACKING', 0, 0, 100, 1, NOW(), NOW()),
('P0001', '智能榨汁机', 'LJ-200', '台', 'FINISHED', 0, 1, 5, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 供应链管理：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(5,   0, '供应链管理', 'M', '/scm', NULL, NULL, 'DeploymentUnitOutlined', 1, 1, 1, NOW(), NOW()),
(140, 901, '采购订单', 'C', 'purchase', 'scm/purchase/index', 'scm:purchase:list', 'ShoppingCartOutlined', 3, 1, 1, NOW(), NOW()),
(141, 901, '采购入库', 'C', 'purchase-in', 'scm/purchaseIn/index', 'scm:purchaseIn:list', 'ImportOutlined', 4, 1, 1, NOW(), NOW()),
(142, 901, '采购退货', 'C', 'purchase-return', 'scm/purchaseReturn/index', 'scm:purchaseReturn:list', 'RollbackOutlined', 5, 1, 1, NOW(), NOW()),
(143, 902, '销售订单', 'C', 'sales', 'scm/sales/index', 'scm:sales:list', 'ShoppingOutlined', 3, 1, 1, NOW(), NOW()),
(144, 902, '销售出库', 'C', 'sales-out', 'scm/salesOut/index', 'scm:salesOut:list', 'ExportOutlined', 5, 1, 1, NOW(), NOW()),
(145, 902, '销售退货', 'C', 'sales-return', 'scm/salesReturn/index', 'scm:salesReturn:list', 'UndoOutlined', 6, 1, 1, NOW(), NOW()),
(146, 902, '物流发运', 'C', 'shipment', 'scm/shipment/index', 'scm:shipment:list', 'CarOutlined', 7, 1, 1, NOW(), NOW()),
(147, 903, '供应商', 'C', 'supplier', 'scm/supplier/index', 'scm:supplier:list', 'BankOutlined', 1, 1, 1, NOW(), NOW()),
(148, 903, '供应商评估', 'C', 'supplier-eval', 'scm/eval/index', 'scm:eval:list', 'StarOutlined', 2, 1, 1, NOW(), NOW()),
(149, 903, '客户', 'C', 'customer', 'scm/customer/index', 'scm:customer:list', 'ContactsOutlined', 3, 1, 1, NOW(), NOW()),
(1400, 140, '采购订单新增', 'F', NULL, NULL, 'scm:purchase:add', NULL, 1, 1, 1, NOW(), NOW()),
(1401, 140, '采购订单修改', 'F', NULL, NULL, 'scm:purchase:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1402, 140, '采购订单删除', 'F', NULL, NULL, 'scm:purchase:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1403, 140, '采购订单审核', 'F', NULL, NULL, 'scm:purchase:approve', NULL, 4, 1, 1, NOW(), NOW()),
(1404, 140, '采购订单关闭', 'F', NULL, NULL, 'scm:purchase:close', NULL, 5, 1, 1, NOW(), NOW()),
(1410, 141, '采购入库', 'F', NULL, NULL, 'scm:purchaseIn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1420, 142, '采购退货', 'F', NULL, NULL, 'scm:purchaseReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1430, 143, '销售订单新增', 'F', NULL, NULL, 'scm:sales:add', NULL, 1, 1, 1, NOW(), NOW()),
(1431, 143, '销售订单修改', 'F', NULL, NULL, 'scm:sales:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1432, 143, '销售订单删除', 'F', NULL, NULL, 'scm:sales:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1433, 143, '销售订单审核', 'F', NULL, NULL, 'scm:sales:approve', NULL, 4, 1, 1, NOW(), NOW()),
(1434, 143, '销售订单关闭', 'F', NULL, NULL, 'scm:sales:close', NULL, 5, 1, 1, NOW(), NOW()),
(1440, 144, '销售出库', 'F', NULL, NULL, 'scm:salesOut:add', NULL, 1, 1, 1, NOW(), NOW()),
(1450, 145, '销售退货', 'F', NULL, NULL, 'scm:salesReturn:add', NULL, 1, 1, 1, NOW(), NOW()),
(1460, 146, '发运单新增', 'F', NULL, NULL, 'scm:shipment:add', NULL, 1, 1, 1, NOW(), NOW()),
(1461, 146, '发运单修改', 'F', NULL, NULL, 'scm:shipment:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1462, 146, '发运单删除', 'F', NULL, NULL, 'scm:shipment:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1463, 146, '发货与跟踪', 'F', NULL, NULL, 'scm:shipment:track', NULL, 4, 1, 1, NOW(), NOW()),
(1470, 147, '供应商新增', 'F', NULL, NULL, 'scm:supplier:add', NULL, 1, 1, 1, NOW(), NOW()),
(1471, 147, '供应商修改', 'F', NULL, NULL, 'scm:supplier:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1472, 147, '供应商删除', 'F', NULL, NULL, 'scm:supplier:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1480, 148, '评估新增', 'F', NULL, NULL, 'scm:eval:add', NULL, 1, 1, 1, NOW(), NOW()),
(1481, 148, '评估删除', 'F', NULL, NULL, 'scm:eval:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1490, 149, '客户新增', 'F', NULL, NULL, 'scm:customer:add', NULL, 1, 1, 1, NOW(), NOW()),
(1491, 149, '客户修改', 'F', NULL, NULL, 'scm:customer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1492, 149, '客户删除', 'F', NULL, NULL, 'scm:customer:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(16, '供应商等级', 'scm_supplier_level', 1, '供应商评估的评定等级', NOW(), NOW()),
(17, '客户等级', 'scm_customer_level', 1, '客户分级', NOW(), NOW()),
(18, '订单状态', 'scm_order_status', 1, '采购、销售订单的状态', NOW(), NOW()),
(19, '出入库种类', 'scm_trade_kind', 1, '采购入库 / 退货、销售出库 / 退货', NOW(), NOW()),
(20, '发运状态', 'scm_shipment_status', 1, '物流发运单的状态', NOW(), NOW()),
(21, '承运商', 'scm_carrier', 1, '物流公司', NOW(), NOW()),
(22, '订单来源', 'scm_order_source', 1, '手工录入或 MRP 生成', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_supplier_level', 'A 优秀', 'A', 'green', 1, 1, NOW(), NOW()),
('scm_supplier_level', 'B 良好', 'B', 'blue', 2, 1, NOW(), NOW()),
('scm_supplier_level', 'C 合格', 'C', 'orange', 3, 1, NOW(), NOW()),
('scm_supplier_level', 'D 待改进', 'D', 'red', 4, 1, NOW(), NOW()),
('scm_customer_level', '重点客户', 'VIP', 'gold', 1, 1, NOW(), NOW()),
('scm_customer_level', '普通客户', 'NORMAL', 'blue', 2, 1, NOW(), NOW()),
('scm_customer_level', '潜在客户', 'POTENTIAL', 'default', 3, 1, NOW(), NOW()),
('scm_order_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('scm_order_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_order_status', '已审核', 'APPROVED', 'blue', 3, 1, NOW(), NOW()),
('scm_order_status', '部分完成', 'PARTIAL', 'cyan', 4, 1, NOW(), NOW()),
('scm_order_status', '已完成', 'COMPLETED', 'green', 5, 1, NOW(), NOW()),
('scm_order_status', '已关闭', 'CLOSED', 'default', 6, 1, NOW(), NOW()),
('scm_trade_kind', '采购入库', 'PURCHASE_IN', 'blue', 1, 1, NOW(), NOW()),
('scm_trade_kind', '采购退货', 'PURCHASE_RETURN', 'orange', 2, 1, NOW(), NOW()),
('scm_trade_kind', '销售出库', 'SALES_OUT', 'green', 3, 1, NOW(), NOW()),
('scm_trade_kind', '销售退货', 'SALES_RETURN', 'red', 4, 1, NOW(), NOW()),
('scm_shipment_status', '待发运', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('scm_shipment_status', '运输中', 'SHIPPED', 'processing', 2, 1, NOW(), NOW()),
('scm_shipment_status', '已签收', 'SIGNED', 'green', 3, 1, NOW(), NOW()),
('scm_shipment_status', '异常', 'EXCEPTION', 'red', 4, 1, NOW(), NOW()),
('scm_carrier', '顺丰速运', 'SF', 'default', 1, 1, NOW(), NOW()),
('scm_carrier', '京东物流', 'JD', 'default', 2, 1, NOW(), NOW()),
('scm_carrier', '中通快递', 'ZTO', 'default', 3, 1, NOW(), NOW()),
('scm_carrier', '德邦物流', 'DEPPON', 'default', 4, 1, NOW(), NOW()),
('scm_carrier', '自提 / 自送', 'SELF', 'default', 5, 1, NOW(), NOW()),
('scm_order_source', '手工', 'MANUAL', 'default', 1, 1, NOW(), NOW()),
('scm_order_source', 'MRP', 'MRP', 'purple', 2, 1, NOW(), NOW());

-- 普通用户：供应链各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 5), (2, 140), (2, 141), (2, 142), (2, 143), (2, 144), (2, 145), (2, 146), (2, 147), (2, 148), (2, 149);

-- 示例往来单位
INSERT INTO scm_supplier (id, code, name, level, contact, phone, address, payment_days, status, create_time, update_time) VALUES
(1, 'S001', '上海鲜果供应链有限公司', 'A', '王经理', '021-55550001', '上海市闵行区', 30, 1, NOW(), NOW()),
(2, 'S002', '华东包装材料厂', 'B', '赵经理', '0571-55550002', '浙江省杭州市', 60, 1, NOW(), NOW());

INSERT INTO scm_customer (id, code, name, level, contact, phone, address, credit_limit, payment_days, status, create_time, update_time) VALUES
(1, 'C001', '鲜饮连锁（上海）有限公司', 'VIP', '陈店长', '021-66660001', '上海市黄浦区南京东路 100 号', 500000, 30, 1, NOW(), NOW()),
(2, 'C002', '优品商贸有限公司', 'NORMAL', '孙经理', '010-66660002', '北京市朝阳区建国路 88 号', 100000, 15, 1, NOW(), NOW());

INSERT INTO scm_supplier_price (supplier_id, material_id, price, tax_rate, lead_days, min_qty, is_default, create_time, update_time) VALUES
(1, 1, 120, 13, 3, 10, 1, NOW(), NOW()),
(2, 2, 2.5, 13, 5, 500, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 生产制造：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(6,   0, '生产制造', 'M', '/mes', NULL, NULL, 'ToolOutlined', 3, 1, 1, NOW(), NOW()),
(160, 921, '生产计划', 'C', 'plan', 'mes/plan/index', 'mes:plan:list', 'CalendarOutlined', 1, 1, 1, NOW(), NOW()),
(161, 921, 'MRP 运算', 'C', 'mrp', 'mes/mrp/index', 'mes:mrp:list', 'CalculatorOutlined', 2, 1, 1, NOW(), NOW()),
(162, 922, '生产工单', 'C', 'wo', 'mes/wo/index', 'mes:wo:list', 'ProfileOutlined', 1, 1, 1, NOW(), NOW()),
(163, 922, '质量检验', 'C', 'inspection', 'mes/inspection/index', 'mes:qc:list', 'SafetyCertificateOutlined', 4, 1, 1, NOW(), NOW()),
(164, 923, '设备台账', 'C', 'equipment', 'mes/equipment/index', 'mes:equipment:list', 'ControlOutlined', 3, 1, 1, NOW(), NOW()),
(165, 923, '设备维护', 'C', 'maintenance', 'mes/maintenance/index', 'mes:maintain:list', 'ToolOutlined', 4, 1, 1, NOW(), NOW()),
(166, 923, '物料清单', 'C', 'bom', 'mes/bom/index', 'mes:bom:list', 'ApartmentOutlined', 1, 1, 1, NOW(), NOW()),
(167, 923, '工作中心', 'C', 'work-center', 'mes/workCenter/index', 'mes:wc:list', 'ClusterOutlined', 2, 1, 1, NOW(), NOW()),
(1600, 160, '计划新增', 'F', NULL, NULL, 'mes:plan:add', NULL, 1, 1, 1, NOW(), NOW()),
(1601, 160, '计划修改', 'F', NULL, NULL, 'mes:plan:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1602, 160, '计划删除', 'F', NULL, NULL, 'mes:plan:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1603, 160, '计划确认', 'F', NULL, NULL, 'mes:plan:confirm', NULL, 4, 1, 1, NOW(), NOW()),
(1610, 161, 'MRP 运算', 'F', NULL, NULL, 'mes:mrp:run', NULL, 1, 1, 1, NOW(), NOW()),
(1611, 161, 'MRP 下达', 'F', NULL, NULL, 'mes:mrp:release', NULL, 2, 1, 1, NOW(), NOW()),
(1620, 162, '工单新增', 'F', NULL, NULL, 'mes:wo:add', NULL, 1, 1, 1, NOW(), NOW()),
(1621, 162, '工单修改', 'F', NULL, NULL, 'mes:wo:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1622, 162, '工单删除', 'F', NULL, NULL, 'mes:wo:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1623, 162, '下达与关闭', 'F', NULL, NULL, 'mes:wo:release', NULL, 4, 1, 1, NOW(), NOW()),
(1624, 162, '领料退料', 'F', NULL, NULL, 'mes:wo:issue', NULL, 5, 1, 1, NOW(), NOW()),
(1625, 162, '报工', 'F', NULL, NULL, 'mes:wo:report', NULL, 6, 1, 1, NOW(), NOW()),
(1626, 162, '完工入库', 'F', NULL, NULL, 'mes:wo:finish', NULL, 7, 1, 1, NOW(), NOW()),
(1630, 163, '检验新增', 'F', NULL, NULL, 'mes:qc:add', NULL, 1, 1, 1, NOW(), NOW()),
(1631, 163, '检验修改', 'F', NULL, NULL, 'mes:qc:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1632, 163, '检验删除', 'F', NULL, NULL, 'mes:qc:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1640, 164, '设备新增', 'F', NULL, NULL, 'mes:equipment:add', NULL, 1, 1, 1, NOW(), NOW()),
(1641, 164, '设备修改', 'F', NULL, NULL, 'mes:equipment:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1642, 164, '设备删除', 'F', NULL, NULL, 'mes:equipment:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1650, 165, '维护单新增', 'F', NULL, NULL, 'mes:maintain:add', NULL, 1, 1, 1, NOW(), NOW()),
(1651, 165, '维护单删除', 'F', NULL, NULL, 'mes:maintain:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1652, 165, '维护处理', 'F', NULL, NULL, 'mes:maintain:handle', NULL, 3, 1, 1, NOW(), NOW()),
(1660, 166, 'BOM 新增', 'F', NULL, NULL, 'mes:bom:add', NULL, 1, 1, 1, NOW(), NOW()),
(1661, 166, 'BOM 修改', 'F', NULL, NULL, 'mes:bom:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1662, 166, 'BOM 删除', 'F', NULL, NULL, 'mes:bom:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1670, 167, '工作中心新增', 'F', NULL, NULL, 'mes:wc:add', NULL, 1, 1, 1, NOW(), NOW()),
(1671, 167, '工作中心修改', 'F', NULL, NULL, 'mes:wc:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1672, 167, '工作中心删除', 'F', NULL, NULL, 'mes:wc:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(30, '生产计划状态', 'mes_plan_status', 1, '主生产计划的状态', NOW(), NOW()),
(31, '工单状态', 'mes_wo_status', 1, '生产工单的状态', NOW(), NOW()),
(32, '供应方式', 'mes_supply_type', 1, 'MRP 的自制 / 采购', NOW(), NOW()),
(33, '工单单据', 'mes_wo_doc_kind', 1, '领料 / 退料 / 完工入库', NOW(), NOW()),
(34, '检验类型', 'mes_qc_type', 1, '质量检验的类型', NOW(), NOW()),
(35, '检验结论', 'mes_qc_result', 1, '质量检验的结论', NOW(), NOW()),
(36, '不良类型', 'mes_defect_type', 1, '质量检验的主要不良类型', NOW(), NOW()),
(37, '设备状态', 'mes_equipment_status', 1, '设备台账的状态', NOW(), NOW()),
(38, '维护类型', 'mes_maintain_type', 1, '故障维修 / 定期保养', NOW(), NOW()),
(39, '维护状态', 'mes_maintain_status', 1, '设备维护单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_type', '生产退料', 'PRODUCTION_RETURN', 'purple', 5, 1, NOW(), NOW()),
('mes_plan_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('mes_plan_status', '已确认', 'CONFIRMED', 'blue', 2, 1, NOW(), NOW()),
('mes_plan_status', '已关闭', 'CLOSED', 'default', 3, 1, NOW(), NOW()),
('mes_wo_status', '草稿', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('mes_wo_status', '已下达', 'RELEASED', 'blue', 2, 1, NOW(), NOW()),
('mes_wo_status', '生产中', 'STARTED', 'processing', 3, 1, NOW(), NOW()),
('mes_wo_status', '已完工', 'COMPLETED', 'green', 4, 1, NOW(), NOW()),
('mes_wo_status', '已关闭', 'CLOSED', 'default', 5, 1, NOW(), NOW()),
('mes_supply_type', '自制', 'MAKE', 'cyan', 1, 1, NOW(), NOW()),
('mes_supply_type', '采购', 'BUY', 'blue', 2, 1, NOW(), NOW()),
('mes_wo_doc_kind', '领料', 'ISSUE', 'orange', 1, 1, NOW(), NOW()),
('mes_wo_doc_kind', '退料', 'RETURN', 'purple', 2, 1, NOW(), NOW()),
('mes_wo_doc_kind', '完工入库', 'FINISH', 'green', 3, 1, NOW(), NOW()),
('mes_qc_type', '来料检验', 'IQC', 'blue', 1, 1, NOW(), NOW()),
('mes_qc_type', '过程检验', 'IPQC', 'cyan', 2, 1, NOW(), NOW()),
('mes_qc_type', '成品检验', 'FQC', 'green', 3, 1, NOW(), NOW()),
('mes_qc_result', '合格', 'PASS', 'green', 1, 1, NOW(), NOW()),
('mes_qc_result', '不合格', 'FAIL', 'red', 2, 1, NOW(), NOW()),
('mes_qc_result', '让步接收', 'CONCESSION', 'orange', 3, 1, NOW(), NOW()),
('mes_defect_type', '外观不良', 'APPEARANCE', 'default', 1, 1, NOW(), NOW()),
('mes_defect_type', '尺寸偏差', 'DIMENSION', 'default', 2, 1, NOW(), NOW()),
('mes_defect_type', '功能失效', 'FUNCTION', 'default', 3, 1, NOW(), NOW()),
('mes_defect_type', '焊接不良', 'SOLDER', 'default', 4, 1, NOW(), NOW()),
('mes_defect_type', '来料不良', 'MATERIAL', 'default', 5, 1, NOW(), NOW()),
('mes_defect_type', '其他', 'OTHER', 'default', 6, 1, NOW(), NOW()),
('mes_equipment_status', '运行', 'RUNNING', 'green', 1, 1, NOW(), NOW()),
('mes_equipment_status', '闲置', 'IDLE', 'default', 2, 1, NOW(), NOW()),
('mes_equipment_status', '维修中', 'REPAIR', 'orange', 3, 1, NOW(), NOW()),
('mes_equipment_status', '报废', 'SCRAPPED', 'red', 4, 1, NOW(), NOW()),
('mes_maintain_type', '故障维修', 'REPAIR', 'red', 1, 1, NOW(), NOW()),
('mes_maintain_type', '定期保养', 'MAINTAIN', 'blue', 2, 1, NOW(), NOW()),
('mes_maintain_status', '待处理', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('mes_maintain_status', '处理中', 'IN_PROGRESS', 'processing', 2, 1, NOW(), NOW()),
('mes_maintain_status', '已完成', 'DONE', 'green', 3, 1, NOW(), NOW());

-- 生产物料管理：物料计划属性、齐套分析、补料申请
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(169, 922, '齐套分析', 'C', 'kit', 'mes/kit/index', 'mes:kit:list', 'CheckSquareOutlined', 2, 1, 1, NOW(), NOW()),
(170, 922, '补料申请', 'C', 'supplement', 'mes/supplement/index', 'mes:supplement:list', 'DiffOutlined', 3, 1, 1, NOW(), NOW()),
(1373, 137, '生产计划属性', 'F', NULL, NULL, 'mes:material:edit', NULL, 4, 1, 1, NOW(), NOW()),
(1690, 169, '缺料下单', 'F', NULL, NULL, 'mes:kit:release', NULL, 1, 1, 1, NOW(), NOW()),
(1691, 169, '替代料切换', 'F', NULL, NULL, 'mes:kit:substitute', NULL, 2, 1, 1, NOW(), NOW()),
(1700, 170, '补料申请', 'F', NULL, NULL, 'mes:supplement:add', NULL, 1, 1, 1, NOW(), NOW()),
(1701, 170, '补料发料', 'F', NULL, NULL, 'mes:supplement:issue', NULL, 2, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(97, '补料原因', 'mes_supplement_reason', 1, '工单补料的原因', NOW(), NOW()),
(98, '补料状态', 'mes_supplement_status', 1, '补料申请的状态', NOW(), NOW()),
(99, '齐套状态', 'mes_kit_status', 1, '工单用料的齐套情况', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('mes_supply_type', '委外', 'OUTSOURCE', 'purple', 3, 1, NOW(), NOW()),
('mes_supply_type', '虚拟件', 'PHANTOM', 'default', 4, 1, NOW(), NOW()),
('mes_wo_doc_kind', '补料', 'SUPPLEMENT', 'red', 4, 1, NOW(), NOW()),
('mes_supplement_reason', '生产报废', 'SCRAP', 'red', 1, 1, NOW(), NOW()),
('mes_supplement_reason', '损耗超标', 'LOSS', 'orange', 2, 1, NOW(), NOW()),
('mes_supplement_reason', '来料不良', 'DEFECT', 'purple', 3, 1, NOW(), NOW()),
('mes_supplement_reason', '其他', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('mes_supplement_status', '审批中', 'APPROVING', 'processing', 1, 1, NOW(), NOW()),
('mes_supplement_status', '待发料', 'APPROVED', 'blue', 2, 1, NOW(), NOW()),
('mes_supplement_status', '已发料', 'DONE', 'green', 3, 1, NOW(), NOW()),
('mes_supplement_status', '已拒绝', 'REJECTED', 'red', 4, 1, NOW(), NOW()),
('mes_supplement_status', '已撤销', 'CANCELED', 'default', 5, 1, NOW(), NOW()),
('mes_kit_status', '齐套', 'COMPLETE', 'green', 1, 1, NOW(), NOW()),
('mes_kit_status', '部分缺料', 'PARTIAL', 'orange', 2, 1, NOW(), NOW()),
('mes_kit_status', '缺料', 'SHORT', 'red', 3, 1, NOW(), NOW()),
('mes_kit_status', '已领齐', 'ISSUED', 'default', 4, 1, NOW(), NOW()),
('scm_order_source', '生产缺料', 'KIT', 'orange', 4, 1, NOW(), NOW()),
('wf_category', '生产', 'PRODUCTION', 'purple', 7, 1, NOW(), NOW());

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 169), (2, 170);

-- 普通用户：生产制造各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 6), (2, 160), (2, 161), (2, 162), (2, 163), (2, 164), (2, 165), (2, 166), (2, 167);

-- 示例工作中心
INSERT INTO mes_work_center (id, code, name, labor_rate, overhead_rate, status, create_time, update_time) VALUES
(1, 'WC01', '装配线', 45, 30, 1, NOW(), NOW()),
(2, 'WC02', '包装线', 35, 15, 1, NOW(), NOW());

-- ---------------------------------------------------------------- 财务管理：菜单、字典、会计科目

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(7,   0, '财务管理', 'M', '/fin', NULL, NULL, 'AccountBookOutlined', 4, 1, 1, NOW(), NOW()),
(180, 931, '凭证管理', 'C', 'voucher', 'fin/voucher/index', 'fin:voucher:list', 'FileTextOutlined', 1, 1, 1, NOW(), NOW()),
(181, 931, '账簿查询', 'C', 'book', 'fin/book/index', 'fin:book:list', 'ReadOutlined', 2, 1, 1, NOW(), NOW()),
(182, 931, '财务报表', 'C', 'report', 'fin/report/index', 'fin:report:list', 'PieChartOutlined', 3, 1, 1, NOW(), NOW()),
(183, 932, '应收管理', 'C', 'ar', 'fin/ar/index', 'fin:ar:list', 'RiseOutlined', 1, 1, 1, NOW(), NOW()),
(184, 932, '应付管理', 'C', 'ap', 'fin/ap/index', 'fin:ap:list', 'FallOutlined', 2, 1, 1, NOW(), NOW()),
(185, 932, '收款单', 'C', 'receipt', 'fin/receipt/index', 'fin:receipt:list', 'TransactionOutlined', 3, 1, 1, NOW(), NOW()),
(186, 932, '付款单', 'C', 'payment', 'fin/payment/index', 'fin:payment:list', 'PayCircleOutlined', 4, 1, 1, NOW(), NOW()),
(187, 934, '固定资产', 'C', 'asset', 'fin/asset/index', 'fin:asset:list', 'BuildOutlined', 1, 1, 1, NOW(), NOW()),
(188, 934, '成本核算', 'C', 'cost', 'fin/cost/index', 'fin:cost:list', 'FundOutlined', 2, 1, 1, NOW(), NOW()),
(189, 935, '预算管理', 'C', 'budget', 'fin/budget/index', 'fin:budget:list', 'AimOutlined', 1, 1, 1, NOW(), NOW()),
(190, 936, '会计科目', 'C', 'account', 'fin/account/index', 'fin:account:list', 'OrderedListOutlined', 2, 1, 1, NOW(), NOW()),
(191, 936, '期末结账', 'C', 'period', 'fin/period/index', 'fin:period:list', 'LockOutlined', 1, 1, 1, NOW(), NOW()),
(1800, 180, '凭证新增', 'F', NULL, NULL, 'fin:voucher:add', NULL, 1, 1, 1, NOW(), NOW()),
(1801, 180, '凭证修改', 'F', NULL, NULL, 'fin:voucher:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1802, 180, '凭证删除', 'F', NULL, NULL, 'fin:voucher:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1803, 180, '凭证审核', 'F', NULL, NULL, 'fin:voucher:audit', NULL, 4, 1, 1, NOW(), NOW()),
(1804, 180, '凭证记账', 'F', NULL, NULL, 'fin:voucher:post', NULL, 5, 1, 1, NOW(), NOW()),
(1830, 183, '应收单新增', 'F', NULL, NULL, 'fin:ar:add', NULL, 1, 1, 1, NOW(), NOW()),
(1831, 183, '应收单删除', 'F', NULL, NULL, 'fin:ar:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1832, 183, '应收生成凭证', 'F', NULL, NULL, 'fin:ar:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1840, 184, '应付单新增', 'F', NULL, NULL, 'fin:ap:add', NULL, 1, 1, 1, NOW(), NOW()),
(1841, 184, '应付单删除', 'F', NULL, NULL, 'fin:ap:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1842, 184, '应付生成凭证', 'F', NULL, NULL, 'fin:ap:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1850, 185, '收款单新增', 'F', NULL, NULL, 'fin:receipt:add', NULL, 1, 1, 1, NOW(), NOW()),
(1851, 185, '收款单删除', 'F', NULL, NULL, 'fin:receipt:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1852, 185, '收款生成凭证', 'F', NULL, NULL, 'fin:receipt:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1860, 186, '付款单新增', 'F', NULL, NULL, 'fin:payment:add', NULL, 1, 1, 1, NOW(), NOW()),
(1861, 186, '付款单删除', 'F', NULL, NULL, 'fin:payment:remove', NULL, 2, 1, 1, NOW(), NOW()),
(1862, 186, '付款生成凭证', 'F', NULL, NULL, 'fin:payment:voucher', NULL, 3, 1, 1, NOW(), NOW()),
(1870, 187, '资产新增', 'F', NULL, NULL, 'fin:asset:add', NULL, 1, 1, 1, NOW(), NOW()),
(1871, 187, '资产修改', 'F', NULL, NULL, 'fin:asset:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1872, 187, '资产删除', 'F', NULL, NULL, 'fin:asset:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1873, 187, '计提折旧', 'F', NULL, NULL, 'fin:asset:depreciate', NULL, 4, 1, 1, NOW(), NOW()),
(1880, 188, '成本核算', 'F', NULL, NULL, 'fin:cost:run', NULL, 1, 1, 1, NOW(), NOW()),
(1890, 189, '预算编制', 'F', NULL, NULL, 'fin:budget:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1900, 190, '科目新增', 'F', NULL, NULL, 'fin:account:add', NULL, 1, 1, 1, NOW(), NOW()),
(1901, 190, '科目修改', 'F', NULL, NULL, 'fin:account:edit', NULL, 2, 1, 1, NOW(), NOW()),
(1902, 190, '科目删除', 'F', NULL, NULL, 'fin:account:remove', NULL, 3, 1, 1, NOW(), NOW()),
(1910, 191, '结账', 'F', NULL, NULL, 'fin:period:close', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(40, '科目类别', 'fin_account_category', 1, '会计科目的类别', NOW(), NOW()),
(41, '凭证状态', 'fin_voucher_status', 1, '记账凭证的状态', NOW(), NOW()),
(42, '凭证来源', 'fin_voucher_source', 1, '手工或业务生成', NOW(), NOW()),
(43, '核销状态', 'fin_bill_status', 1, '应收应付单的核销状态', NOW(), NOW()),
(44, '应收应付来源', 'fin_bill_source', 1, '应收应付单的来源', NOW(), NOW()),
(45, '结算方式', 'fin_settle_method', 1, '收付款的结算方式', NOW(), NOW()),
(46, '资产类别', 'fin_asset_category', 1, '固定资产的类别', NOW(), NOW()),
(47, '资产状态', 'fin_asset_status', 1, '固定资产的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_account_category', '资产', 'ASSET', 'blue', 1, 1, NOW(), NOW()),
('fin_account_category', '负债', 'LIABILITY', 'orange', 2, 1, NOW(), NOW()),
('fin_account_category', '所有者权益', 'EQUITY', 'purple', 3, 1, NOW(), NOW()),
('fin_account_category', '成本', 'COST', 'cyan', 4, 1, NOW(), NOW()),
('fin_account_category', '损益', 'PROFIT', 'green', 5, 1, NOW(), NOW()),
('fin_voucher_status', '未审核', 'DRAFT', 'orange', 1, 1, NOW(), NOW()),
('fin_voucher_status', '已审核', 'AUDITED', 'blue', 2, 1, NOW(), NOW()),
('fin_voucher_status', '已记账', 'POSTED', 'green', 3, 1, NOW(), NOW()),
('fin_voucher_source', '手工', 'MANUAL', 'default', 1, 1, NOW(), NOW()),
('fin_voucher_source', '应收', 'AR', 'green', 2, 1, NOW(), NOW()),
('fin_voucher_source', '应付', 'AP', 'orange', 3, 1, NOW(), NOW()),
('fin_voucher_source', '收款', 'RECEIPT', 'cyan', 4, 1, NOW(), NOW()),
('fin_voucher_source', '付款', 'PAYMENT', 'gold', 5, 1, NOW(), NOW()),
('fin_voucher_source', '折旧', 'DEPRECIATION', 'purple', 6, 1, NOW(), NOW()),
('fin_voucher_source', '资产处置', 'ASSET', 'purple', 7, 1, NOW(), NOW()),
('fin_voucher_source', '成本结转', 'COST', 'blue', 8, 1, NOW(), NOW()),
('fin_voucher_source', '结转损益', 'CLOSE', 'red', 9, 1, NOW(), NOW()),
('fin_bill_status', '未核销', 'UNSETTLED', 'orange', 1, 1, NOW(), NOW()),
('fin_bill_status', '部分核销', 'PARTIAL', 'blue', 2, 1, NOW(), NOW()),
('fin_bill_status', '已核销', 'SETTLED', 'green', 3, 1, NOW(), NOW()),
('fin_bill_source', '销售出库', 'SALES_OUT', 'green', 1, 1, NOW(), NOW()),
('fin_bill_source', '销售退货', 'SALES_RETURN', 'red', 2, 1, NOW(), NOW()),
('fin_bill_source', '采购入库', 'PURCHASE_IN', 'blue', 3, 1, NOW(), NOW()),
('fin_bill_source', '采购退货', 'PURCHASE_RETURN', 'orange', 4, 1, NOW(), NOW()),
('fin_bill_source', '期初 / 手工', 'MANUAL', 'default', 5, 1, NOW(), NOW()),
('fin_settle_method', '银行转账', 'TRANSFER', 'default', 1, 1, NOW(), NOW()),
('fin_settle_method', '现金', 'CASH', 'default', 2, 1, NOW(), NOW()),
('fin_settle_method', '承兑汇票', 'DRAFT', 'default', 3, 1, NOW(), NOW()),
('fin_settle_method', '支付宝 / 微信', 'ONLINE', 'default', 4, 1, NOW(), NOW()),
('fin_asset_category', '房屋建筑物', 'BUILDING', 'default', 1, 1, NOW(), NOW()),
('fin_asset_category', '机器设备', 'MACHINE', 'default', 2, 1, NOW(), NOW()),
('fin_asset_category', '运输工具', 'VEHICLE', 'default', 3, 1, NOW(), NOW()),
('fin_asset_category', '电子设备', 'ELECTRONIC', 'default', 4, 1, NOW(), NOW()),
('fin_asset_category', '办公家具', 'FURNITURE', 'default', 5, 1, NOW(), NOW()),
('fin_asset_status', '在用', 'IN_USE', 'green', 1, 1, NOW(), NOW()),
('fin_asset_status', '闲置', 'IDLE', 'orange', 2, 1, NOW(), NOW()),
('fin_asset_status', '已处置', 'DISPOSED', 'default', 3, 1, NOW(), NOW());

-- 普通用户：财务管理各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 7), (2, 180), (2, 181), (2, 182), (2, 183), (2, 184), (2, 185), (2, 186), (2, 187), (2, 188), (2, 189), (2, 190), (2, 191);

-- 会计科目（企业会计准则常用科目）：自动生成凭证使用的科目见 FinConstants，需保持为末级科目
INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(1,  '1001',   '库存现金',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(2,  '1002',   '银行存款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(3,  '100201', '工商银行',               2,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(4,  '100202', '招商银行',               2,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(5,  '1122',   '应收账款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(6,  '1123',   '预付账款',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(7,  '1221',   '其他应收款',             0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(8,  '1403',   '原材料',                 0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(9,  '1405',   '库存商品',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(10, '1601',   '固定资产',               0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(11, '1602',   '累计折旧',               0,  'ASSET',     'CREDIT', 1, NOW(), NOW()),
(12, '1606',   '固定资产清理',           0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(13, '1901',   '待处理财产损溢',         0,  'ASSET',     'DEBIT',  1, NOW(), NOW()),
(20, '2001',   '短期借款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(21, '2202',   '应付账款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(22, '2203',   '预收账款',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(23, '2211',   '应付职工薪酬',           0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(24, '2221',   '应交税费',               0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(25, '222101', '应交增值税（进项税额）', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(26, '222102', '应交增值税（销项税额）', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(27, '2241',   '其他应付款',             0,  'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(30, '4001',   '实收资本',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(31, '4103',   '本年利润',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(32, '4104',   '利润分配',               0,  'EQUITY',    'CREDIT', 1, NOW(), NOW()),
(40, '5001',   '生产成本',               0,  'COST',      'DEBIT',  1, NOW(), NOW()),
(41, '5101',   '制造费用',               0,  'COST',      'DEBIT',  1, NOW(), NOW()),
(50, '6001',   '主营业务收入',           0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(51, '6051',   '其他业务收入',           0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(52, '6301',   '营业外收入',             0,  'PROFIT',    'CREDIT', 1, NOW(), NOW()),
(53, '6401',   '主营业务成本',           0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(54, '6403',   '税金及附加',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(55, '6601',   '销售费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(56, '6602',   '管理费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(57, '660201', '工资',                   56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(58, '660202', '办公费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(59, '660203', '折旧费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(60, '660204', '差旅费',                 56, 'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(61, '6603',   '财务费用',               0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(62, '6711',   '营业外支出',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW()),
(63, '6801',   '所得税费用',             0,  'PROFIT',    'DEBIT',  1, NOW(), NOW());
-- 往来科目按客户 / 供应商辅助核算
UPDATE fin_account SET aux_items = 'CUSTOMER' WHERE code IN ('1122', '2203');
UPDATE fin_account SET aux_items = 'SUPPLIER' WHERE code IN ('1123', '2202');

-- ---------------------------------------------------------------- 人力资源：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(8,   0, '人力资源', 'M', '/hr', NULL, NULL, 'IdcardOutlined', 7, 1, 1, NOW(), NOW()),
(200, 951, '员工档案', 'C', 'employee', 'hr/employee/index', 'hr:employee:list', 'SolutionOutlined', 2, 1, 1, NOW(), NOW()),
(201, 951, '职位管理', 'C', 'position', 'hr/position/index', 'hr:position:list', 'ClusterOutlined', 4, 1, 1, NOW(), NOW()),
(202, 952, '招聘管理', 'C', 'recruit', 'hr/recruit/index', 'hr:recruit:list', 'UserAddOutlined', 1, 1, 1, NOW(), NOW()),
(203, 952, '培训管理', 'C', 'training', 'hr/training/index', 'hr:training:list', 'ReadOutlined', 2, 1, 1, NOW(), NOW()),
(204, 953, '考勤管理', 'C', 'attendance', 'hr/attendance/index', 'hr:attendance:list', 'FieldTimeOutlined', 1, 1, 1, NOW(), NOW()),
(205, 953, '假勤审批', 'C', 'leave', 'hr/leave/index', 'hr:leave:list', 'CalendarOutlined', 2, 1, 1, NOW(), NOW()),
(206, 954, '薪资档案', 'C', 'salary', 'hr/salary/index', 'hr:salary:list', 'WalletOutlined', 1, 1, 1, NOW(), NOW()),
(207, 954, '薪酬核算', 'C', 'payroll', 'hr/payroll/index', 'hr:payroll:list', 'MoneyCollectOutlined', 2, 1, 1, NOW(), NOW()),
(208, 953, '绩效考核', 'C', 'perf', 'hr/perf/index', 'hr:perf:list', 'TrophyOutlined', 3, 1, 1, NOW(), NOW()),
(209, 951, '员工自助', 'C', 'self', 'hr/self/index', 'hr:self:list', 'SmileOutlined', 1, 1, 1, NOW(), NOW()),
(2000, 200, '员工新增', 'F', NULL, NULL, 'hr:employee:add', NULL, 1, 1, 1, NOW(), NOW()),
(2001, 200, '员工修改', 'F', NULL, NULL, 'hr:employee:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2002, 200, '员工删除', 'F', NULL, NULL, 'hr:employee:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2003, 200, '人事异动', 'F', NULL, NULL, 'hr:employee:change', NULL, 4, 1, 1, NOW(), NOW()),
(2010, 201, '职位新增', 'F', NULL, NULL, 'hr:position:add', NULL, 1, 1, 1, NOW(), NOW()),
(2011, 201, '职位修改', 'F', NULL, NULL, 'hr:position:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2012, 201, '职位删除', 'F', NULL, NULL, 'hr:position:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2020, 202, '需求新增', 'F', NULL, NULL, 'hr:recruit:add', NULL, 1, 1, 1, NOW(), NOW()),
(2021, 202, '需求修改', 'F', NULL, NULL, 'hr:recruit:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2022, 202, '需求删除', 'F', NULL, NULL, 'hr:recruit:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2023, 202, '需求审核', 'F', NULL, NULL, 'hr:recruit:approve', NULL, 4, 1, 1, NOW(), NOW()),
(2024, 202, '应聘者管理', 'F', NULL, NULL, 'hr:recruit:candidate', NULL, 5, 1, 1, NOW(), NOW()),
(2025, 202, '录用入职', 'F', NULL, NULL, 'hr:recruit:hire', NULL, 6, 1, 1, NOW(), NOW()),
(2030, 203, '培训新增', 'F', NULL, NULL, 'hr:training:add', NULL, 1, 1, 1, NOW(), NOW()),
(2031, 203, '培训修改', 'F', NULL, NULL, 'hr:training:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2032, 203, '培训删除', 'F', NULL, NULL, 'hr:training:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2040, 204, '考勤补录', 'F', NULL, NULL, 'hr:attendance:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2041, 204, '班次与节假日', 'F', NULL, NULL, 'hr:attendance:config', NULL, 2, 1, 1, NOW(), NOW()),
(2050, 205, '假勤审批', 'F', NULL, NULL, 'hr:leave:approve', NULL, 1, 1, 1, NOW(), NOW()),
(2060, 206, '薪资档案维护', 'F', NULL, NULL, 'hr:salary:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2070, 207, '生成工资表', 'F', NULL, NULL, 'hr:payroll:add', NULL, 1, 1, 1, NOW(), NOW()),
(2071, 207, '工资调整', 'F', NULL, NULL, 'hr:payroll:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2072, 207, '工资确认', 'F', NULL, NULL, 'hr:payroll:confirm', NULL, 3, 1, 1, NOW(), NOW()),
(2073, 207, '工资发放', 'F', NULL, NULL, 'hr:payroll:pay', NULL, 4, 1, 1, NOW(), NOW()),
(2074, 207, '工资表删除', 'F', NULL, NULL, 'hr:payroll:remove', NULL, 5, 1, 1, NOW(), NOW()),
(2080, 208, '考核新增', 'F', NULL, NULL, 'hr:perf:add', NULL, 1, 1, 1, NOW(), NOW()),
(2081, 208, '考核修改', 'F', NULL, NULL, 'hr:perf:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2082, 208, '考核删除', 'F', NULL, NULL, 'hr:perf:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2083, 208, '代为评分', 'F', NULL, NULL, 'hr:perf:score', NULL, 4, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(50, '职位类别', 'hr_position_category', 1, '职位的类别', NOW(), NOW()),
(51, '学历', 'hr_education', 1, '员工、应聘者的学历', NOW(), NOW()),
(52, '用工类型', 'hr_emp_type', 1, '员工的用工类型', NOW(), NOW()),
(53, '员工状态', 'hr_emp_status', 1, '试用、正式、离职', NOW(), NOW()),
(54, '异动类型', 'hr_change_type', 1, '人事异动的类型', NOW(), NOW()),
(55, '招聘需求状态', 'hr_recruit_status', 1, '招聘需求的状态', NOW(), NOW()),
(56, '应聘阶段', 'hr_candidate_stage', 1, '应聘者所处阶段', NOW(), NOW()),
(57, '简历来源', 'hr_candidate_source', 1, '应聘者的简历来源', NOW(), NOW()),
(58, '面试结果', 'hr_interview_result', 1, '面试结果', NOW(), NOW()),
(59, '培训类别', 'hr_training_category', 1, '培训的类别', NOW(), NOW()),
(60, '培训状态', 'hr_training_status', 1, '培训的状态', NOW(), NOW()),
(61, '考勤状态', 'hr_attendance_status', 1, '每日考勤结果', NOW(), NOW()),
(62, '假期类型', 'hr_leave_type', 1, '请假的类型', NOW(), NOW()),
(63, '假勤状态', 'hr_leave_status', 1, '请假、加班申请的状态', NOW(), NOW()),
(64, '工资表状态', 'hr_payroll_status', 1, '工资表的状态', NOW(), NOW()),
(65, '考核状态', 'hr_perf_status', 1, '绩效考核方案的状态', NOW(), NOW()),
(66, '绩效等级', 'hr_perf_grade', 1, '绩效等级与奖金系数', NOW(), NOW()),
(67, '考核表状态', 'hr_appraisal_status', 1, '个人考核表的状态', NOW(), NOW()),
(68, '节假日类型', 'hr_holiday_type', 1, '法定节假日、调休上班', NOW(), NOW()),
(69, '假勤种类', 'hr_leave_kind', 1, '请假、加班', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time) VALUES
('hr_position_category', '管理', 'MANAGEMENT', 'purple', 1, 1, NULL, NOW(), NOW()),
('hr_position_category', '技术', 'TECH', 'blue', 2, 1, NULL, NOW(), NOW()),
('hr_position_category', '生产', 'PRODUCTION', 'cyan', 3, 1, NULL, NOW(), NOW()),
('hr_position_category', '销售', 'SALES', 'green', 4, 1, NULL, NOW(), NOW()),
('hr_position_category', '职能', 'FUNCTION', 'default', 5, 1, NULL, NOW(), NOW()),
('hr_education', '博士', 'DOCTOR', 'default', 1, 1, NULL, NOW(), NOW()),
('hr_education', '硕士', 'MASTER', 'default', 2, 1, NULL, NOW(), NOW()),
('hr_education', '本科', 'BACHELOR', 'default', 3, 1, NULL, NOW(), NOW()),
('hr_education', '大专', 'COLLEGE', 'default', 4, 1, NULL, NOW(), NOW()),
('hr_education', '高中及以下', 'HIGH', 'default', 5, 1, NULL, NOW(), NOW()),
('hr_emp_type', '全职', 'FULL_TIME', 'blue', 1, 1, NULL, NOW(), NOW()),
('hr_emp_type', '兼职', 'PART_TIME', 'cyan', 2, 1, NULL, NOW(), NOW()),
('hr_emp_type', '实习', 'INTERN', 'orange', 3, 1, NULL, NOW(), NOW()),
('hr_emp_type', '劳务派遣', 'OUTSOURCE', 'default', 4, 1, NULL, NOW(), NOW()),
('hr_emp_status', '试用期', 'PROBATION', 'orange', 1, 1, NULL, NOW(), NOW()),
('hr_emp_status', '正式', 'REGULAR', 'green', 2, 1, NULL, NOW(), NOW()),
('hr_emp_status', '离职', 'LEFT', 'default', 3, 1, NULL, NOW(), NOW()),
('hr_change_type', '入职', 'ENTRY', 'blue', 1, 1, NULL, NOW(), NOW()),
('hr_change_type', '转正', 'REGULAR', 'green', 2, 1, NULL, NOW(), NOW()),
('hr_change_type', '调岗', 'TRANSFER', 'purple', 3, 1, NULL, NOW(), NOW()),
('hr_change_type', '调薪', 'SALARY', 'gold', 4, 1, NULL, NOW(), NOW()),
('hr_change_type', '离职', 'LEAVE', 'default', 5, 1, NULL, NOW(), NOW()),
('hr_recruit_status', '草稿', 'DRAFT', 'orange', 1, 1, NULL, NOW(), NOW()),
('hr_recruit_status', '审批中', 'APPROVING', 'processing', 2, 1, NULL, NOW(), NOW()),
('hr_recruit_status', '招聘中', 'OPEN', 'blue', 3, 1, NULL, NOW(), NOW()),
('hr_recruit_status', '已关闭', 'CLOSED', 'default', 4, 1, NULL, NOW(), NOW()),
('hr_candidate_stage', '新简历', 'NEW', 'default', 1, 1, NULL, NOW(), NOW()),
('hr_candidate_stage', '面试中', 'INTERVIEW', 'processing', 2, 1, NULL, NOW(), NOW()),
('hr_candidate_stage', '已发 Offer', 'OFFER', 'cyan', 3, 1, NULL, NOW(), NOW()),
('hr_candidate_stage', '已入职', 'HIRED', 'green', 4, 1, NULL, NOW(), NOW()),
('hr_candidate_stage', '已淘汰', 'REJECTED', 'red', 5, 1, NULL, NOW(), NOW()),
('hr_candidate_source', '招聘网站', 'WEBSITE', 'default', 1, 1, NULL, NOW(), NOW()),
('hr_candidate_source', '内部推荐', 'REFERRAL', 'default', 2, 1, NULL, NOW(), NOW()),
('hr_candidate_source', '猎头', 'HUNTER', 'default', 3, 1, NULL, NOW(), NOW()),
('hr_candidate_source', '校园招聘', 'CAMPUS', 'default', 4, 1, NULL, NOW(), NOW()),
('hr_candidate_source', '其他', 'OTHER', 'default', 5, 1, NULL, NOW(), NOW()),
('hr_interview_result', '待面试', 'PENDING', 'default', 1, 1, NULL, NOW(), NOW()),
('hr_interview_result', '通过', 'PASS', 'green', 2, 1, NULL, NOW(), NOW()),
('hr_interview_result', '未通过', 'FAIL', 'red', 3, 1, NULL, NOW(), NOW()),
('hr_training_category', '入职培训', 'ONBOARDING', 'blue', 1, 1, NULL, NOW(), NOW()),
('hr_training_category', '技能培训', 'SKILL', 'cyan', 2, 1, NULL, NOW(), NOW()),
('hr_training_category', '安全培训', 'SAFETY', 'red', 3, 1, NULL, NOW(), NOW()),
('hr_training_category', '质量培训', 'QUALITY', 'green', 4, 1, NULL, NOW(), NOW()),
('hr_training_category', '管理培训', 'MANAGEMENT', 'purple', 5, 1, NULL, NOW(), NOW()),
('hr_training_status', '计划中', 'PLANNED', 'default', 1, 1, NULL, NOW(), NOW()),
('hr_training_status', '进行中', 'ONGOING', 'processing', 2, 1, NULL, NOW(), NOW()),
('hr_training_status', '已结束', 'FINISHED', 'green', 3, 1, NULL, NOW(), NOW()),
('hr_training_status', '已取消', 'CANCELED', 'red', 4, 1, NULL, NOW(), NOW()),
('hr_attendance_status', '正常', 'NORMAL', 'green', 1, 1, NULL, NOW(), NOW()),
('hr_attendance_status', '迟到', 'LATE', 'orange', 2, 1, NULL, NOW(), NOW()),
('hr_attendance_status', '早退', 'EARLY', 'orange', 3, 1, NULL, NOW(), NOW()),
('hr_attendance_status', '迟到早退', 'LATE_EARLY', 'red', 4, 1, NULL, NOW(), NOW()),
('hr_attendance_status', '缺卡', 'MISSING', 'red', 5, 1, NULL, NOW(), NOW()),
('hr_leave_type', '年假', 'ANNUAL', 'green', 1, 1, '带薪', NOW(), NOW()),
('hr_leave_type', '事假', 'PERSONAL', 'orange', 2, 1, '按小时扣发工资', NOW(), NOW()),
('hr_leave_type', '病假', 'SICK', 'red', 3, 1, '按小时扣发 40% 工资', NOW(), NOW()),
('hr_leave_type', '婚假', 'MARRIAGE', 'magenta', 4, 1, '带薪', NOW(), NOW()),
('hr_leave_type', '产假 / 陪产假', 'MATERNITY', 'pink', 5, 1, '带薪', NOW(), NOW()),
('hr_leave_type', '调休', 'COMPENSATORY', 'blue', 6, 1, '带薪', NOW(), NOW()),
('hr_leave_status', '审批中', 'APPROVING', 'processing', 1, 1, NULL, NOW(), NOW()),
('hr_leave_status', '已通过', 'APPROVED', 'green', 2, 1, NULL, NOW(), NOW()),
('hr_leave_status', '已拒绝', 'REJECTED', 'red', 3, 1, NULL, NOW(), NOW()),
('hr_leave_status', '已撤销', 'CANCELED', 'default', 4, 1, NULL, NOW(), NOW()),
('hr_payroll_status', '草稿', 'DRAFT', 'orange', 1, 1, NULL, NOW(), NOW()),
('hr_payroll_status', '已确认', 'CONFIRMED', 'blue', 2, 1, NULL, NOW(), NOW()),
('hr_payroll_status', '已发放', 'PAID', 'green', 3, 1, NULL, NOW(), NOW()),
('hr_perf_status', '草稿', 'DRAFT', 'orange', 1, 1, NULL, NOW(), NOW()),
('hr_perf_status', '考核中', 'SCORING', 'processing', 2, 1, NULL, NOW(), NOW()),
('hr_perf_status', '已归档', 'FINISHED', 'green', 3, 1, NULL, NOW(), NOW()),
('hr_perf_grade', 'S 卓越', 'S', 'gold', 1, 1, '90 分及以上，奖金系数 1.5', NOW(), NOW()),
('hr_perf_grade', 'A 优秀', 'A', 'green', 2, 1, '80-89 分，奖金系数 1.2', NOW(), NOW()),
('hr_perf_grade', 'B 良好', 'B', 'blue', 3, 1, '70-79 分，奖金系数 1.0', NOW(), NOW()),
('hr_perf_grade', 'C 合格', 'C', 'orange', 4, 1, '60-69 分，奖金系数 0.8', NOW(), NOW()),
('hr_perf_grade', 'D 待改进', 'D', 'red', 5, 1, '60 分以下，无绩效奖金', NOW(), NOW()),
('hr_appraisal_status', '待自评', 'SELF', 'orange', 1, 1, NULL, NOW(), NOW()),
('hr_appraisal_status', '待评分', 'REVIEW', 'processing', 2, 1, NULL, NOW(), NOW()),
('hr_appraisal_status', '已完成', 'DONE', 'green', 3, 1, NULL, NOW(), NOW()),
('hr_holiday_type', '法定节假日', 'HOLIDAY', 'red', 1, 1, NULL, NOW(), NOW()),
('hr_holiday_type', '调休上班', 'WORKDAY', 'blue', 2, 1, NULL, NOW(), NOW()),
('hr_leave_kind', '请假', 'LEAVE', 'blue', 1, 1, NULL, NOW(), NOW()),
('hr_leave_kind', '加班', 'OVERTIME', 'purple', 2, 1, NULL, NOW(), NOW());

-- 普通用户：人力资源各页面只读；员工自助可以打卡、请假、自评（需关联员工档案）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 8), (2, 200), (2, 201), (2, 202), (2, 203), (2, 204), (2, 205), (2, 206), (2, 207), (2, 208), (2, 209);

-- 示例职位、默认班次、2026 年国庆节假日、管理员和测试用户的员工档案
INSERT INTO hr_position (id, code, name, category, grade, sort, status, create_time, update_time) VALUES
(1, 'GM', '总经理', 'MANAGEMENT', 'M5', 1, 1, NOW(), NOW()),
(2, 'RD-ENG', '研发工程师', 'TECH', 'P5', 2, 1, NOW(), NOW()),
(3, 'SALES', '销售经理', 'SALES', 'P5', 3, 1, NOW(), NOW()),
(4, 'ACCOUNTANT', '会计', 'FUNCTION', 'P4', 4, 1, NOW(), NOW()),
(5, 'OPERATOR', '生产操作员', 'PRODUCTION', 'P2', 5, 1, NOW(), NOW());

INSERT INTO hr_shift (id, name, start_time, end_time, flex_minutes, work_hours, is_default, create_time, update_time) VALUES
(1, '标准班', '09:00:00', '18:00:00', 10, 8, 1, NOW(), NOW()),
(2, '早班', '08:00:00', '17:00:00', 5, 8, 0, NOW(), NOW());

INSERT INTO hr_holiday (holiday_date, name, type, create_time, update_time) VALUES
('2026-10-01', '国庆节', 'HOLIDAY', NOW(), NOW()),
('2026-10-02', '国庆节', 'HOLIDAY', NOW(), NOW()),
('2026-10-05', '国庆节', 'HOLIDAY', NOW(), NOW()),
('2026-10-06', '国庆节', 'HOLIDAY', NOW(), NOW()),
('2026-10-07', '国庆节', 'HOLIDAY', NOW(), NOW()),
('2026-10-10', '国庆节调休', 'WORKDAY', NOW(), NOW());

INSERT INTO hr_employee (id, emp_no, name, gender, phone, education, dept_id, position_id, user_id, emp_type, status, entry_date, regular_date, create_time, update_time) VALUES
(1, 'E0001', '超级管理员', 'M', '13800000001', 'MASTER', 1, 1, 1, 'FULL_TIME', 'REGULAR', '2020-01-02', '2020-04-02', NOW(), NOW()),
(2, 'E0002', '测试用户', 'F', '13800000002', 'BACHELOR', 10, 2, 2, 'FULL_TIME', 'REGULAR', '2023-07-03', '2023-10-03', NOW(), NOW());

-- ---------------------------------------------------------------- 客户关系管理：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(9,   0, '客户关系', 'M', '/crm', NULL, NULL, 'HeartOutlined', 5, 1, 1, NOW(), NOW()),
(220, 941, '客户管理', 'C', 'customer', 'crm/customer/index', 'crm:customer:list', 'ContactsOutlined', 1, 1, 1, NOW(), NOW()),
(221, 941, '线索管理', 'C', 'lead', 'crm/lead/index', 'crm:lead:list', 'BulbOutlined', 2, 1, 1, NOW(), NOW()),
(222, 941, '销售机会', 'C', 'opportunity', 'crm/opportunity/index', 'crm:opp:list', 'FunnelPlotOutlined', 3, 1, 1, NOW(), NOW()),
(223, 941, '跟进记录', 'C', 'followup', 'crm/followup/index', 'crm:followup:list', 'MessageOutlined', 4, 1, 1, NOW(), NOW()),
(224, 942, '市场活动', 'C', 'campaign', 'crm/campaign/index', 'crm:campaign:list', 'NotificationOutlined', 2, 1, 1, NOW(), NOW()),
(225, 942, '服务工单', 'C', 'ticket', 'crm/ticket/index', 'crm:ticket:list', 'CustomerServiceOutlined', 1, 1, 1, NOW(), NOW()),
(226, 942, '客户分析', 'C', 'analysis', 'crm/analysis/index', 'crm:analysis:list', 'RadarChartOutlined', 3, 1, 1, NOW(), NOW()),
(2200, 220, '客户新增', 'F', NULL, NULL, 'crm:customer:add', NULL, 1, 1, 1, NOW(), NOW()),
(2201, 220, '客户修改', 'F', NULL, NULL, 'crm:customer:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2202, 220, '分配客户', 'F', NULL, NULL, 'crm:customer:assign', NULL, 3, 1, 1, NOW(), NOW()),
(2203, 220, '领取公海客户', 'F', NULL, NULL, 'crm:customer:claim', NULL, 4, 1, 1, NOW(), NOW()),
(2210, 221, '线索新增', 'F', NULL, NULL, 'crm:lead:add', NULL, 1, 1, 1, NOW(), NOW()),
(2211, 221, '线索修改', 'F', NULL, NULL, 'crm:lead:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2212, 221, '线索删除', 'F', NULL, NULL, 'crm:lead:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2213, 221, '线索转化', 'F', NULL, NULL, 'crm:lead:convert', NULL, 4, 1, 1, NOW(), NOW()),
(2220, 222, '商机新增', 'F', NULL, NULL, 'crm:opp:add', NULL, 1, 1, 1, NOW(), NOW()),
(2221, 222, '商机修改', 'F', NULL, NULL, 'crm:opp:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2222, 222, '商机删除', 'F', NULL, NULL, 'crm:opp:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2223, 222, '生成销售订单', 'F', NULL, NULL, 'crm:opp:order', NULL, 4, 1, 1, NOW(), NOW()),
(2230, 223, '写跟进', 'F', NULL, NULL, 'crm:followup:add', NULL, 1, 1, 1, NOW(), NOW()),
(2231, 223, '跟进删除', 'F', NULL, NULL, 'crm:followup:remove', NULL, 2, 1, 1, NOW(), NOW()),
(2240, 224, '活动新增', 'F', NULL, NULL, 'crm:campaign:add', NULL, 1, 1, 1, NOW(), NOW()),
(2241, 224, '活动修改', 'F', NULL, NULL, 'crm:campaign:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2242, 224, '活动删除', 'F', NULL, NULL, 'crm:campaign:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2250, 225, '工单受理', 'F', NULL, NULL, 'crm:ticket:add', NULL, 1, 1, 1, NOW(), NOW()),
(2251, 225, '工单处理', 'F', NULL, NULL, 'crm:ticket:handle', NULL, 2, 1, 1, NOW(), NOW()),
(2252, 225, '工单删除', 'F', NULL, NULL, 'crm:ticket:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(70, '客户行业', 'crm_industry', 1, '客户、线索所属行业', NOW(), NOW()),
(71, '客户来源', 'crm_source', 1, '客户、线索、商机的来源渠道', NOW(), NOW()),
(72, '客户阶段', 'crm_customer_stage', 1, '客户生命周期阶段', NOW(), NOW()),
(73, '线索状态', 'crm_lead_status', 1, '销售线索的状态', NOW(), NOW()),
(74, '跟进方式', 'crm_follow_method', 1, '跟进记录的方式', NOW(), NOW()),
(75, '销售阶段', 'crm_opp_stage', 1, '商机销售阶段，备注为默认赢单概率', NOW(), NOW()),
(76, '活动类型', 'crm_campaign_type', 1, '市场活动的类型', NOW(), NOW()),
(77, '活动状态', 'crm_campaign_status', 1, '市场活动的状态', NOW(), NOW()),
(78, '工单类型', 'crm_ticket_type', 1, '服务工单的类型', NOW(), NOW()),
(79, '工单优先级', 'crm_ticket_priority', 1, '服务工单的优先级与处理时限', NOW(), NOW()),
(80, '工单状态', 'crm_ticket_status', 1, '服务工单的状态', NOW(), NOW()),
(81, '受理渠道', 'crm_ticket_channel', 1, '服务工单的受理渠道', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time) VALUES
('crm_industry', '制造业', 'MANUFACTURING', 'default', 1, 1, NULL, NOW(), NOW()),
('crm_industry', '电子电器', 'ELECTRONICS', 'default', 2, 1, NULL, NOW(), NOW()),
('crm_industry', '信息技术', 'IT', 'default', 3, 1, NULL, NOW(), NOW()),
('crm_industry', '零售批发', 'RETAIL', 'default', 4, 1, NULL, NOW(), NOW()),
('crm_industry', '能源电力', 'ENERGY', 'default', 5, 1, NULL, NOW(), NOW()),
('crm_industry', '交通物流', 'LOGISTICS', 'default', 6, 1, NULL, NOW(), NOW()),
('crm_industry', '医疗健康', 'MEDICAL', 'default', 7, 1, NULL, NOW(), NOW()),
('crm_industry', '政府与公共事业', 'GOVERNMENT', 'default', 8, 1, NULL, NOW(), NOW()),
('crm_industry', '其他', 'OTHER', 'default', 9, 1, NULL, NOW(), NOW()),
('crm_source', '官网咨询', 'WEBSITE', 'blue', 1, 1, NULL, NOW(), NOW()),
('crm_source', '展会', 'EXHIBITION', 'purple', 2, 1, NULL, NOW(), NOW()),
('crm_source', '电话营销', 'PHONE', 'cyan', 3, 1, NULL, NOW(), NOW()),
('crm_source', '客户转介绍', 'REFERRAL', 'green', 4, 1, NULL, NOW(), NOW()),
('crm_source', '广告投放', 'ADVERTISING', 'orange', 5, 1, NULL, NOW(), NOW()),
('crm_source', '其他', 'OTHER', 'default', 6, 1, NULL, NOW(), NOW()),
('crm_customer_stage', '潜在客户', 'POTENTIAL', 'default', 1, 1, NULL, NOW(), NOW()),
('crm_customer_stage', '意向客户', 'INTENT', 'blue', 2, 1, NULL, NOW(), NOW()),
('crm_customer_stage', '成交客户', 'DEAL', 'green', 3, 1, NULL, NOW(), NOW()),
('crm_customer_stage', '流失客户', 'LOST', 'red', 4, 1, NULL, NOW(), NOW()),
('crm_lead_status', '待跟进', 'NEW', 'orange', 1, 1, NULL, NOW(), NOW()),
('crm_lead_status', '跟进中', 'FOLLOWING', 'processing', 2, 1, NULL, NOW(), NOW()),
('crm_lead_status', '已转化', 'CONVERTED', 'green', 3, 1, NULL, NOW(), NOW()),
('crm_lead_status', '无效', 'INVALID', 'default', 4, 1, NULL, NOW(), NOW()),
('crm_follow_method', '电话', 'PHONE', 'blue', 1, 1, NULL, NOW(), NOW()),
('crm_follow_method', '拜访', 'VISIT', 'green', 2, 1, NULL, NOW(), NOW()),
('crm_follow_method', '微信', 'WECHAT', 'cyan', 3, 1, NULL, NOW(), NOW()),
('crm_follow_method', '邮件', 'EMAIL', 'default', 4, 1, NULL, NOW(), NOW()),
('crm_follow_method', '会议', 'MEETING', 'purple', 5, 1, NULL, NOW(), NOW()),
('crm_opp_stage', '初步接洽', 'INITIAL', 'default', 1, 1, '10', NOW(), NOW()),
('crm_opp_stage', '需求确认', 'NEEDS', 'blue', 2, 1, '30', NOW(), NOW()),
('crm_opp_stage', '方案报价', 'PROPOSAL', 'cyan', 3, 1, '50', NOW(), NOW()),
('crm_opp_stage', '商务谈判', 'NEGOTIATION', 'purple', 4, 1, '80', NOW(), NOW()),
('crm_opp_stage', '赢单', 'WON', 'green', 5, 1, '100', NOW(), NOW()),
('crm_opp_stage', '输单', 'LOST', 'red', 6, 1, '0', NOW(), NOW()),
('crm_campaign_type', '展会', 'EXHIBITION', 'purple', 1, 1, NULL, NOW(), NOW()),
('crm_campaign_type', '线上推广', 'ONLINE', 'blue', 2, 1, NULL, NOW(), NOW()),
('crm_campaign_type', '研讨会', 'SEMINAR', 'cyan', 3, 1, NULL, NOW(), NOW()),
('crm_campaign_type', '邮件营销', 'EMAIL', 'default', 4, 1, NULL, NOW(), NOW()),
('crm_campaign_type', '促销活动', 'PROMOTION', 'orange', 5, 1, NULL, NOW(), NOW()),
('crm_campaign_status', '计划中', 'PLANNED', 'default', 1, 1, NULL, NOW(), NOW()),
('crm_campaign_status', '进行中', 'ACTIVE', 'processing', 2, 1, NULL, NOW(), NOW()),
('crm_campaign_status', '已结束', 'FINISHED', 'green', 3, 1, NULL, NOW(), NOW()),
('crm_campaign_status', '已取消', 'CANCELED', 'red', 4, 1, NULL, NOW(), NOW()),
('crm_ticket_type', '咨询', 'CONSULT', 'blue', 1, 1, NULL, NOW(), NOW()),
('crm_ticket_type', '投诉', 'COMPLAINT', 'red', 2, 1, NULL, NOW(), NOW()),
('crm_ticket_type', '报修', 'REPAIR', 'orange', 3, 1, NULL, NOW(), NOW()),
('crm_ticket_type', '退换货', 'RETURN', 'purple', 4, 1, NULL, NOW(), NOW()),
('crm_ticket_type', '建议', 'SUGGESTION', 'cyan', 5, 1, NULL, NOW(), NOW()),
('crm_ticket_priority', '紧急', 'URGENT', 'red', 1, 1, '4 小时内解决', NOW(), NOW()),
('crm_ticket_priority', '高', 'HIGH', 'orange', 2, 1, '24 小时内解决', NOW(), NOW()),
('crm_ticket_priority', '中', 'MEDIUM', 'blue', 3, 1, '72 小时内解决', NOW(), NOW()),
('crm_ticket_priority', '低', 'LOW', 'default', 4, 1, '7 天内解决', NOW(), NOW()),
('crm_ticket_status', '待处理', 'OPEN', 'orange', 1, 1, NULL, NOW(), NOW()),
('crm_ticket_status', '处理中', 'PROCESSING', 'processing', 2, 1, NULL, NOW(), NOW()),
('crm_ticket_status', '已解决', 'RESOLVED', 'green', 3, 1, NULL, NOW(), NOW()),
('crm_ticket_status', '已关闭', 'CLOSED', 'default', 4, 1, NULL, NOW(), NOW()),
('crm_ticket_channel', '电话', 'PHONE', 'default', 1, 1, NULL, NOW(), NOW()),
('crm_ticket_channel', '邮件', 'EMAIL', 'default', 2, 1, NULL, NOW(), NOW()),
('crm_ticket_channel', '在线客服', 'ONLINE', 'default', 3, 1, NULL, NOW(), NOW()),
('crm_ticket_channel', '上门', 'ONSITE', 'default', 4, 1, NULL, NOW(), NOW());

-- 普通用户：客户关系各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 9), (2, 220), (2, 221), (2, 222), (2, 223), (2, 224), (2, 225), (2, 226);

-- ---------------------------------------------------------------- 项目管理：菜单、字典

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(10,  0,  '项目管理', 'M', '/pm', NULL, NULL, 'ProjectOutlined', 6, 1, 1, NOW(), NOW()),
(240, 10, '项目列表', 'C', 'project', 'pm/project/index', 'pm:project:list', 'ProfileOutlined', 1, 1, 1, NOW(), NOW()),
(241, 10, '计划进度', 'C', 'plan', 'pm/plan/index', 'pm:task:list', 'BarsOutlined', 2, 1, 1, NOW(), NOW()),
(242, 10, '资源分配', 'C', 'resource', 'pm/resource/index', 'pm:member:list', 'TeamOutlined', 3, 1, 1, NOW(), NOW()),
(243, 10, '工时管理', 'C', 'timesheet', 'pm/timesheet/index', 'pm:timesheet:list', 'ClockCircleOutlined', 4, 1, 1, NOW(), NOW()),
(244, 10, '成本控制', 'C', 'cost', 'pm/cost/index', 'pm:cost:list', 'DollarOutlined', 5, 1, 1, NOW(), NOW()),
(245, 10, '项目核算', 'C', 'accounting', 'pm/accounting/index', 'pm:accounting:list', 'CalculatorOutlined', 6, 1, 1, NOW(), NOW()),
(2400, 240, '项目新增', 'F', NULL, NULL, 'pm:project:add', NULL, 1, 1, 1, NOW(), NOW()),
(2401, 240, '项目修改', 'F', NULL, NULL, 'pm:project:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2402, 240, '项目删除', 'F', NULL, NULL, 'pm:project:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2410, 241, '任务维护', 'F', NULL, NULL, 'pm:task:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2420, 242, '成员维护', 'F', NULL, NULL, 'pm:member:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2430, 243, '工时审批', 'F', NULL, NULL, 'pm:timesheet:approve', NULL, 1, 1, 1, NOW(), NOW()),
(2440, 244, '费用录入', 'F', NULL, NULL, 'pm:cost:add', NULL, 1, 1, 1, NOW(), NOW()),
(2441, 244, '费用删除', 'F', NULL, NULL, 'pm:cost:remove', NULL, 2, 1, 1, NOW(), NOW()),
(2442, 244, '预算编制', 'F', NULL, NULL, 'pm:cost:budget', NULL, 3, 1, 1, NOW(), NOW()),
(2443, 244, '项目领料', 'F', NULL, NULL, 'pm:cost:issue', NULL, 4, 1, 1, NOW(), NOW()),
(2450, 245, '收款计划维护', 'F', NULL, NULL, 'pm:billing:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2459, 245, '项目月结', 'F', NULL, NULL, 'pm:accounting:close', NULL, 9, 1, 1, NOW(), NOW()),
(2451, 245, '开票生成应收', 'F', NULL, NULL, 'pm:billing:bill', NULL, 2, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(85, '项目类型', 'pm_project_type', 1, '项目的类型', NOW(), NOW()),
(86, '项目状态', 'pm_project_status', 1, '项目的状态', NOW(), NOW()),
(87, '项目角色', 'pm_member_role', 1, '项目成员的角色', NOW(), NOW()),
(88, '任务状态', 'pm_task_status', 1, '项目任务的状态', NOW(), NOW()),
(89, '工时状态', 'pm_timesheet_status', 1, '工时的审批状态', NOW(), NOW()),
(90, '成本类型', 'pm_cost_type', 1, '项目成本的类型', NOW(), NOW()),
(91, '收款状态', 'pm_billing_status', 1, '项目收款计划的状态', NOW(), NOW()),
(92, '优先级', 'pm_priority', 1, '项目优先级', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time) VALUES
('pm_project_type', '研发项目', 'RD', 'blue', 1, 1, NULL, NOW(), NOW()),
('pm_project_type', '交付实施', 'DELIVERY', 'green', 2, 1, NULL, NOW(), NOW()),
('pm_project_type', '工程项目', 'ENGINEERING', 'purple', 3, 1, NULL, NOW(), NOW()),
('pm_project_type', '内部改进', 'INTERNAL', 'default', 4, 1, NULL, NOW(), NOW()),
('pm_project_status', '规划中', 'PLANNING', 'default', 1, 1, NULL, NOW(), NOW()),
('pm_project_status', '进行中', 'ACTIVE', 'processing', 2, 1, NULL, NOW(), NOW()),
('pm_project_status', '已暂停', 'SUSPENDED', 'orange', 3, 1, NULL, NOW(), NOW()),
('pm_project_status', '已完成', 'FINISHED', 'green', 4, 1, NULL, NOW(), NOW()),
('pm_project_status', '已关闭', 'CLOSED', 'default', 5, 1, NULL, NOW(), NOW()),
('pm_member_role', '项目经理', 'MANAGER', 'gold', 1, 1, NULL, NOW(), NOW()),
('pm_member_role', '开发', 'DEVELOPER', 'blue', 2, 1, NULL, NOW(), NOW()),
('pm_member_role', '测试', 'TESTER', 'cyan', 3, 1, NULL, NOW(), NOW()),
('pm_member_role', '实施', 'IMPLEMENTER', 'green', 4, 1, NULL, NOW(), NOW()),
('pm_member_role', '工程师', 'ENGINEER', 'purple', 5, 1, NULL, NOW(), NOW()),
('pm_member_role', '顾问', 'CONSULTANT', 'default', 6, 1, NULL, NOW(), NOW()),
('pm_task_status', '未开始', 'TODO', 'default', 1, 1, NULL, NOW(), NOW()),
('pm_task_status', '进行中', 'DOING', 'processing', 2, 1, NULL, NOW(), NOW()),
('pm_task_status', '已完成', 'DONE', 'green', 3, 1, NULL, NOW(), NOW()),
('pm_timesheet_status', '待审批', 'SUBMITTED', 'orange', 1, 1, NULL, NOW(), NOW()),
('pm_timesheet_status', '已通过', 'APPROVED', 'green', 2, 1, NULL, NOW(), NOW()),
('pm_timesheet_status', '已驳回', 'REJECTED', 'red', 3, 1, NULL, NOW(), NOW()),
('pm_cost_type', '人工', 'LABOR', 'blue', 1, 1, '来自审批通过的工时', NOW(), NOW()),
('pm_cost_type', '材料', 'MATERIAL', 'cyan', 2, 1, NULL, NOW(), NOW()),
('pm_cost_type', '差旅', 'TRAVEL', 'orange', 3, 1, NULL, NOW(), NOW()),
('pm_cost_type', '外包', 'OUTSOURCE', 'purple', 4, 1, NULL, NOW(), NOW()),
('pm_cost_type', '设备', 'EQUIPMENT', 'geekblue', 5, 1, NULL, NOW(), NOW()),
('pm_cost_type', '其他', 'OTHER', 'default', 6, 1, NULL, NOW(), NOW()),
('pm_billing_status', '待开票', 'PLANNED', 'orange', 1, 1, NULL, NOW(), NOW()),
('pm_billing_status', '已开票', 'BILLED', 'green', 2, 1, NULL, NOW(), NOW()),
('pm_priority', '高', 'HIGH', 'red', 1, 1, NULL, NOW(), NOW()),
('pm_priority', '中', 'MEDIUM', 'blue', 2, 1, NULL, NOW(), NOW()),
('pm_priority', '低', 'LOW', 'default', 3, 1, NULL, NOW(), NOW());

-- 普通用户：项目管理各页面只读；作为项目成员可以填报自己的工时、更新自己负责的任务进度
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 10), (2, 240), (2, 241), (2, 242), (2, 243), (2, 244), (2, 245);

-- ---------------------------------------------------------------- 商务智能：菜单、字典、预警规则

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(11,  0,  '商务智能', 'M', '/bi', NULL, NULL, 'DotChartOutlined', 8, 1, 1, NOW(), NOW()),
(260, 11, '经营驾驶舱', 'C', 'cockpit', 'bi/cockpit/index', 'bi:cockpit:list', 'DashboardOutlined', 1, 1, 1, NOW(), NOW()),
(261, 11, '多维分析', 'C', 'olap', 'bi/olap/index', 'bi:olap:list', 'AppstoreOutlined', 2, 1, 1, NOW(), NOW()),
(262, 11, '自定义报表', 'C', 'report', 'bi/report/index', 'bi:report:list', 'TableOutlined', 3, 1, 1, NOW(), NOW()),
(263, 11, '自定义仪表盘', 'C', 'board', 'bi/board/index', 'bi:board:list', 'LayoutOutlined', 4, 1, 1, NOW(), NOW()),
(264, 11, '决策支持', 'C', 'decision', 'bi/decision/index', 'bi:decision:list', 'ExperimentOutlined', 5, 1, 1, NOW(), NOW()),
(265, 11, '数据仓库', 'C', 'etl', 'bi/etl/index', 'bi:etl:list', 'DatabaseOutlined', 6, 1, 1, NOW(), NOW()),
(2620, 262, '报表新增', 'F', NULL, NULL, 'bi:report:add', NULL, 1, 1, 1, NOW(), NOW()),
(2621, 262, '报表修改', 'F', NULL, NULL, 'bi:report:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2622, 262, '报表删除', 'F', NULL, NULL, 'bi:report:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2630, 263, '仪表盘新增', 'F', NULL, NULL, 'bi:board:add', NULL, 1, 1, 1, NOW(), NOW()),
(2631, 263, '仪表盘修改', 'F', NULL, NULL, 'bi:board:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2632, 263, '仪表盘删除', 'F', NULL, NULL, 'bi:board:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2640, 264, '预警规则维护', 'F', NULL, NULL, 'bi:alert:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2650, 265, '立即同步', 'F', NULL, NULL, 'bi:etl:run', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(95, '同步状态', 'bi_etl_status', 1, '数据仓库同步任务的状态', NOW(), NOW()),
(96, '预警级别', 'bi_alert_level', 1, '预警规则的级别', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time) VALUES
('bi_etl_status', '运行中', 'RUNNING', 'processing', 1, 1, NULL, NOW(), NOW()),
('bi_etl_status', '成功', 'SUCCESS', 'green', 2, 1, NULL, NOW(), NOW()),
('bi_etl_status', '失败', 'FAILED', 'red', 3, 1, NULL, NOW(), NOW()),
('bi_alert_level', '提示', 'INFO', 'blue', 1, 1, NULL, NOW(), NOW()),
('bi_alert_level', '警告', 'WARN', 'orange', 2, 1, NULL, NOW(), NOW()),
('bi_alert_level', '严重', 'DANGER', 'red', 3, 1, NULL, NOW(), NOW());

-- 普通用户：商务智能各页面只读
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 11), (2, 260), (2, 261), (2, 262), (2, 263), (2, 264), (2, 265);

INSERT INTO bi_alert_rule (id, name, metric, operator, threshold, level, enabled, remark, create_time, update_time) VALUES
(1, '应收账款逾期', 'AR_OVERDUE', 'GT', 0, 'WARN', 1, '催收逾期款项，必要时暂停发货', NOW(), NOW()),
(2, '库存低于安全库存', 'STOCK_BELOW_SAFETY', 'GT', 0, 'WARN', 1, '及时补货或运行 MRP', NOW(), NOW()),
(3, '呆滞物料', 'SLOW_MOVING', 'GT', 0, 'INFO', 1, '90 天没有出库的物料，考虑促销或调拨', NOW(), NOW()),
(4, '本月毛利率偏低', 'GROSS_MARGIN', 'LT', 15, 'DANGER', 1, '检查销售价格与采购成本', NOW(), NOW()),
(5, '服务工单超时', 'TICKET_OVERDUE', 'GT', 0, 'DANGER', 1, '超过处理时限未解决的工单', NOW(), NOW()),
(6, '项目任务逾期', 'TASK_OVERDUE', 'GT', 0, 'WARN', 1, '超过计划完成日期未完成的任务', NOW(), NOW()),
(7, '生产工单延期', 'WO_LATE', 'GT', 0, 'WARN', 1, '超过计划完工日期未完工的工单', NOW(), NOW()),
(8, '预算超支', 'BUDGET_OVERRUN', 'GT', 0, 'WARN', 1, '本年累计执行超过预算的科目数', NOW(), NOW());

-- ---------------------------------------------------------------- 模块联动：凭证来源、应收来源、出库类型、订单来源、流程分类、个税科目

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, remark, create_time, update_time) VALUES
('fin_voucher_source', '工资', 'SALARY', 'magenta', 10, 1, NULL, NOW(), NOW()),
('fin_bill_source', '项目开票', 'PROJECT', 'purple', 6, 1, NULL, NOW(), NOW()),
('wms_out_type', '项目领料', 'PROJECT', 'purple', 5, 1, NULL, NOW(), NOW()),
('scm_order_source', '商机', 'CRM', 'green', 3, 1, NULL, NOW(), NOW()),
('wf_category', '人事', 'HR', 'magenta', 6, 1, NULL, NOW(), NOW());

INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(64, '222103', '应交个人所得税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

-- 账务修正：委托加工物资、合同履约成本科目；委外出入库类型；期初余额菜单
INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(65, '1408', '委托加工物资', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()),
(66, '5401', '合同履约成本', 0, 'COST', 'DEBIT', 1, NOW(), NOW()),
(67, '2205', '合同负债', 0, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(68, '660205', '培训费', 56, 'PROFIT', 'DEBIT', 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_out_type', '委外发料', 'OUTSOURCE', 'magenta', 6, 1, NOW(), NOW()),
('wms_in_type', '委外收回', 'OUTSOURCE_RETURN', 'magenta', 6, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(192, 936, '期初余额', 'C', 'opening', 'fin/opening/index', 'fin:opening:list', 'FlagOutlined', 3, 1, 1, NOW(), NOW()),
(1920, 192, '期初修改', 'F', NULL, NULL, 'fin:opening:edit', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 192);

-- 加班补偿方式
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(100, '加班补偿方式', 'hr_overtime_comp', 1, '加班发加班费或转调休', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('hr_overtime_comp', '加班费', 'PAY', 'blue', 1, 1, NOW(), NOW()),
('hr_overtime_comp', '转调休', 'TO_LEAVE', 'cyan', 2, 1, NOW(), NOW());

-- 站内消息类别
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(101, '消息类别', 'sys_message_category', 1, '站内消息的类别', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_message_category', '审批', 'APPROVAL', 'blue', 1, 1, NOW(), NOW()),
('sys_message_category', '提醒', 'REMIND', 'orange', 2, 1, NOW(), NOW()),
('sys_message_category', '预警', 'ALERT', 'red', 3, 1, NOW(), NOW()),
('sys_message_category', '系统', 'SYSTEM', 'default', 4, 1, NOW(), NOW());

-- 内置流程示例：未发布，业务单据仍直接审核；在「流程管理」中按需调整并发布后启用
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('scm_purchase_order', '采购订单审批', 'PURCHASE', 1, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按订单金额", "next": {"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 5 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "50000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 金额 ≥ 5 万元时总经理 → 抄送会计。发布后采购订单「审核」改为提交审批，审批通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('scm_sales_order', '销售订单审批', 'SALES', 2, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "销售主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按订单金额", "next": {"id": "cc_wh", "type": "CC", "name": "抄送仓库管理员", "assignee": {"type": "ROLE", "ids": [6], "names": ["仓库管理员"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 20 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "200000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：销售主管 → 金额 ≥ 20 万元时总经理 → 抄送仓库。发布后销售订单「审核」改为提交审批（先校验信用额度），审批通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('mes_supplement', '补料申请审批', 'PRODUCTION', 3, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "车间主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "reason_check", "type": "CONDITION", "name": "按补料原因和数量", "next": {"id": "cc_wh", "type": "CC", "name": "抄送仓库管理员", "assignee": {"type": "ROLE", "ids": [6], "names": ["仓库管理员"]}, "next": null}, "branches": [{"id": "reason_check_yes", "name": "报废 / 来料不良或数量 ≥ 100", "defaultBranch": false, "conditions": [[{"field": "reason", "operator": "IN", "value": "SCRAP,DEFECT"}], [{"field": "totalQty", "operator": "GE", "value": "100"}]], "next": {"id": "leader2", "type": "APPROVER", "name": "生产经理审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "reason_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：车间主管 → 报废或来料不良、数量 ≥ 100 时生产经理 → 抄送仓库。发布后补料申请需审批，通过后待发料；可在流程设计中调整审批人后发布', NOW(), NOW()),
('hr_leave', '请假加班审批', 'HR', 4, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "days_check", "type": "CONDITION", "name": "按请假天数", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "days_check_yes", "name": "请假 ≥ 3 天", "defaultBranch": false, "conditions": [[{"field": "kind", "operator": "EQ", "value": "LEAVE"}, {"field": "days", "operator": "GE", "value": "3"}]], "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "days_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 请假 ≥ 3 天时上级主管 → 抄送人事。发布后请假、加班申请需审批；可在流程设计中调整审批人后发布', NOW(), NOW()),
('hr_recruit', '招聘需求审批', 'HR', 5, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "hr", "type": "APPROVER", "name": "人事专员审核", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}}}', '内置流程（未发布）：部门主管 → 人事专员 → 总经理。发布后招聘需求需审批；可在流程设计中调整审批人后发布', NOW(), NOW()),
('hr_change', '人事异动审批', 'HR', 6, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "hr", "type": "APPROVER", "name": "人事专员审核", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "type_check", "type": "CONDITION", "name": "按异动类型", "next": {"id": "cc_gm", "type": "CC", "name": "抄送总经理", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "next": null}, "branches": [{"id": "type_check_yes", "name": "调薪 ≥ 2000 元或离职", "defaultBranch": false, "conditions": [[{"field": "type", "operator": "EQ", "value": "SALARY"}, {"field": "salaryIncrease", "operator": "GE", "value": "2000"}], [{"field": "type", "operator": "EQ", "value": "LEAVE"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "type_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：部门主管 → 人事专员 → 调薪 ≥ 2000 元或离职时总经理。发布后转正、调岗、调薪、离职需审批，通过后生效；可在流程设计中调整审批人后发布', NOW(), NOW()),
('hr_payroll', '工资表审批', 'HR', 7, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "acct", "type": "APPROVER", "name": "会计复核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}}}}}', '内置流程（未发布）：会计复核 → 总经理 → 抄送出纳。发布后工资表确认需审批，通过后确认并生成计提凭证；可在流程设计中调整审批人后发布', NOW(), NOW()),
('fin_voucher', '凭证审批', 'FINANCE', 8, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "财务主管审核", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}', '内置流程（未发布）：财务主管审核。发布后凭证不能直接审核，由制单人提交审批，通过后变为已审核；可在流程设计中调整审批人后发布', NOW(), NOW()),
('fin_payment', '付款审批', 'FINANCE', 9, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "财务主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按付款金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 10 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "100000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：财务主管 → 金额 ≥ 10 万元时总经理 → 抄送出纳。发布后付款单需审批，通过后核销应付单；可在流程设计中调整审批人后发布', NOW(), NOW()),
('pm_project', '项目立项审批', 'PROJECT', 10, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计预算评审", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按合同金额", "next": null, "branches": [{"id": "amount_check_yes", "name": "合同 ≥ 50 万元", "defaultBranch": false, "conditions": [[{"field": "contractAmount", "operator": "GE", "value": "500000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：部门主管 → 会计（预算评审）→ 合同 ≥ 50 万元时总经理。发布后规划中的项目启动需审批；可在流程设计中调整审批人后发布', NOW(), NOW()),
('pm_cost', '项目费用审批', 'PROJECT', 11, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按费用金额", "next": {"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 2 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "20000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 金额 ≥ 2 万元时总经理 → 抄送会计。发布后手工录入的项目费用需审批，通过后计入项目成本；可在流程设计中调整审批人后发布', NOW(), NOW()),
('scm_requisition', '请购单审批', 'PURCHASE', 12, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_buyer", "type": "CC", "name": "抄送采购员", "assignee": {"type": "ROLE", "ids": [4], "names": ["采购员"]}, "next": null}}}}', '内置流程（未发布）：部门主管 → 抄送采购员。发布后请购单提交需审批，批准后才能生成采购订单；可在流程设计中调整审批人后发布', NOW(), NOW()),
('scm_quotation', '销售报价审批', 'SALES', 13, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "销售主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "discount_check", "type": "CONDITION", "name": "按折扣和金额", "next": null, "branches": [{"id": "discount_check_yes", "name": "低于 85 折或金额 ≥ 50 万元", "defaultBranch": false, "conditions": [[{"field": "discountRate", "operator": "LT", "value": "85"}], [{"field": "totalAmount", "operator": "GE", "value": "500000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "discount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：销售主管 → 低于 85 折或金额 ≥ 50 万元时总经理。发布后报价单提交需审批，流程变量 totalAmount 报价金额、discountRate 最低折扣率（报价 / 价目价 × 100），可按折扣设置条件分支', NOW(), NOW()),
('wms_check', '盘点差异审批', 'INVENTORY', 14, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "仓库主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_gm", "type": "CC", "name": "抄送总经理", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "next": null}}}}}', '内置流程（未发布）：仓库主管 → 会计。发布后盘点单确认需审批，通过后才调整库存；流程变量 gainQty 盘盈数量、lossQty 盘亏数量、diffLines 差异行数', NOW(), NOW()),
('fin_expense', '费用报销审批', 'FINANCE', 15, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按报销金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 5000 元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "5000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：部门主管 → 会计 → 金额 ≥ 5000 元时总经理 → 抄送出纳。发布后费用报销提交需审批，批准后待付款；流程变量 amount 报销金额', NOW(), NOW()),
('fin_loan', '员工借款审批', 'FINANCE', 16, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按借款金额", "next": {"id": "cc_cashier", "type": "CC", "name": "抄送出纳", "assignee": {"type": "ROLE", "ids": [10], "names": ["出纳"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 1 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "10000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：部门主管 → 会计 → 金额 ≥ 1 万元时总经理 → 抄送出纳。发布后员工借款提交需审批，批准后待付款；流程变量 amount 借款金额', NOW(), NOW()),
('crm_contract', '销售合同审批', 'SALES', 17, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "销售主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "acct", "type": "APPROVER", "name": "会计合同评审", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按合同金额", "next": null, "branches": [{"id": "amount_check_yes", "name": "金额 ≥ 30 万元", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "300000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}', '内置流程（未发布）：销售主管 → 会计（合同评审）→ 金额 ≥ 30 万元时总经理。发布后合同提交需审批，通过后合同生效；流程变量 amount 合同金额', NOW(), NOW()),
('scm_order_change', '订单变更审批', 'SALES', 18, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按金额变化", "next": null, "branches": [{"id": "amount_check_yes", "name": "增加或减少 ≥ 5 万元", "defaultBranch": false, "conditions": [[{"field": "amountDelta", "operator": "GE", "value": "50000"}], [{"field": "amountDelta", "operator": "LE", "value": "-50000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 金额变化 ≥ 5 万元时总经理。发布后已审核订单的变更单需审批，通过后生效；流程变量 kind 订单种类、amountDelta 金额变化', NOW(), NOW()),
('wms_stock_doc', '出入库审批', 'INVENTORY', 19, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "仓库主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "biz_check", "type": "CONDITION", "name": "按业务类型", "next": null, "branches": [{"id": "biz_check_yes", "name": "报废、其他出入库", "defaultBranch": false, "conditions": [[{"field": "bizType", "operator": "IN", "value": "SCRAP,SCRAP_IN,OTHER"}]], "next": {"id": "acct", "type": "APPROVER", "name": "会计审核", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "biz_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：仓库主管 → 报废、其他出入库时会计。发布后手工出入库单、调拨单确认前需审批，通过后更新库存；流程变量 kind（IN / OUT / TRANSFER）、bizType、totalQty', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wf_category', '项目', 'PROJECT', 'geekblue', 8, 1, NOW(), NOW()),
('hr_payroll_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('fin_voucher_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('pm_project_status', '立项审批中', 'APPROVING', 'processing', 1, 1, NOW(), NOW()),
('sys_approve_status', '已生效', 'APPROVED', 'green', 1, 1, NOW(), NOW()),
('sys_approve_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('sys_approve_status', '已拒绝', 'REJECTED', 'red', 3, 1, NOW(), NOW()),
('sys_approve_status', '已撤回', 'CANCELED', 'default', 4, 1, NOW(), NOW()),
('sys_approve_status', '已生效', 'DONE', 'green', 5, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(102, '审批状态', 'sys_approve_status', 1, '需审批生效的业务记录（人事异动、付款、项目费用）的状态', NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(103, '冻结状态', 'wms_hold_status', 1, '库存冻结（来料检验）的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_hold_status', '待检', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('wms_hold_status', '不合格', 'REJECTED', 'red', 2, 1, NOW(), NOW()),
('wms_hold_status', '已放行', 'RELEASED', 'green', 3, 1, NOW(), NOW()),
('wms_hold_status', '已退货', 'RETURNED', 'default', 4, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '项目', 'PROJECT', 'geekblue', 11, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_bill_source', '运费', 'SHIPMENT', 'cyan', 7, 1, NOW(), NOW()),
('fin_voucher_source', '费用', 'EXPENSE', 'orange', 12, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_out_type', '服务领用', 'SERVICE', 'gold', 7, 1, NOW(), NOW()),
('fin_bill_source', '服务收费', 'TICKET', 'gold', 8, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(267, 901, '请购单', 'C', 'requisition', 'scm/requisition/index', 'scm:requisition:list', 'FormOutlined', 1, 1, 1, NOW(), NOW()),
(268, 901, '询价比价', 'C', 'inquiry', 'scm/inquiry/index', 'scm:inquiry:list', 'DiffOutlined', 2, 1, 1, NOW(), NOW()),
(269, 902, '销售报价', 'C', 'quotation', 'scm/quotation/index', 'scm:quotation:list', 'FileTextOutlined', 1, 1, 1, NOW(), NOW()),
(270, 902, '销售价目表', 'C', 'price-list', 'scm/priceList/index', 'scm:price:list', 'TagsOutlined', 2, 1, 1, NOW(), NOW()),
(2670, 267, '请购单新增', 'F', NULL, NULL, 'scm:requisition:add', NULL, 1, 1, 1, NOW(), NOW()),
(2671, 267, '请购单修改', 'F', NULL, NULL, 'scm:requisition:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2672, 267, '请购单删除', 'F', NULL, NULL, 'scm:requisition:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2673, 267, '生成采购订单', 'F', NULL, NULL, 'scm:requisition:order', NULL, 4, 1, 1, NOW(), NOW()),
(2680, 268, '询价单新增', 'F', NULL, NULL, 'scm:inquiry:add', NULL, 1, 1, 1, NOW(), NOW()),
(2681, 268, '询价单修改', 'F', NULL, NULL, 'scm:inquiry:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2682, 268, '询价单删除', 'F', NULL, NULL, 'scm:inquiry:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2690, 269, '报价单新增', 'F', NULL, NULL, 'scm:quotation:add', NULL, 1, 1, 1, NOW(), NOW()),
(2691, 269, '报价单修改', 'F', NULL, NULL, 'scm:quotation:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2692, 269, '报价单删除', 'F', NULL, NULL, 'scm:quotation:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2693, 269, '转销售订单', 'F', NULL, NULL, 'scm:quotation:order', NULL, 4, 1, 1, NOW(), NOW()),
(2700, 270, '价目表维护', 'F', NULL, NULL, 'scm:price:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 267), (2, 268), (2, 269), (2, 270);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(104, '请购单状态', 'scm_requisition_status', 1, '请购单的状态', NOW(), NOW()),
(105, '询价单状态', 'scm_inquiry_status', 1, '询价单的状态', NOW(), NOW()),
(106, '报价单状态', 'scm_quotation_status', 1, '销售报价单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_requisition_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('scm_requisition_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_requisition_status', '已批准', 'APPROVED', 'blue', 3, 1, NOW(), NOW()),
('scm_requisition_status', '已下单', 'ORDERED', 'green', 4, 1, NOW(), NOW()),
('scm_requisition_status', '已关闭', 'CLOSED', 'default', 5, 1, NOW(), NOW()),
('scm_inquiry_status', '询价中', 'QUOTING', 'processing', 1, 1, NOW(), NOW()),
('scm_inquiry_status', '已定标', 'FINISHED', 'green', 2, 1, NOW(), NOW()),
('scm_inquiry_status', '已关闭', 'CLOSED', 'default', 3, 1, NOW(), NOW()),
('scm_quotation_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('scm_quotation_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_quotation_status', '已批准', 'APPROVED', 'blue', 3, 1, NOW(), NOW()),
('scm_quotation_status', '已成交', 'ORDERED', 'green', 4, 1, NOW(), NOW()),
('scm_quotation_status', '已失效', 'LOST', 'red', 5, 1, NOW(), NOW()),
('scm_order_source', '请购单', 'REQUISITION', 'cyan', 5, 1, NOW(), NOW()),
('scm_order_source', '询价单', 'INQUIRY', 'geekblue', 6, 1, NOW(), NOW()),
('scm_order_source', '销售报价', 'QUOTATION', 'magenta', 7, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(271, 902, '发货通知', 'C', 'delivery', 'scm/delivery/index', 'scm:delivery:list', 'SendOutlined', 4, 1, 1, NOW(), NOW()),
(272, 902, '欠交清单', 'C', 'backorder', 'scm/backorder/index', 'scm:backorder:list', 'FieldTimeOutlined', 8, 1, 1, NOW(), NOW()),
(273, 913, '库存预留', 'C', 'reservation', 'wms/reservation/index', 'wms:reservation:list', 'PushpinOutlined', 1, 1, 1, NOW(), NOW()),
(1435, 143, '预留库存', 'F', NULL, NULL, 'scm:sales:reserve', NULL, 6, 1, 1, NOW(), NOW()),
(2710, 271, '发货通知新增', 'F', NULL, NULL, 'scm:delivery:add', NULL, 1, 1, 1, NOW(), NOW()),
(2711, 271, '拣货', 'F', NULL, NULL, 'scm:delivery:pick', NULL, 2, 1, 1, NOW(), NOW()),
(2730, 273, '释放预留', 'F', NULL, NULL, 'wms:reservation:release', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 271), (2, 272), (2, 273);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(107, '发货通知状态', 'scm_delivery_status', 1, '发货通知单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_delivery_status', '待拣货', 'PENDING', 'orange', 1, 1, NOW(), NOW()),
('scm_delivery_status', '已拣货', 'PICKED', 'blue', 2, 1, NOW(), NOW()),
('scm_delivery_status', '已出库', 'SHIPPED', 'green', 3, 1, NOW(), NOW()),
('scm_delivery_status', '已取消', 'CANCELLED', 'default', 4, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1304, 130, '入库冲销', 'F', NULL, NULL, 'wms:in:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1314, 131, '出库冲销', 'F', NULL, NULL, 'wms:out:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1324, 132, '调拨冲销', 'F', NULL, NULL, 'wms:transfer:reverse', NULL, 5, 1, 1, NOW(), NOW()),
(1325, 132, '调拨收货', 'F', NULL, NULL, 'wms:transfer:receive', NULL, 6, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(108, '仓库类型', 'wms_warehouse_type', 1, '不良品仓的库存不计入 MRP、齐套、可承诺量', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_warehouse_type', '普通仓', 'NORMAL', 'default', 1, 1, NOW(), NOW()),
('wms_warehouse_type', '原料仓', 'RAW', 'cyan', 2, 1, NOW(), NOW()),
('wms_warehouse_type', '成品仓', 'FINISHED', 'green', 3, 1, NOW(), NOW()),
('wms_warehouse_type', '不良品仓', 'DEFECT', 'red', 4, 1, NOW(), NOW()),
('wms_doc_status', '在途', 'TRANSIT', 'blue', 3, 1, NOW(), NOW()),
('wms_doc_status', '审批中', 'APPROVING', 'processing', 4, 1, NOW(), NOW()),
('wms_out_type', '报废出库', 'SCRAP', 'red', 8, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(274, 901, '补货建议', 'C', 'reorder', 'scm/reorder/index', 'scm:reorder:list', 'AlertOutlined', 6, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 274);

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(275, 921, '产能排程', 'C', 'capacity', 'mes/capacity/index', 'mes:capacity:list', 'ScheduleOutlined', 3, 1, 1, NOW(), NOW()),
(2750, 275, '排程与日历', 'F', NULL, NULL, 'mes:capacity:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1627, 162, '改量与拆分', 'F', NULL, NULL, 'mes:wo:change', NULL, 8, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 275);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(109, '工单类型', 'mes_wo_type', 1, '普通工单、返工工单', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('mes_wo_type', '普通', 'NORMAL', 'default', 1, 1, NOW(), NOW()),
('mes_wo_type', '返工', 'REWORK', 'orange', 2, 1, NOW(), NOW()),
('mes_wo_doc_kind', '报废入库', 'SCRAP', 'default', 5, 1, NOW(), NOW()),
('wms_in_type', '报废入库', 'SCRAP_IN', 'red', 8, 1, NOW(), NOW());

INSERT INTO fin_account (id, code, name, parent_id, category, direction, status, create_time, update_time) VALUES
(69, '222104', '未交增值税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(70, '222105', '应交城市维护建设税', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(71, '222106', '应交教育费附加', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW()),
(72, '222107', '应交地方教育附加', 24, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(276, 932, '发票管理', 'C', 'invoice', 'fin/invoice/index', 'fin:invoice:list', 'ProfileOutlined', 6, 1, 1, NOW(), NOW()),
(277, 932, '预收预付', 'C', 'advance', 'fin/advance/index', 'fin:advance:list', 'WalletOutlined', 5, 1, 1, NOW(), NOW()),
(278, 933, '银行对账', 'C', 'bank', 'fin/bank/index', 'fin:bank:list', 'BankOutlined', 3, 1, 1, NOW(), NOW()),
(279, 933, '费用报销', 'C', 'claim', 'fin/claim/index', 'fin:claim:list', 'AccountBookOutlined', 1, 1, 1, NOW(), NOW()),
(280, 933, '员工借款', 'C', 'loan', 'fin/loan/index', 'fin:loan:list', 'MoneyCollectOutlined', 2, 1, 1, NOW(), NOW()),
(281, 935, '经营分析', 'C', 'analysis', 'fin/analysis/index', 'fin:analysis:list', 'BarChartOutlined', 2, 1, 1, NOW(), NOW()),
(2760, 276, '发票登记', 'F', NULL, NULL, 'fin:invoice:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2770, 277, '预收预付登记', 'F', NULL, NULL, 'fin:advance:add', NULL, 1, 1, 1, NOW(), NOW()),
(2780, 278, '导入与勾对', 'F', NULL, NULL, 'fin:bank:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2790, 279, '报销新增', 'F', NULL, NULL, 'fin:claim:add', NULL, 1, 1, 1, NOW(), NOW()),
(2791, 279, '报销付款', 'F', NULL, NULL, 'fin:claim:pay', NULL, 2, 1, 1, NOW(), NOW()),
(2800, 280, '借款新增', 'F', NULL, NULL, 'fin:loan:add', NULL, 1, 1, 1, NOW(), NOW()),
(2801, 280, '借款付款与还款', 'F', NULL, NULL, 'fin:loan:pay', NULL, 2, 1, 1, NOW(), NOW()),
(1834, 183, '坏账核销', 'F', NULL, NULL, 'fin:ar:baddebt', NULL, 9, 1, 1, NOW(), NOW()),
(1805, 180, '红字冲销', 'F', NULL, NULL, 'fin:voucher:reverse', NULL, 9, 1, 1, NOW(), NOW()),
(1912, 191, '增值税结转', 'F', NULL, NULL, 'fin:period:tax', NULL, 9, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 276), (2, 277), (2, 278), (2, 279), (2, 280), (2, 281);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(110, '发票类型', 'fin_invoice_type', 1, '增值税专用发票、普通发票', NOW(), NOW()),
(111, '报销状态', 'fin_claim_status', 1, '费用报销单的状态', NOW(), NOW()),
(112, '借款状态', 'fin_loan_status', 1, '员工借款的状态', NOW(), NOW()),
(113, '预算控制', 'fin_budget_control', 1, '费用报销超预算时的处理', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_invoice_type', '增值税专用发票', 'SPECIAL', 'red', 1, 1, NOW(), NOW()),
('fin_invoice_type', '普通发票', 'NORMAL', 'blue', 2, 1, NOW(), NOW()),
('fin_invoice_type', '数电票（专票）', 'E_SPECIAL', 'volcano', 3, 1, NOW(), NOW()),
('fin_invoice_type', '数电票（普票）', 'E_NORMAL', 'geekblue', 4, 1, NOW(), NOW()),
('fin_claim_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_claim_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('fin_claim_status', '待付款', 'APPROVED', 'orange', 3, 1, NOW(), NOW()),
('fin_claim_status', '已付款', 'PAID', 'green', 4, 1, NOW(), NOW()),
('fin_loan_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_loan_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('fin_loan_status', '待付款', 'APPROVED', 'orange', 3, 1, NOW(), NOW()),
('fin_loan_status', '未还清', 'PAID', 'blue', 4, 1, NOW(), NOW()),
('fin_loan_status', '已还清', 'CLEARED', 'green', 5, 1, NOW(), NOW()),
('fin_budget_control', '不控制', 'NONE', 'default', 1, 1, NOW(), NOW()),
('fin_budget_control', '超预算提示', 'WARN', 'orange', 2, 1, NOW(), NOW()),
('fin_budget_control', '超预算禁止', 'BLOCK', 'red', 3, 1, NOW(), NOW()),
('fin_voucher_source', '预收预付', 'ADVANCE', 'cyan', 13, 1, NOW(), NOW()),
('fin_voucher_source', '坏账', 'BAD_DEBT', 'red', 14, 1, NOW(), NOW()),
('fin_voucher_source', '红字冲销', 'REVERSAL', 'red', 15, 1, NOW(), NOW()),
('fin_voucher_source', '税费', 'TAX', 'purple', 16, 1, NOW(), NOW()),
('fin_voucher_source', '报销', 'CLAIM', 'orange', 17, 1, NOW(), NOW()),
('fin_voucher_source', '借款', 'LOAN', 'gold', 18, 1, NOW(), NOW()),
('fin_bill_source', '固定资产', 'ASSET', 'purple', 9, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(282, 951, '劳动合同', 'C', 'contract', 'hr/contract/index', 'hr:contract:list', 'AuditOutlined', 3, 1, 1, NOW(), NOW()),
(283, 954, '社保方案', 'C', 'social', 'hr/social/index', 'hr:social:list', 'SafetyOutlined', 3, 1, 1, NOW(), NOW()),
(284, 954, '专项附加扣除', 'C', 'deduction', 'hr/deduction/index', 'hr:deduction:list', 'ReconciliationOutlined', 4, 1, 1, NOW(), NOW()),
(2820, 282, '合同维护', 'F', NULL, NULL, 'hr:contract:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2830, 283, '方案维护', 'F', NULL, NULL, 'hr:social:edit', NULL, 1, 1, 1, NOW(), NOW()),
(2840, 284, '扣除维护', 'F', NULL, NULL, 'hr:deduction:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 282), (2, 283), (2, 284);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(114, '合同类型', 'hr_contract_type', 1, '劳动合同的类型', NOW(), NOW()),
(115, '合同状态', 'hr_contract_status', 1, '劳动合同的状态', NOW(), NOW()),
(116, '专项附加扣除', 'hr_deduction_type', 1, '个税专项附加扣除项目', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('hr_contract_type', '固定期限', 'FIXED', 'blue', 1, 1, NOW(), NOW()),
('hr_contract_type', '无固定期限', 'OPEN', 'green', 2, 1, NOW(), NOW()),
('hr_contract_type', '以完成一定工作任务为期限', 'TASK', 'cyan', 3, 1, NOW(), NOW()),
('hr_contract_type', '实习协议', 'INTERN', 'orange', 4, 1, NOW(), NOW()),
('hr_contract_type', '劳务协议', 'LABOR', 'purple', 5, 1, NOW(), NOW()),
('hr_contract_status', '生效中', 'ACTIVE', 'green', 1, 1, NOW(), NOW()),
('hr_contract_status', '已到期', 'EXPIRED', 'orange', 2, 1, NOW(), NOW()),
('hr_contract_status', '已续签', 'RENEWED', 'blue', 3, 1, NOW(), NOW()),
('hr_contract_status', '已终止', 'TERMINATED', 'default', 4, 1, NOW(), NOW()),
('hr_deduction_type', '子女教育', 'CHILD_EDU', 'blue', 1, 1, NOW(), NOW()),
('hr_deduction_type', '继续教育', 'CONTINUING_EDU', 'cyan', 2, 1, NOW(), NOW()),
('hr_deduction_type', '住房贷款利息', 'HOUSING_LOAN', 'green', 3, 1, NOW(), NOW()),
('hr_deduction_type', '住房租金', 'HOUSING_RENT', 'gold', 4, 1, NOW(), NOW()),
('hr_deduction_type', '赡养老人', 'ELDERLY', 'purple', 5, 1, NOW(), NOW()),
('hr_deduction_type', '婴幼儿照护', 'INFANT', 'magenta', 6, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(285, 941, '合同管理', 'C', 'contract', 'crm/contract/index', 'crm:contract:list', 'FileProtectOutlined', 5, 1, 1, NOW(), NOW()),
(2850, 285, '合同新增', 'F', NULL, NULL, 'crm:contract:add', NULL, 1, 1, 1, NOW(), NOW()),
(2851, 285, '合同修改', 'F', NULL, NULL, 'crm:contract:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2852, 285, '合同删除', 'F', NULL, NULL, 'crm:contract:remove', NULL, 3, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 285);

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(117, '合同状态', 'crm_contract_status', 1, '销售合同的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('crm_contract_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('crm_contract_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('crm_contract_status', '执行中', 'ACTIVE', 'blue', 3, 1, NOW(), NOW()),
('crm_contract_status', '已完成', 'COMPLETED', 'green', 4, 1, NOW(), NOW()),
('crm_contract_status', '已终止', 'TERMINATED', 'red', 5, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(1405, 140, '采购订单变更', 'F', NULL, NULL, 'scm:purchase:change', NULL, 6, 1, 1, NOW(), NOW()),
(1436, 143, '销售订单变更', 'F', NULL, NULL, 'scm:sales:change', NULL, 7, 1, 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(118, '变更单状态', 'scm_change_status', 1, '订单变更单的状态', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_change_status', '审批中', 'APPROVING', 'processing', 1, 1, NOW(), NOW()),
('scm_change_status', '已生效', 'APPROVED', 'green', 2, 1, NOW(), NOW()),
('scm_change_status', '已驳回', 'REJECTED', 'red', 3, 1, NOW(), NOW());

INSERT INTO fin_currency (code, name, symbol, is_base, status, sort, create_time, update_time) VALUES
('CNY', '人民币', '¥', 1, 1, 1, NOW(), NOW()),
('USD', '美元', '$', 0, 1, 2, NOW(), NOW()),
('EUR', '欧元', '€', 0, 1, 3, NOW(), NOW()),
('HKD', '港币', 'HK$', 0, 1, 4, NOW(), NOW()),
('JPY', '日元', '¥', 0, 1, 5, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(286, 936, '币种汇率', 'C', 'currency', 'fin/currency/index', 'fin:currency:list', 'DollarOutlined', 4, 1, 1, NOW(), NOW()),
(2860, 286, '汇率维护与期末调汇', 'F', NULL, NULL, 'fin:currency:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 286);
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_voucher_source', '汇兑', 'FX', 'geekblue', 19, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(287, 902, '寄售结算', 'C', 'consign', 'scm/consign/index', 'scm:consign:list', 'SwapOutlined', 9, 1, 1, NOW(), NOW()),
(2870, 287, '寄售结算', 'F', NULL, NULL, 'scm:consign:settle', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 287);
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_warehouse_type', '客户寄售仓', 'CONSIGN_OUT', 'purple', 5, 1, NOW(), NOW()),
('wms_warehouse_type', '供应商寄售仓', 'CONSIGN_IN', 'geekblue', 6, 1, NOW(), NOW()),
('wms_in_type', '寄售入库', 'CONSIGN_IN', 'geekblue', 9, 1, NOW(), NOW()),
('wms_out_type', '寄售消耗', 'CONSIGN', 'geekblue', 9, 1, NOW(), NOW()),
('scm_order_source', '寄售结算', 'CONSIGN', 'purple', 8, 1, NOW(), NOW());

-- 内置角色：按岗位预置菜单和按钮权限，可在「角色管理」中调整
INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(3, '总经理', 'gm', 3, 1, 1, '查看全部业务数据和商务智能，审核采购销售订单、招聘需求、假勤和工时', NOW(), NOW()),
(4, '采购员', 'purchaser', 4, 3, 1, '请购、询价、采购订单与收退货、供应商、寄售结算，可查看库存和物料', NOW(), NOW()),
(5, '销售员', 'sales', 5, 5, 1, '报价、销售订单、发货通知、客户与商机跟进，只能看本人的数据', NOW(), NOW()),
(6, '仓库管理员', 'warehouse', 6, 1, 1, '出入库、调拨、盘点、批次序列号、库存冻结预留，采购收货、销售出库、拣货和发运，生产领料和完工入库', NOW(), NOW()),
(7, '生产计划员', 'planner', 7, 4, 1, '生产计划、MRP、产能排程、工单、齐套、BOM 与工作中心，可提请购单', NOW(), NOW()),
(8, '车间主管', 'workshop', 8, 3, 1, '工单领料、报工、完工，补料申请、质量检验、设备与维护', NOW(), NOW()),
(9, '会计', 'accountant', 9, 1, 1, '财务全部功能：凭证、往来、资产、成本、预算、结账，可查看采购销售单据', NOW(), NOW()),
(10, '出纳', 'cashier', 10, 1, 1, '收付款、预收预付、银行对账、报销和借款付款，可查看账簿和往来', NOW(), NOW()),
(11, '人事专员', 'hr', 11, 1, 1, '人力资源全部功能：员工、合同、招聘培训、考勤、绩效、薪酬社保', NOW(), NOW()),
(12, '项目经理', 'pm', 12, 5, 1, '负责项目的计划进度、资源、工时审批、成本和核算，只能看本人负责的数据', NOW(), NOW()),
(13, '普通员工', 'staff', 13, 5, 1, '员工自助（请假加班、工资条）、发起和处理审批、费用报销和借款', NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 3), (3, 4), (3, 5), (3, 6), (3, 7), (3, 8), (3, 9), (3, 10), (3, 11), (3, 120), (3, 121), (3, 122),
(3, 123), (3, 124), (3, 130), (3, 131), (3, 132), (3, 133), (3, 134), (3, 135), (3, 136), (3, 137), (3, 138), (3, 139),
(3, 140), (3, 141), (3, 142), (3, 143), (3, 144), (3, 145), (3, 146), (3, 147), (3, 148), (3, 149), (3, 160), (3, 161),
(3, 162), (3, 163), (3, 164), (3, 165), (3, 166), (3, 167), (3, 169), (3, 170), (3, 180), (3, 181), (3, 182), (3, 183),
(3, 184), (3, 185), (3, 186), (3, 187), (3, 188), (3, 189), (3, 190), (3, 191), (3, 192), (3, 200), (3, 201), (3, 202),
(3, 203), (3, 204), (3, 205), (3, 206), (3, 207), (3, 208), (3, 209), (3, 220), (3, 221), (3, 222), (3, 223), (3, 224),
(3, 225), (3, 226), (3, 240), (3, 241), (3, 242), (3, 243), (3, 244), (3, 245), (3, 260), (3, 261), (3, 262), (3, 263),
(3, 264), (3, 265), (3, 266), (3, 267), (3, 268), (3, 269), (3, 270), (3, 271), (3, 272), (3, 273), (3, 274), (3, 275),
(3, 276), (3, 277), (3, 278), (3, 279), (3, 280), (3, 281), (3, 282), (3, 283), (3, 284), (3, 285), (3, 286), (3, 287),
(3, 1403), (3, 1433), (3, 2023), (3, 2050), (3, 2430), (3, 2620), (3, 2621), (3, 2622), (3, 2630), (3, 2631), (3, 2632), (3, 2640),
(3, 2650), (3, 2790), (3, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 120), (4, 121), (4, 122), (4, 123), (4, 124), (4, 134),
(4, 135), (4, 137), (4, 138), (4, 140), (4, 141), (4, 142), (4, 147), (4, 148), (4, 162), (4, 169), (4, 209), (4, 267),
(4, 268), (4, 274), (4, 279), (4, 280), (4, 287), (4, 1400), (4, 1401), (4, 1402), (4, 1403), (4, 1404), (4, 1405), (4, 1410), (4, 1420),
(4, 1470), (4, 1471), (4, 1472), (4, 1480), (4, 1481), (4, 2670), (4, 2671), (4, 2672), (4, 2673), (4, 2680), (4, 2681), (4, 2682),
(4, 2790), (4, 2800), (4, 2870);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(5, 3), (5, 4), (5, 5), (5, 7), (5, 8), (5, 9), (5, 120), (5, 121), (5, 122), (5, 123), (5, 124), (5, 134),
(5, 137), (5, 143), (5, 144), (5, 145), (5, 146), (5, 149), (5, 209), (5, 220), (5, 221), (5, 222), (5, 223), (5, 225),
(5, 269), (5, 270), (5, 271), (5, 272), (5, 273), (5, 279), (5, 280), (5, 285), (5, 1430), (5, 1431), (5, 1432), (5, 1433), (5, 1434),
(5, 1435), (5, 1436), (5, 1490), (5, 1491), (5, 1492), (5, 2200), (5, 2201), (5, 2202), (5, 2203), (5, 2210), (5, 2211), (5, 2212),
(5, 2213), (5, 2220), (5, 2221), (5, 2222), (5, 2223), (5, 2230), (5, 2231), (5, 2690), (5, 2691), (5, 2692), (5, 2693), (5, 2710),
(5, 2790), (5, 2800), (5, 2850), (5, 2851), (5, 2852);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 3), (6, 4), (6, 5), (6, 6), (6, 7), (6, 8), (6, 120), (6, 121), (6, 122), (6, 123), (6, 124), (6, 130),
(6, 131), (6, 132), (6, 133), (6, 134), (6, 135), (6, 136), (6, 137), (6, 138), (6, 139), (6, 140), (6, 141), (6, 142),
(6, 143), (6, 144), (6, 145), (6, 146), (6, 147), (6, 149), (6, 162), (6, 170), (6, 209), (6, 266), (6, 271), (6, 273),
(6, 279), (6, 280), (6, 287), (6, 1300), (6, 1301), (6, 1302), (6, 1303), (6, 1304), (6, 1310), (6, 1311), (6, 1312), (6, 1313),
(6, 1314), (6, 1320), (6, 1321), (6, 1322), (6, 1323), (6, 1324), (6, 1325), (6, 1330), (6, 1331), (6, 1332), (6, 1333), (6, 1370),
(6, 1371), (6, 1372), (6, 1380), (6, 1381), (6, 1382), (6, 1390), (6, 1391), (6, 1392), (6, 1410), (6, 1420), (6, 1440), (6, 1450),
(6, 1460), (6, 1461), (6, 1462), (6, 1463), (6, 1624), (6, 1626), (6, 1701), (6, 2660), (6, 2711), (6, 2730), (6, 2790), (6, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(7, 3), (7, 4), (7, 5), (7, 6), (7, 7), (7, 8), (7, 120), (7, 121), (7, 122), (7, 123), (7, 124), (7, 134),
(7, 137), (7, 140), (7, 143), (7, 160), (7, 161), (7, 162), (7, 163), (7, 166), (7, 167), (7, 169), (7, 170), (7, 209),
(7, 267), (7, 272), (7, 275), (7, 279), (7, 280), (7, 1373), (7, 1600), (7, 1601), (7, 1602), (7, 1603), (7, 1610), (7, 1611),
(7, 1620), (7, 1621), (7, 1622), (7, 1623), (7, 1627), (7, 1660), (7, 1661), (7, 1662), (7, 1670), (7, 1671), (7, 1672), (7, 1690),
(7, 1691), (7, 2670), (7, 2671), (7, 2672), (7, 2750), (7, 2790), (7, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(8, 3), (8, 4), (8, 6), (8, 7), (8, 8), (8, 120), (8, 121), (8, 122), (8, 123), (8, 124), (8, 134), (8, 137),
(8, 160), (8, 162), (8, 163), (8, 164), (8, 165), (8, 166), (8, 167), (8, 169), (8, 170), (8, 209), (8, 279), (8, 280),
(8, 1624), (8, 1625), (8, 1626), (8, 1630), (8, 1631), (8, 1632), (8, 1640), (8, 1641), (8, 1642), (8, 1650), (8, 1651), (8, 1652),
(8, 1700), (8, 2790), (8, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(9, 3), (9, 4), (9, 5), (9, 7), (9, 8), (9, 10), (9, 120), (9, 121), (9, 122), (9, 123), (9, 124), (9, 134),
(9, 137), (9, 140), (9, 141), (9, 142), (9, 143), (9, 144), (9, 145), (9, 147), (9, 149), (9, 180), (9, 181), (9, 182),
(9, 183), (9, 184), (9, 185), (9, 186), (9, 187), (9, 188), (9, 189), (9, 190), (9, 191), (9, 192), (9, 207), (9, 209),
(9, 245), (9, 276), (9, 277), (9, 278), (9, 279), (9, 280), (9, 281), (9, 286), (9, 1800), (9, 1801), (9, 1802), (9, 1803),
(9, 1804), (9, 1805), (9, 1830), (9, 1831), (9, 1832), (9, 1834), (9, 1840), (9, 1841), (9, 1842), (9, 1850), (9, 1851), (9, 1852),
(9, 1860), (9, 1861), (9, 1862), (9, 1870), (9, 1871), (9, 1872), (9, 1873), (9, 1880), (9, 1890), (9, 1900), (9, 1901), (9, 1902),
(9, 1910), (9, 1912), (9, 1920), (9, 2760), (9, 2770), (9, 2780), (9, 2790), (9, 2791), (9, 2800), (9, 2801), (9, 2860);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(10, 3), (10, 7), (10, 8), (10, 120), (10, 121), (10, 122), (10, 123), (10, 124), (10, 181), (10, 183), (10, 184), (10, 185),
(10, 186), (10, 209), (10, 276), (10, 277), (10, 278), (10, 279), (10, 280), (10, 286), (10, 1850), (10, 1851), (10, 1860), (10, 1861),
(10, 2770), (10, 2780), (10, 2790), (10, 2791), (10, 2800), (10, 2801);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(11, 3), (11, 7), (11, 8), (11, 120), (11, 121), (11, 122), (11, 123), (11, 124), (11, 200), (11, 201), (11, 202), (11, 203),
(11, 204), (11, 205), (11, 206), (11, 207), (11, 208), (11, 209), (11, 279), (11, 280), (11, 282), (11, 283), (11, 284), (11, 2000),
(11, 2001), (11, 2002), (11, 2003), (11, 2010), (11, 2011), (11, 2012), (11, 2020), (11, 2021), (11, 2022), (11, 2023), (11, 2024), (11, 2025),
(11, 2030), (11, 2031), (11, 2032), (11, 2040), (11, 2041), (11, 2050), (11, 2060), (11, 2070), (11, 2071), (11, 2072), (11, 2073), (11, 2074),
(11, 2080), (11, 2081), (11, 2082), (11, 2083), (11, 2790), (11, 2800), (11, 2820), (11, 2830), (11, 2840);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(12, 3), (12, 4), (12, 7), (12, 8), (12, 9), (12, 10), (12, 120), (12, 121), (12, 122), (12, 123), (12, 124), (12, 134),
(12, 137), (12, 209), (12, 220), (12, 240), (12, 241), (12, 242), (12, 243), (12, 244), (12, 245), (12, 279), (12, 280), (12, 2400),
(12, 2401), (12, 2402), (12, 2410), (12, 2420), (12, 2430), (12, 2440), (12, 2441), (12, 2442), (12, 2443), (12, 2450), (12, 2451), (12, 2459),
(12, 2790), (12, 2800);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(13, 3), (13, 7), (13, 8), (13, 120), (13, 121), (13, 122), (13, 123), (13, 124), (13, 209), (13, 279), (13, 280), (13, 2790),
(13, 2800);

-- 菜单分组：模块下的二级目录，不填路由地址，页面地址保持不变
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(901, 5, '采购', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(902, 5, '销售', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(903, 5, '往来单位', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(911, 4, '库存作业', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(912, 4, '库存查询', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(913, 4, '库存控制', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(914, 4, '基础资料', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(921, 6, '生产计划', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(922, 6, '车间执行', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(923, 6, '工艺与设备', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(931, 7, '总账', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(932, 7, '应收应付', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(933, 7, '资金与费用', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(934, 7, '资产与成本', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(935, 7, '预算与分析', 'M', NULL, NULL, NULL, NULL, 5, 1, 1, NOW(), NOW()),
(936, 7, '期末与设置', 'M', NULL, NULL, NULL, NULL, 6, 1, 1, NOW(), NOW()),
(941, 9, '客户经营', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(942, 9, '服务与营销', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(951, 8, '员工管理', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(952, 8, '招聘培训', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW()),
(953, 8, '考勤绩效', 'M', NULL, NULL, NULL, NULL, 3, 1, 1, NOW(), NOW()),
(954, 8, '薪酬福利', 'M', NULL, NULL, NULL, NULL, 4, 1, 1, NOW(), NOW()),
(961, 3, '我的事务', 'M', NULL, NULL, NULL, NULL, 1, 1, 1, NOW(), NOW()),
(962, 3, '流程管理', 'M', NULL, NULL, NULL, NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, c.parent_id FROM sys_role_menu rm JOIN sys_menu c ON c.id = rm.menu_id
WHERE c.parent_id IN (901, 902, 903, 911, 912, 913, 914, 921, 922, 923, 931, 932, 933, 934, 935, 936, 941, 942, 951, 952, 953, 954, 961, 962)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = c.parent_id);

-- 通用流程（可在「发起申请」中直接发起，未发布）
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wf_category', '行政', 'ADMIN', 'cyan', 9, 1, NOW(), NOW());
INSERT INTO wf_model (model_key, name, category, icon, sort, status, design_json, remark, create_time, update_time) VALUES
('oa_travel', '出差申请', 'ADMIN', 'CarOutlined', 21, 1, '{"form": [{"field": "destination", "label": "出差地点", "type": "INPUT", "required": true, "placeholder": "如：上海"}, {"field": "startDate", "label": "开始日期", "type": "DATE", "required": true, "placeholder": ""}, {"field": "endDate", "label": "结束日期", "type": "DATE", "required": true, "placeholder": ""}, {"field": "days", "label": "出差天数", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "budget", "label": "预计费用（元）", "type": "NUMBER", "required": false, "placeholder": ""}, {"field": "reason", "label": "出差事由", "type": "TEXTAREA", "required": true, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "days_check", "type": "CONDITION", "name": "按天数和费用", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "days_check_yes", "name": "≥ 5 天或费用 ≥ 1 万元", "defaultBranch": false, "conditions": [[{"field": "days", "operator": "GE", "value": "5"}], [{"field": "budget", "operator": "GE", "value": "10000"}]], "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "days_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程：部门主管 → 出差 ≥ 5 天或费用 ≥ 1 万元时上级主管 → 抄送人事。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_seal', '用印申请', 'ADMIN', 'SafetyCertificateOutlined', 22, 1, '{"form": [{"field": "sealType", "label": "印章类型", "type": "SELECT", "required": true, "placeholder": "", "options": ["公章", "合同专用章", "财务专用章", "部门章"]}, {"field": "fileName", "label": "文件名称", "type": "INPUT", "required": true, "placeholder": ""}, {"field": "copies", "label": "用印份数", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "purpose", "label": "用途说明", "type": "TEXTAREA", "required": true, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "seal_check", "type": "CONDITION", "name": "按印章类型", "next": {"id": "cc_hr", "type": "CC", "name": "抄送人事专员", "assignee": {"type": "ROLE", "ids": [11], "names": ["人事专员"]}, "next": null}, "branches": [{"id": "seal_check_yes", "name": "公章、合同专用章", "defaultBranch": false, "conditions": [[{"field": "sealType", "operator": "IN", "value": "公章,合同专用章"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "seal_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程：部门主管 → 公章、合同章时总经理 → 抄送人事行政。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_goods', '物品领用', 'ADMIN', 'ShoppingOutlined', 23, 1, '{"form": [{"field": "goods", "label": "物品名称", "type": "INPUT", "required": true, "placeholder": "如：笔记本电脑、显示器"}, {"field": "qty", "label": "数量", "type": "NUMBER", "required": true, "placeholder": ""}, {"field": "useDate", "label": "领用日期", "type": "DATE", "required": false, "placeholder": ""}, {"field": "purpose", "label": "用途", "type": "TEXTAREA", "required": false, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_wh", "type": "CC", "name": "抄送仓库管理员", "assignee": {"type": "ROLE", "ids": [6], "names": ["仓库管理员"]}, "next": null}}}}', '内置流程：部门主管 → 抄送仓库管理员。可在「发起申请」中直接发起', NOW(), NOW()),
('oa_general', '通用审批', 'OTHER', 'FileTextOutlined', 24, 1, '{"form": [{"field": "title", "label": "审批事项", "type": "INPUT", "required": true, "placeholder": ""}, {"field": "content", "label": "详细说明", "type": "TEXTAREA", "required": true, "placeholder": ""}, {"field": "amount", "label": "涉及金额（元）", "type": "NUMBER", "required": false, "placeholder": ""}], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "leader2", "type": "APPROVER", "name": "上级主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 2}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}}}', '内置流程：部门主管 → 上级主管（依次审批）。可在「发起申请」中直接发起', NOW(), NOW());

-- ---------------------------------------------------------------- 资金管理：票据、账户调拨、资金计划
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('1121', '应收票据', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW()),
('2201', '应付票据', 0, 'LIABILITY', 'CREDIT', 1, NOW(), NOW());

INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(119, '票据类型', 'fin_note_type', 1, '承兑汇票的类型', NOW(), NOW()),
(120, '票据状态', 'fin_note_status', 1, '应收票据、应付票据的状态', NOW(), NOW()),
(121, '票据操作', 'fin_note_action', 1, '票据的操作记录', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_settle_method', '票据', 'NOTE', 'purple', 3, 1, NOW(), NOW()),
('fin_voucher_source', '票据', 'NOTE', 'purple', 20, 1, NOW(), NOW()),
('fin_voucher_source', '账户调拨', 'TRANSFER', 'cyan', 21, 1, NOW(), NOW()),
('fin_bill_source', '票据拒付', 'NOTE', 'red', 10, 1, NOW(), NOW()),
('fin_note_type', '银行承兑汇票', 'BANK', 'blue', 1, 1, NOW(), NOW()),
('fin_note_type', '商业承兑汇票', 'COMMERCIAL', 'orange', 2, 1, NOW(), NOW()),
('fin_note_status', '持有', 'HELD', 'blue', 1, 1, NOW(), NOW()),
('fin_note_status', '背书审批中', 'ENDORSING', 'gold', 2, 1, NOW(), NOW()),
('fin_note_status', '已背书', 'ENDORSED', 'default', 3, 1, NOW(), NOW()),
('fin_note_status', '已贴现', 'DISCOUNTED', 'default', 4, 1, NOW(), NOW()),
('fin_note_status', '已托收', 'COLLECTED', 'green', 5, 1, NOW(), NOW()),
('fin_note_status', '已拒付', 'DISHONORED', 'red', 6, 1, NOW(), NOW()),
('fin_note_status', '开票审批中', 'ISSUING', 'gold', 7, 1, NOW(), NOW()),
('fin_note_status', '未兑付', 'ISSUED', 'blue', 8, 1, NOW(), NOW()),
('fin_note_status', '已兑付', 'PAID', 'green', 9, 1, NOW(), NOW()),
('fin_note_status', '已作废', 'CANCELLED', 'default', 10, 1, NOW(), NOW()),
('fin_note_action', '收票', 'RECEIVE', 'blue', 1, 1, NOW(), NOW()),
('fin_note_action', '背书转让', 'ENDORSE', 'purple', 2, 1, NOW(), NOW()),
('fin_note_action', '贴现', 'DISCOUNT', 'orange', 3, 1, NOW(), NOW()),
('fin_note_action', '托收', 'COLLECT', 'green', 4, 1, NOW(), NOW()),
('fin_note_action', '拒付', 'DISHONOR', 'red', 5, 1, NOW(), NOW()),
('fin_note_action', '开票', 'ISSUE', 'blue', 6, 1, NOW(), NOW()),
('fin_note_action', '兑付', 'PAY', 'green', 7, 1, NOW(), NOW());

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(193, 933, '票据管理', 'C', 'note', 'fin/note/index', 'fin:note:list', 'CreditCardOutlined', 4, 1, 1, NOW(), NOW()),
(194, 933, '账户调拨', 'C', 'transfer', 'fin/transfer/index', 'fin:transfer:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(195, 933, '资金计划', 'C', 'fund-plan', 'fin/fund-plan/index', 'fin:fundPlan:list', 'FundOutlined', 6, 1, 1, NOW(), NOW()),
(1930, 193, '票据操作', 'F', NULL, NULL, 'fin:note:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1940, 194, '调拨登记', 'F', NULL, NULL, 'fin:transfer:edit', NULL, 1, 1, 1, NOW(), NOW()),
(1950, 195, '计划项维护', 'F', NULL, NULL, 'fin:fundPlan:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 普通用户只读；会计、出纳可操作；总经理查看资金计划
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 193), (2, 194), (2, 195),
(9, 193), (9, 194), (9, 195), (9, 1930), (9, 1940), (9, 1950),
(10, 193), (10, 194), (10, 195), (10, 1930), (10, 1940), (10, 1950),
(3, 195);

-- ---------------------------------------------------------------- 质量管理：菜单、字典、内置角色
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(122, '不合格来源', 'qms_ncr_source', 1, '不合格品处理单的来源', NOW(), NOW()),
(123, '不合格处置', 'qms_disposition', 1, '不合格品的处置方式', NOW(), NOW()),
(124, '责任方', 'qms_responsibility', 1, '不合格的责任方', NOW(), NOW()),
(125, '不合格处理状态', 'qms_ncr_status', 1, '不合格品处理单的状态', NOW(), NOW()),
(126, '纠正预防来源', 'qms_capa_source', 1, '纠正预防措施的来源', NOW(), NOW()),
(127, '纠正预防状态', 'qms_capa_status', 1, '纠正预防措施的状态', NOW(), NOW()),
(128, '索赔状态', 'qms_claim_status', 1, '供应商质量索赔的状态', NOW(), NOW()),
(129, '抽样方式', 'qms_sampling_mode', 1, '检验方案的抽样方式', NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('mes_qc_type', '首件检验', 'FAI', 'purple', 4, 1, NOW(), NOW()),
('fin_bill_source', '质量索赔', 'QUALITY_CLAIM', 'red', 11, 1, NOW(), NOW()),
('qms_ncr_source', '检验不合格', 'INSPECTION', 'blue', 1, 1, NOW(), NOW()),
('qms_ncr_source', '生产发现', 'PRODUCTION', 'orange', 2, 1, NOW(), NOW()),
('qms_ncr_source', '客户投诉', 'CUSTOMER', 'red', 3, 1, NOW(), NOW()),
('qms_ncr_source', '其他', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('qms_disposition', '退货', 'RETURN', 'red', 1, 1, NOW(), NOW()),
('qms_disposition', '返工', 'REWORK', 'orange', 2, 1, NOW(), NOW()),
('qms_disposition', '报废', 'SCRAP', 'default', 3, 1, NOW(), NOW()),
('qms_disposition', '让步接收', 'CONCESSION', 'gold', 4, 1, NOW(), NOW()),
('qms_disposition', '挑选', 'SORT', 'cyan', 5, 1, NOW(), NOW()),
('qms_responsibility', '供应商', 'SUPPLIER', 'red', 1, 1, NOW(), NOW()),
('qms_responsibility', '生产', 'PRODUCTION', 'orange', 2, 1, NOW(), NOW()),
('qms_responsibility', '设计', 'DESIGN', 'purple', 3, 1, NOW(), NOW()),
('qms_responsibility', '其他', 'OTHER', 'default', 4, 1, NOW(), NOW()),
('qms_ncr_status', '待评审', 'OPEN', 'blue', 1, 1, NOW(), NOW()),
('qms_ncr_status', '审批中', 'APPROVING', 'gold', 2, 1, NOW(), NOW()),
('qms_ncr_status', '已处置', 'DONE', 'green', 3, 1, NOW(), NOW()),
('qms_ncr_status', '已取消', 'CANCELLED', 'default', 4, 1, NOW(), NOW()),
('qms_capa_source', '不合格品', 'NCR', 'red', 1, 1, NOW(), NOW()),
('qms_capa_source', '客户投诉', 'COMPLAINT', 'orange', 2, 1, NOW(), NOW()),
('qms_capa_source', '内部发现', 'INTERNAL', 'blue', 3, 1, NOW(), NOW()),
('qms_capa_source', '审核发现', 'AUDIT', 'purple', 4, 1, NOW(), NOW()),
('qms_capa_status', '处理中', 'OPEN', 'blue', 1, 1, NOW(), NOW()),
('qms_capa_status', '待验证', 'VERIFYING', 'gold', 2, 1, NOW(), NOW()),
('qms_capa_status', '已关闭', 'CLOSED', 'green', 3, 1, NOW(), NOW()),
('qms_claim_status', '待确认', 'DRAFT', 'blue', 1, 1, NOW(), NOW()),
('qms_claim_status', '已确认', 'CONFIRMED', 'green', 2, 1, NOW(), NOW()),
('qms_claim_status', '已取消', 'CANCELLED', 'default', 3, 1, NOW(), NOW()),
('qms_sampling_mode', '全检', 'FULL', 'default', 1, 1, NOW(), NOW()),
('qms_sampling_mode', '固定数量', 'FIXED', 'blue', 2, 1, NOW(), NOW()),
('qms_sampling_mode', 'AQL 抽样', 'AQL', 'purple', 3, 1, NOW(), NOW());

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wf_category', '质量', 'QUALITY', 'volcano', 10, 1, NOW(), NOW());
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('qms_ncr', '不合格品处置审批', 'QUALITY', 30, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "qa", "type": "APPROVER", "name": "质量工程师审核", "assignee": {"type": "ROLE", "ids": [14], "names": ["质量工程师"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "disp_check", "type": "CONDITION", "name": "按处置方式和数量", "next": null, "branches": [{"id": "disp_check_yes", "name": "让步接收 / 报废或数量 ≥ 100", "defaultBranch": false, "conditions": [[{"field": "disposition", "operator": "IN", "value": "CONCESSION,SCRAP"}], [{"field": "qty", "operator": "GE", "value": "100"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "disp_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：质量工程师 → 让步接收、报废或数量 ≥ 100 时总经理。发布后不合格品评审需审批，通过后执行处置；流程变量 qty 数量、disposition 处置方式、responsibility 责任方', NOW(), NOW());

-- 质量管理排在生产制造之后
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 0 AND sort >= 4;
UPDATE sys_menu SET parent_id = 963, sort = 2 WHERE id = 163;

INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(12, 0, '质量管理', 'M', '/qms', NULL, NULL, 'SafetyCertificateOutlined', 4, 1, 1, NOW(), NOW()),
(963, 12, '检验管理', 'M', NULL, NULL, NULL, 'AuditOutlined', 1, 1, 1, NOW(), NOW()),
(964, 12, '不良处理', 'M', NULL, NULL, NULL, 'WarningOutlined', 2, 1, 1, NOW(), NOW()),
(965, 12, '质量分析', 'M', NULL, NULL, NULL, 'LineChartOutlined', 3, 1, 1, NOW(), NOW()),
(290, 963, '检验方案', 'C', 'plan', 'qms/plan/index', 'qms:plan:list', 'ProfileOutlined', 1, 1, 1, NOW(), NOW()),
(291, 964, '不合格品处理', 'C', 'ncr', 'qms/ncr/index', 'qms:ncr:list', 'ExceptionOutlined', 1, 1, 1, NOW(), NOW()),
(292, 964, '纠正预防', 'C', 'capa', 'qms/capa/index', 'qms:capa:list', 'ToolOutlined', 2, 1, 1, NOW(), NOW()),
(293, 964, '供应商索赔', 'C', 'claim', 'qms/claim/index', 'qms:claim:list', 'AccountBookOutlined', 3, 1, 1, NOW(), NOW()),
(294, 965, '质量分析', 'C', 'analysis', 'qms/analysis/index', 'qms:analysis:list', 'BarChartOutlined', 1, 1, 1, NOW(), NOW()),
(295, 965, 'SPC 控制图', 'C', 'spc', 'qms/spc/index', 'qms:spc:list', 'StockOutlined', 2, 1, 1, NOW(), NOW()),
(2900, 290, '方案新增', 'F', NULL, NULL, 'qms:plan:add', NULL, 1, 1, 1, NOW(), NOW()),
(2901, 290, '方案修改', 'F', NULL, NULL, 'qms:plan:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2902, 290, '方案删除', 'F', NULL, NULL, 'qms:plan:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2910, 291, '不合格登记', 'F', NULL, NULL, 'qms:ncr:add', NULL, 1, 1, 1, NOW(), NOW()),
(2911, 291, '评审处置', 'F', NULL, NULL, 'qms:ncr:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2912, 291, '不合格删除', 'F', NULL, NULL, 'qms:ncr:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2920, 292, '纠正预防登记', 'F', NULL, NULL, 'qms:capa:add', NULL, 1, 1, 1, NOW(), NOW()),
(2921, 292, '8D 填写', 'F', NULL, NULL, 'qms:capa:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2922, 292, '效果验证', 'F', NULL, NULL, 'qms:capa:verify', NULL, 3, 1, 1, NOW(), NOW()),
(2923, 292, '纠正预防删除', 'F', NULL, NULL, 'qms:capa:remove', NULL, 4, 1, 1, NOW(), NOW()),
(2930, 293, '索赔登记', 'F', NULL, NULL, 'qms:claim:add', NULL, 1, 1, 1, NOW(), NOW()),
(2931, 293, '索赔确认', 'F', NULL, NULL, 'qms:claim:confirm', NULL, 2, 1, 1, NOW(), NOW()),
(2932, 293, '索赔删除', 'F', NULL, NULL, 'qms:claim:remove', NULL, 3, 1, 1, NOW(), NOW());

INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(14, '质量工程师', 'quality', 14, 1, 1, '检验方案、质量检验与首件、不合格品评审处置、纠正预防（8D）、供应商索赔、质量分析与 SPC', NOW(), NOW());

-- 质量工程师：质量管理全部功能，可查看物料、供应商、工单；普通用户只读；车间主管可登记不合格；采购员查看不合格和索赔；总经理看分析
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 14, id FROM sys_menu WHERE id IN (12, 963, 964, 965, 163, 1630, 1631, 1632) OR id BETWEEN 290 AND 295 OR id BETWEEN 2900 AND 2932;
-- 授权页面时连同所在分组和模块（否则菜单树中没有上级）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(14, 4), (14, 914), (14, 137), (14, 5), (14, 903), (14, 147), (14, 6), (14, 922), (14, 162),
(14, 3), (14, 961), (14, 120), (14, 121), (14, 122), (14, 123), (14, 124),
(2, 12), (2, 963), (2, 964), (2, 965), (2, 290), (2, 291), (2, 292), (2, 293), (2, 294), (2, 295),
(8, 12), (8, 964), (8, 291), (8, 2910), (8, 292),
(4, 12), (4, 964), (4, 291), (4, 293),
(3, 12), (3, 964), (3, 965), (3, 294), (3, 295), (3, 291), (3, 292);

-- ===================== 采购合同、招标与供应商协同（SRM） =====================
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(130, '采购合同类型', 'scm_contract_type', 1, '框架协议或固定数量合同', NOW(), NOW()),
(131, '采购合同状态', 'scm_contract_status', 1, '采购合同的状态', NOW(), NOW()),
(132, '招标状态', 'scm_tender_status', 1, '招标的状态', NOW(), NOW()),
(133, '送货通知状态', 'scm_asn_status', 1, '供应商送货通知的状态', NOW(), NOW()),
(134, '供应商确认', 'scm_supplier_confirm', 1, '供应商对采购订单的确认结果', NOW(), NOW()),
(135, '对账单状态', 'fin_statement_status', 1, '供应商对账单的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_contract_type', '框架协议', 'FRAMEWORK', 'blue', 1, 1, NOW(), NOW()),
('scm_contract_type', '固定数量合同', 'FIXED', 'green', 2, 1, NOW(), NOW()),
('scm_contract_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('scm_contract_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('scm_contract_status', '生效', 'ACTIVE', 'green', 3, 1, NOW(), NOW()),
('scm_contract_status', '已关闭', 'CLOSED', 'default', 4, 1, NOW(), NOW()),
('scm_tender_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('scm_tender_status', '投标中', 'BIDDING', 'processing', 2, 1, NOW(), NOW()),
('scm_tender_status', '已开标', 'OPENED', 'orange', 3, 1, NOW(), NOW()),
('scm_tender_status', '已定标', 'AWARDED', 'green', 4, 1, NOW(), NOW()),
('scm_tender_status', '已取消', 'CANCELLED', 'default', 5, 1, NOW(), NOW()),
('scm_asn_status', '待收货', 'SUBMITTED', 'blue', 1, 1, NOW(), NOW()),
('scm_asn_status', '已收货', 'RECEIVED', 'green', 2, 1, NOW(), NOW()),
('scm_asn_status', '已取消', 'CANCELLED', 'default', 3, 1, NOW(), NOW()),
('scm_supplier_confirm', '已接受', 'ACCEPTED', 'green', 1, 1, NOW(), NOW()),
('scm_supplier_confirm', '已拒绝', 'REJECTED', 'red', 2, 1, NOW(), NOW()),
('fin_statement_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_statement_status', '待确认', 'SENT', 'blue', 2, 1, NOW(), NOW()),
('fin_statement_status', '已确认', 'CONFIRMED', 'green', 3, 1, NOW(), NOW()),
('fin_statement_status', '有异议', 'DISPUTED', 'red', 4, 1, NOW(), NOW()),
('scm_order_source', '招标', 'TENDER', 'gold', 9, 1, NOW(), NOW());

-- 供应链新增「采购寻源」分组（询价比价移入），采购下新增送货通知；应收应付下新增供应商对账；顶部导航最后为供应商门户（只对供应商账号显示）
UPDATE sys_menu SET sort = 3 WHERE id = 902;
UPDATE sys_menu SET sort = 4 WHERE id = 903;
UPDATE sys_menu SET parent_id = 904, sort = 1 WHERE id = 268;
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 901 AND sort >= 4;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(904, 5, '采购寻源', 'M', NULL, NULL, NULL, 'FileSearchOutlined', 2, 1, 1, NOW(), NOW()),
(296, 904, '招标采购', 'C', 'tender', 'scm/tender/index', 'scm:tender:list', 'TrophyOutlined', 2, 1, 1, NOW(), NOW()),
(297, 904, '采购合同', 'C', 'contract', 'scm/contract/index', 'scm:contract:list', 'FileProtectOutlined', 3, 1, 1, NOW(), NOW()),
(298, 901, '送货通知', 'C', 'asn', 'scm/asn/index', 'scm:asn:list', 'CarOutlined', 4, 1, 1, NOW(), NOW()),
(299, 932, '供应商对账', 'C', 'statement', 'fin/statement/index', 'fin:statement:list', 'ReconciliationOutlined', 7, 1, 1, NOW(), NOW()),
(13, 0, '供应商门户', 'M', '/portal', NULL, NULL, 'ShopOutlined', 13, 1, 1, NOW(), NOW()),
(300, 13, '我的订单', 'C', 'order', 'portal/order/index', 'portal:order:list', 'ShoppingOutlined', 1, 1, 1, NOW(), NOW()),
(301, 13, '送货通知', 'C', 'asn', 'portal/asn/index', 'portal:asn:list', 'CarOutlined', 2, 1, 1, NOW(), NOW()),
(302, 13, '对账单', 'C', 'statement', 'portal/statement/index', 'portal:statement:list', 'ReconciliationOutlined', 3, 1, 1, NOW(), NOW()),
(303, 13, '招标投标', 'C', 'tender', 'portal/tender/index', 'portal:tender:list', 'TrophyOutlined', 4, 1, 1, NOW(), NOW()),
(304, 13, '质量索赔', 'C', 'claim', 'portal/claim/index', 'portal:claim:list', 'AccountBookOutlined', 5, 1, 1, NOW(), NOW()),
(1473, 147, '门户账号', 'F', NULL, NULL, 'scm:supplier:portal', NULL, 4, 1, 1, NOW(), NOW()),
(2960, 296, '招标新增', 'F', NULL, NULL, 'scm:tender:add', NULL, 1, 1, 1, NOW(), NOW()),
(2961, 296, '招标修改', 'F', NULL, NULL, 'scm:tender:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2962, 296, '招标删除', 'F', NULL, NULL, 'scm:tender:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2963, 296, '开标定标', 'F', NULL, NULL, 'scm:tender:award', NULL, 4, 1, 1, NOW(), NOW()),
(2970, 297, '合同新增', 'F', NULL, NULL, 'scm:contract:add', NULL, 1, 1, 1, NOW(), NOW()),
(2971, 297, '合同修改', 'F', NULL, NULL, 'scm:contract:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2972, 297, '合同删除', 'F', NULL, NULL, 'scm:contract:remove', NULL, 3, 1, 1, NOW(), NOW()),
(2973, 297, '合同提交', 'F', NULL, NULL, 'scm:contract:approve', NULL, 4, 1, 1, NOW(), NOW()),
(2974, 297, '合同关闭', 'F', NULL, NULL, 'scm:contract:close', NULL, 5, 1, 1, NOW(), NOW()),
(2980, 298, '送货收货', 'F', NULL, NULL, 'scm:asn:receive', NULL, 1, 1, 1, NOW(), NOW()),
(2990, 299, '对账单生成', 'F', NULL, NULL, 'fin:statement:add', NULL, 1, 1, 1, NOW(), NOW()),
(2991, 299, '对账单发送', 'F', NULL, NULL, 'fin:statement:edit', NULL, 2, 1, 1, NOW(), NOW()),
(2992, 299, '对账单删除', 'F', NULL, NULL, 'fin:statement:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3000, 300, '订单确认', 'F', NULL, NULL, 'portal:order:confirm', NULL, 1, 1, 1, NOW(), NOW()),
(3010, 301, '发起送货', 'F', NULL, NULL, 'portal:asn:add', NULL, 1, 1, 1, NOW(), NOW()),
(3020, 302, '对账确认', 'F', NULL, NULL, 'portal:statement:confirm', NULL, 1, 1, 1, NOW(), NOW()),
(3030, 303, '提交投标', 'F', NULL, NULL, 'portal:tender:bid', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(15, '供应商', 'supplier', 15, 5, 1, '供应商门户账号：确认订单、送货通知、对账、投标、查看质量索赔，只能看到本供应商的数据', NOW(), NOW());

-- 询价比价移入采购寻源：已授权询价比价的角色连同新分组授权
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 904 FROM sys_role_menu WHERE menu_id = 268;
-- 普通用户只读；采购员：招标、合同、门户账号；仓库：送货收货；会计：供应商对账；总经理查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(15, 13), (15, 300), (15, 301), (15, 302), (15, 303), (15, 304), (15, 3000), (15, 3010), (15, 3020), (15, 3030),
(2, 296), (2, 297), (2, 298), (2, 299),
(4, 296), (4, 297), (4, 298), (4, 2960), (4, 2961), (4, 2962), (4, 2963), (4, 2970), (4, 2971), (4, 2972), (4, 2973), (4, 2974), (4, 1473),
(6, 298), (6, 2980),
(9, 299), (9, 2990), (9, 2991), (9, 2992),
(3, 296), (3, 297), (3, 298), (3, 299);

-- 采购合同审批（未发布）：部门主管 → 金额 ≥ 10 万时总经理，抄送会计
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('scm_contract', '采购合同审批', 'PURCHASE', 31, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "amount_check", "type": "CONDITION", "name": "按合同金额", "next": {"id": "cc_acct", "type": "CC", "name": "抄送会计", "assignee": {"type": "ROLE", "ids": [9], "names": ["会计"]}, "next": null}, "branches": [{"id": "amount_check_yes", "name": "合同金额 ≥ 10 万", "defaultBranch": false, "conditions": [[{"field": "amount", "operator": "GE", "value": "100000"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [3], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "amount_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}', '内置流程（未发布）：部门主管 → 合同金额 ≥ 10 万时总经理 → 抄送会计。发布后采购合同提交需审批，通过后生效；流程变量 amount 合同金额（金额上限或明细数量 × 单价合计）、contractType 合同类型', NOW(), NOW());

-- ===================== 研发 / PLM：工程变更、技术文档 =====================
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(136, '工程变更原因', 'mes_ecn_reason', 1, '工程变更的原因', NOW(), NOW()),
(137, '工程变更状态', 'mes_ecn_status', 1, '工程变更的状态', NOW(), NOW()),
(138, '技术文档类型', 'mes_doc_type', 1, '图纸、规格书、作业指导书等', NOW(), NOW()),
(139, '技术文档状态', 'mes_doc_status', 1, '技术文档的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('mes_ecn_reason', '设计改进', 'DESIGN', 'blue', 1, 1, NOW(), NOW()),
('mes_ecn_reason', '降本', 'COST', 'green', 2, 1, NOW(), NOW()),
('mes_ecn_reason', '质量问题', 'QUALITY', 'red', 3, 1, NOW(), NOW()),
('mes_ecn_reason', '客户要求', 'CUSTOMER', 'purple', 4, 1, NOW(), NOW()),
('mes_ecn_reason', '物料替代', 'SUBSTITUTE', 'orange', 5, 1, NOW(), NOW()),
('mes_ecn_reason', '其他', 'OTHER', 'default', 6, 1, NOW(), NOW()),
('mes_ecn_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('mes_ecn_status', '审批中', 'APPROVING', 'processing', 2, 1, NOW(), NOW()),
('mes_ecn_status', '待生效', 'APPROVED', 'blue', 3, 1, NOW(), NOW()),
('mes_ecn_status', '已生效', 'EFFECTIVE', 'green', 4, 1, NOW(), NOW()),
('mes_ecn_status', '已取消', 'CANCELLED', 'default', 5, 1, NOW(), NOW()),
('mes_doc_type', '图纸', 'DRAWING', 'blue', 1, 1, NOW(), NOW()),
('mes_doc_type', '规格书', 'SPEC', 'cyan', 2, 1, NOW(), NOW()),
('mes_doc_type', '作业指导书', 'SOP', 'green', 3, 1, NOW(), NOW()),
('mes_doc_type', '工艺文件', 'PROCESS', 'purple', 4, 1, NOW(), NOW()),
('mes_doc_type', '检验标准', 'INSPECTION', 'orange', 5, 1, NOW(), NOW()),
('mes_doc_type', '其他', 'OTHER', 'default', 6, 1, NOW(), NOW()),
('mes_doc_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('mes_doc_status', '已发布', 'RELEASED', 'green', 2, 1, NOW(), NOW()),
('mes_doc_status', '已作废', 'OBSOLETE', 'default', 3, 1, NOW(), NOW());

-- 工艺与设备：物料清单之后为工程变更、技术文档
UPDATE sys_menu SET sort = sort + 2 WHERE parent_id = 923 AND sort >= 2;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(305, 923, '工程变更', 'C', 'ecn', 'mes/ecn/index', 'mes:ecn:list', 'DiffOutlined', 2, 1, 1, NOW(), NOW()),
(306, 923, '技术文档', 'C', 'doc', 'mes/doc/index', 'mes:doc:list', 'FileTextOutlined', 3, 1, 1, NOW(), NOW()),
(3050, 305, '变更新增', 'F', NULL, NULL, 'mes:ecn:add', NULL, 1, 1, 1, NOW(), NOW()),
(3051, 305, '变更修改', 'F', NULL, NULL, 'mes:ecn:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3052, 305, '变更删除', 'F', NULL, NULL, 'mes:ecn:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3053, 305, '变更提交', 'F', NULL, NULL, 'mes:ecn:approve', NULL, 4, 1, 1, NOW(), NOW()),
(3060, 306, '文档新增', 'F', NULL, NULL, 'mes:doc:add', NULL, 1, 1, 1, NOW(), NOW()),
(3061, 306, '文档修改', 'F', NULL, NULL, 'mes:doc:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3062, 306, '文档删除', 'F', NULL, NULL, 'mes:doc:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3063, 306, '文档发布', 'F', NULL, NULL, 'mes:doc:release', NULL, 4, 1, 1, NOW(), NOW());

-- 普通用户只读；生产计划员：工程变更与技术文档；车间主管、质量工程师、总经理查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 305), (2, 306),
(7, 305), (7, 306), (7, 3050), (7, 3051), (7, 3052), (7, 3053), (7, 3060), (7, 3061), (7, 3062), (7, 3063),
(8, 306),
(14, 306), (14, 923),
(3, 305), (3, 306);

-- 工程变更审批（未发布）：部门主管 → 质量工程师会签 → 抄送生产计划员
INSERT INTO wf_model (model_key, name, category, sort, status, design_json, remark, create_time, update_time) VALUES
('mes_ecn', '工程变更审批', 'PRODUCTION', 32, 1, '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "qa", "type": "APPROVER", "name": "质量工程师审核", "assignee": {"type": "ROLE", "ids": [14], "names": ["质量工程师"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "cc_plan", "type": "CC", "name": "抄送生产计划员", "assignee": {"type": "ROLE", "ids": [7], "names": ["生产计划员"]}, "next": null}}}}}', '内置流程（未发布）：部门主管 → 质量工程师 → 抄送生产计划员。发布后工程变更提交需审批，通过后按生效日期切换 BOM；流程变量 reason 变更原因、materialCode 产品编码', NOW(), NOW());

-- ===================== 资产：使用人与存放地点、领用调拨、盘点、低值易耗品 =====================
INSERT INTO fin_account (code, name, parent_id, category, direction, status, create_time, update_time) VALUES
('1411', '周转材料', 0, 'ASSET', 'DEBIT', 1, NOW(), NOW());
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(140, '资产变动类型', 'fin_asset_move_type', 1, '资产领用、归还、调拨', NOW(), NOW()),
(141, '资产盘点状态', 'fin_asset_check_status', 1, '资产盘点单的状态', NOW(), NOW()),
(142, '资产盘点结果', 'fin_asset_check_result', 1, '资产盘点明细的结果', NOW(), NOW()),
(143, '易耗品领用状态', 'fin_consumable_status', 1, '低值易耗品领用单的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_asset_move_type', '领用', 'ASSIGN', 'blue', 1, 1, NOW(), NOW()),
('fin_asset_move_type', '归还', 'RETURN', 'orange', 2, 1, NOW(), NOW()),
('fin_asset_move_type', '调拨', 'TRANSFER', 'purple', 3, 1, NOW(), NOW()),
('fin_asset_check_status', '盘点中', 'CHECKING', 'processing', 1, 1, NOW(), NOW()),
('fin_asset_check_status', '已完成', 'DONE', 'green', 2, 1, NOW(), NOW()),
('fin_asset_check_result', '待盘', 'PENDING', 'default', 1, 1, NOW(), NOW()),
('fin_asset_check_result', '相符', 'NORMAL', 'green', 2, 1, NOW(), NOW()),
('fin_asset_check_result', '位置变化', 'MOVED', 'orange', 3, 1, NOW(), NOW()),
('fin_asset_check_result', '盘亏', 'LOSS', 'red', 4, 1, NOW(), NOW()),
('fin_asset_check_result', '盘盈', 'GAIN', 'purple', 5, 1, NOW(), NOW()),
('fin_consumable_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_consumable_status', '已出库', 'CONFIRMED', 'green', 2, 1, NOW(), NOW()),
('wms_material_category', '低值易耗品', 'LOW_VALUE', 'magenta', 6, 1, NOW(), NOW()),
('wms_out_type', '部门领用', 'CONSUME', 'lime', 10, 1, NOW(), NOW());

-- 资产与成本：固定资产之后为资产盘点、低值易耗品
UPDATE sys_menu SET sort = sort + 2 WHERE parent_id = 934 AND sort >= 2;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(307, 934, '资产盘点', 'C', 'asset-check', 'fin/asset-check/index', 'fin:assetCheck:list', 'AuditOutlined', 2, 1, 1, NOW(), NOW()),
(308, 934, '低值易耗品', 'C', 'consumable', 'fin/consumable/index', 'fin:consumable:list', 'ToolOutlined', 3, 1, 1, NOW(), NOW()),
(1874, 187, '领用调拨', 'F', NULL, NULL, 'fin:asset:move', NULL, 5, 1, 1, NOW(), NOW()),
(3070, 307, '盘点新增', 'F', NULL, NULL, 'fin:assetCheck:add', NULL, 1, 1, 1, NOW(), NOW()),
(3071, 307, '盘点录入', 'F', NULL, NULL, 'fin:assetCheck:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3072, 307, '盘点删除', 'F', NULL, NULL, 'fin:assetCheck:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3080, 308, '领用新增', 'F', NULL, NULL, 'fin:consumable:add', NULL, 1, 1, 1, NOW(), NOW()),
(3081, 308, '领用出库', 'F', NULL, NULL, 'fin:consumable:confirm', NULL, 2, 1, 1, NOW(), NOW()),
(3082, 308, '领用删除', 'F', NULL, NULL, 'fin:consumable:remove', NULL, 3, 1, 1, NOW(), NOW());

-- 普通用户只读；会计：全部；总经理查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 307), (2, 308),
(9, 307), (9, 308), (9, 1874), (9, 3070), (9, 3071), (9, 3072), (9, 3080), (9, 3081), (9, 3082),
(3, 307), (3, 308);

-- ---------------------------------------------------------------- 标准成本与成本差异
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(144, '标准成本版本状态', 'fin_std_cost_status', 1, '标准成本版本的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_std_cost_status', '草稿', 'DRAFT', 'default', 1, 1, NOW(), NOW()),
('fin_std_cost_status', '已发布', 'RELEASED', 'green', 2, 1, NOW(), NOW());

-- 资产与成本：成本核算之后为标准成本（成本差异在成本核算页面）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(309, 934, '标准成本', 'C', 'std-cost', 'fin/std-cost/index', 'fin:stdCost:list', 'CalculatorOutlined', 5, 1, 1, NOW(), NOW()),
(3090, 309, '标准成本维护', 'F', NULL, NULL, 'fin:stdCost:edit', NULL, 1, 1, 1, NOW(), NOW());

-- 与成本核算相同：普通用户、总经理查看，会计维护
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 309), (3, 309), (9, 309), (9, 3090);

-- ---------------------------------------------------------------- 设备 OEE、模具工装
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(145, '停机原因', 'mes_stop_reason', 1, '设备停机原因：保养、无生产计划为计划停机', NOW(), NOW()),
(146, '模具类型', 'mes_mold_type', 1, '模具工装的类型', NOW(), NOW()),
(147, '模具状态', 'mes_mold_status', 1, '模具工装的状态', NOW(), NOW()),
(148, '模具履历', 'mes_mold_action', 1, '模具领用、归还、使用、维修、报废', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('mes_stop_reason', '故障', 'BREAKDOWN', 'red', 1, 1, NOW(), NOW()),
('mes_stop_reason', '换型调机', 'CHANGEOVER', 'orange', 2, 1, NOW(), NOW()),
('mes_stop_reason', '待料', 'MATERIAL', 'gold', 3, 1, NOW(), NOW()),
('mes_stop_reason', '计划保养', 'MAINTAIN', 'blue', 4, 1, NOW(), NOW()),
('mes_stop_reason', '无生产计划', 'NO_ORDER', 'default', 5, 1, NOW(), NOW()),
('mes_stop_reason', '其他', 'OTHER', 'purple', 6, 1, NOW(), NOW()),
('mes_mold_type', '模具', 'MOLD', 'blue', 1, 1, NOW(), NOW()),
('mes_mold_type', '工装夹具', 'FIXTURE', 'cyan', 2, 1, NOW(), NOW()),
('mes_mold_type', '检具', 'GAUGE', 'purple', 3, 1, NOW(), NOW()),
('mes_mold_status', '在库', 'AVAILABLE', 'green', 1, 1, NOW(), NOW()),
('mes_mold_status', '使用中', 'IN_USE', 'processing', 2, 1, NOW(), NOW()),
('mes_mold_status', '维修中', 'REPAIR', 'orange', 3, 1, NOW(), NOW()),
('mes_mold_status', '报废', 'SCRAPPED', 'default', 4, 1, NOW(), NOW()),
('mes_mold_action', '领用', 'ISSUE', 'blue', 1, 1, NOW(), NOW()),
('mes_mold_action', '归还', 'RETURN', 'green', 2, 1, NOW(), NOW()),
('mes_mold_action', '使用', 'USE', 'cyan', 3, 1, NOW(), NOW()),
('mes_mold_action', '送修', 'REPAIR', 'orange', 4, 1, NOW(), NOW()),
('mes_mold_action', '修复', 'REPAIRED', 'lime', 5, 1, NOW(), NOW()),
('mes_mold_action', '报废', 'SCRAP', 'default', 6, 1, NOW(), NOW());

-- 工艺与设备：设备维护之后为设备 OEE、模具工装
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(310, 923, '设备 OEE', 'C', 'oee', 'mes/oee/index', 'mes:oee:list', 'FieldTimeOutlined', 7, 1, 1, NOW(), NOW()),
(311, 923, '模具工装', 'C', 'mold', 'mes/mold/index', 'mes:mold:list', 'BlockOutlined', 8, 1, 1, NOW(), NOW()),
(3100, 310, '停机登记', 'F', NULL, NULL, 'mes:equipment:stop', NULL, 1, 1, 1, NOW(), NOW()),
(3110, 311, '模具新增', 'F', NULL, NULL, 'mes:mold:add', NULL, 1, 1, 1, NOW(), NOW()),
(3111, 311, '模具修改', 'F', NULL, NULL, 'mes:mold:edit', NULL, 2, 1, 1, NOW(), NOW()),
(3112, 311, '模具删除', 'F', NULL, NULL, 'mes:mold:remove', NULL, 3, 1, 1, NOW(), NOW()),
(3113, 311, '领用归还', 'F', NULL, NULL, 'mes:mold:issue', NULL, 4, 1, 1, NOW(), NOW());

-- 普通用户、总经理、生产计划员查看，车间主管全部
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 310), (2, 311), (3, 310), (3, 311), (7, 310), (7, 311),
(8, 310), (8, 311), (8, 3100), (8, 3110), (8, 3111), (8, 3112), (8, 3113);

-- ---------------------------------------------------------------- 促销、返利
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(149, '促销类型', 'scm_promo_type', 1, '促销活动的类型', NOW(), NOW()),
(150, '返利状态', 'scm_rebate_status', 1, '返利协议的状态', NOW(), NOW()),
(151, '返利兑付方式', 'scm_rebate_settle', 1, '返利结算后的兑付方式', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_promo_type', '折扣', 'DISCOUNT', 'blue', 1, 1, NOW(), NOW()),
('scm_promo_type', '满减', 'REDUCE', 'orange', 2, 1, NOW(), NOW()),
('scm_promo_type', '买赠', 'GIFT', 'magenta', 3, 1, NOW(), NOW()),
('scm_rebate_status', '执行中', 'ACTIVE', 'processing', 1, 1, NOW(), NOW()),
('scm_rebate_status', '已结算', 'SETTLED', 'green', 2, 1, NOW(), NOW()),
('scm_rebate_settle', '红字应收', 'CREDIT_NOTE', 'red', 1, 1, NOW(), NOW()),
('scm_rebate_settle', '下次订单抵扣', 'NEXT_ORDER', 'blue', 2, 1, NOW(), NOW()),
('fin_bill_source', '销售返利', 'REBATE', 'volcano', 12, 1, NOW(), NOW());

-- 销售：价目表之后为促销活动、销售返利
UPDATE sys_menu SET sort = sort + 2 WHERE parent_id = 902 AND sort >= 3;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(312, 902, '促销活动', 'C', 'promotion', 'scm/promotion/index', 'scm:promotion:list', 'GiftOutlined', 3, 1, 1, NOW(), NOW()),
(313, 902, '销售返利', 'C', 'rebate', 'scm/rebate/index', 'scm:rebate:list', 'RollbackOutlined', 4, 1, 1, NOW(), NOW()),
(3120, 312, '促销维护', 'F', NULL, NULL, 'scm:promotion:edit', NULL, 1, 1, 1, NOW(), NOW()),
(3130, 313, '返利协议维护', 'F', NULL, NULL, 'scm:rebate:add', NULL, 1, 1, 1, NOW(), NOW()),
(3131, 313, '返利结算', 'F', NULL, NULL, 'scm:rebate:settle', NULL, 2, 1, 1, NOW(), NOW());

-- 普通用户查看；销售员维护促销和返利协议；总经理、会计结算
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 312), (2, 313), (3, 312), (3, 313), (3, 3131),
(5, 312), (5, 313), (5, 3120), (5, 3130), (9, 313), (9, 3131);

-- ---------------------------------------------------------------- 渠道（经销商门户）、外部订单导入
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(152, '销售渠道', 'scm_channel', 1, '客户的销售渠道：经销商可开通经销商门户', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_channel', '直销', 'DIRECT', 'default', 1, 1, NOW(), NOW()),
('scm_channel', '经销商', 'DEALER', 'purple', 2, 1, NOW(), NOW()),
('scm_order_source', '经销商订货', 'DEALER', 'purple', 8, 1, NOW(), NOW()),
('scm_order_source', '外部导入', 'IMPORT', 'lime', 9, 1, NOW(), NOW());

-- 销售：销售返利之后为渠道库存；客户资料中开通经销商门户账号；经销商门户（订货、渠道上报）
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 902 AND sort >= 5;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(314, 902, '渠道库存', 'C', 'channel', 'scm/channel/index', 'scm:channel:list', 'ClusterOutlined', 5, 1, 1, NOW(), NOW()),
(1493, 149, '门户账号', 'F', NULL, NULL, 'scm:customer:portal', NULL, 4, 1, 1, NOW(), NOW()),
(14, 0, '经销商门户', 'M', '/dealer', NULL, NULL, 'ShopOutlined', 14, 1, 1, NOW(), NOW()),
(320, 14, '我要订货', 'C', 'order', 'dealer/order/index', 'dealer:order:list', 'ShoppingCartOutlined', 1, 1, 1, NOW(), NOW()),
(321, 14, '库存销量上报', 'C', 'report', 'dealer/report/index', 'dealer:report:list', 'BarChartOutlined', 2, 1, 1, NOW(), NOW()),
(3200, 320, '下单', 'F', NULL, NULL, 'dealer:order:add', NULL, 1, 1, 1, NOW(), NOW()),
(3210, 321, '上报', 'F', NULL, NULL, 'dealer:report:edit', NULL, 1, 1, 1, NOW(), NOW());

INSERT INTO sys_role (id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(16, '经销商', 'dealer', 16, 5, 1, '经销商门户账号：按价目表订货（自动应用促销、可抵扣返利）、查看订单进度、上报渠道库存和销量，只能看到本经销商的数据', NOW(), NOW());

-- 经销商门户；普通用户、总经理、销售员查看渠道库存，销售员开通门户账号
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(16, 14), (16, 320), (16, 321), (16, 3200), (16, 3210),
(2, 314), (3, 314), (5, 314), (5, 1493);

-- ---------------------------------------------------------------- 仓储作业：库位类型与区域、上架策略、波次拣货
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(153, '库位类型', 'wms_location_type', 1, '存储位、拣货位、收发货暂存位、不良品位', NOW(), NOW()),
(154, '波次状态', 'scm_wave_status', 1, '拣货波次的状态', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_location_type', '存储位', 'STORAGE', 'blue', 1, 1, NOW(), NOW()),
('wms_location_type', '拣货位', 'PICK', 'green', 2, 1, NOW(), NOW()),
('wms_location_type', '收货暂存', 'RECEIVE', 'cyan', 3, 1, NOW(), NOW()),
('wms_location_type', '发货暂存', 'SHIP', 'purple', 4, 1, NOW(), NOW()),
('wms_location_type', '不良品位', 'DEFECT', 'red', 5, 1, NOW(), NOW()),
('scm_wave_status', '拣货中', 'PICKING', 'processing', 1, 1, NOW(), NOW()),
('scm_wave_status', '已完成', 'DONE', 'green', 2, 1, NOW(), NOW()),
('scm_wave_status', '已取消', 'CANCELLED', 'default', 3, 1, NOW(), NOW());

-- 销售：发货通知之后为波次拣货
UPDATE sys_menu SET sort = sort + 1 WHERE parent_id = 902 AND sort >= 8;
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(315, 902, '波次拣货', 'C', 'wave', 'scm/wave/index', 'scm:wave:list', 'MergeCellsOutlined', 8, 1, 1, NOW(), NOW()),
(3150, 315, '波次维护', 'F', NULL, NULL, 'scm:wave:add', NULL, 1, 1, 1, NOW(), NOW()),
(3151, 315, '完成拣货', 'F', NULL, NULL, 'scm:wave:pick', NULL, 2, 1, 1, NOW(), NOW());

-- 普通用户、总经理查看，仓库管理员全部
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 315), (3, 315), (6, 315), (6, 3150), (6, 3151);

-- 移动作业（手机 / PDA）：独立的移动端布局，页面权限沿用对应的业务操作权限
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(15, 0, '移动作业', 'M', '/m', NULL, NULL, 'MobileOutlined', 15, 1, 1, NOW(), NOW()),
(330, 15, '扫码收货', 'C', 'receive', 'mobile/receive/index', 'scm:purchaseIn:add', 'ScanOutlined', 1, 1, 1, NOW(), NOW()),
(331, 15, '上架', 'C', 'putaway', 'mobile/putaway/index', 'wms:transfer:add', 'VerticalAlignTopOutlined', 2, 1, 1, NOW(), NOW()),
(332, 15, '拣货', 'C', 'pick', 'mobile/pick/index', 'scm:delivery:pick', 'ShoppingCartOutlined', 3, 1, 1, NOW(), NOW()),
(333, 15, '盘点', 'C', 'check', 'mobile/check/index', 'wms:check:edit', 'AuditOutlined', 4, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 15), (6, 330), (6, 331), (6, 332), (6, 333);

-- 装箱（发货通知的按钮）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(2713, 271, '装箱', 'F', NULL, NULL, 'scm:delivery:pack', NULL, 3, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(6, 2713);

-- 存货计价方法
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(155, '存货计价方法', 'wms_cost_method', 1, '物料的存货计价方法，月末成本核算时按此计算出库成本', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_cost_method', '月末一次加权平均', 'WEIGHTED', 'blue', 1, 1, NOW(), NOW()),
('wms_cost_method', '移动加权平均', 'MOVING', 'cyan', 2, 1, NOW(), NOW()),
('wms_cost_method', '先进先出', 'FIFO', 'purple', 3, 1, NOW(), NOW());

-- 期初入库：系统启用时导入的期初库存
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_in_type', '期初入库', 'OPENING', 'gold', 10, 1, NOW(), NOW());

-- 规格属性（多规格物料）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(342, 914, '规格属性', 'C', 'attribute', 'wms/attribute/index', 'wms:attribute:list', 'TagsOutlined', 4, 1, 1, NOW(), NOW()),
(3420, 342, '规格属性维护', 'F', NULL, NULL, 'wms:attribute:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 342), (3, 342), (4, 342), (6, 342), (6, 3420);
INSERT INTO wms_attribute (id, name, sort, status, create_time, update_time) VALUES
(1, '颜色', 1, 1, NOW(), NOW()),
(2, '尺码', 2, 1, NOW(), NOW());
INSERT INTO wms_attribute_value (attribute_id, attr_value, code, sort, create_time, update_time) VALUES
(1, '黑色', 'BK', 1, NOW(), NOW()),
(1, '白色', 'WH', 2, NOW(), NOW()),
(1, '红色', 'RD', 3, NOW(), NOW()),
(1, '蓝色', 'BL', 4, NOW(), NOW()),
(2, 'S', 'S', 1, NOW(), NOW()),
(2, 'M', 'M', 2, NOW(), NOW()),
(2, 'L', 'L', 3, NOW(), NOW()),
(2, 'XL', 'XL', 4, NOW(), NOW()),
(2, 'XXL', 'XXL', 5, NOW(), NOW());

-- 组装拆卸单
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(343, 911, '组装拆卸', 'C', 'assembly', 'wms/assembly/index', 'wms:assembly:list', 'BuildOutlined', 5, 1, 1, NOW(), NOW()),
(3430, 343, '组装拆卸维护', 'F', NULL, NULL, 'wms:assembly:add', NULL, 1, 1, 1, NOW(), NOW()),
(3431, 343, '组装拆卸确认', 'F', NULL, NULL, 'wms:assembly:confirm', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 343), (3, 343), (6, 343), (6, 3430), (6, 3431), (9, 343);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(156, '组装拆卸种类', 'wms_assembly_kind', 1, '组装、拆卸', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('wms_assembly_kind', '组装', 'ASSEMBLE', 'blue', 1, 1, NOW(), NOW()),
('wms_assembly_kind', '拆卸', 'DISASSEMBLE', 'orange', 2, 1, NOW(), NOW()),
('wms_in_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW()),
('wms_out_type', '组装拆卸', 'ASSEMBLY', 'cyan', 11, 1, NOW(), NOW());

-- 销售提成：规则与按月计算（销售 → 销售提成）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(344, 902, '销售提成', 'C', 'commission', 'fin/commission/index', 'fin:commission:list', 'PercentageOutlined', 20, 1, 1, NOW(), NOW()),
(3440, 344, '提成规则维护', 'F', NULL, NULL, 'fin:commission:rule', NULL, 1, 1, 1, NOW(), NOW()),
(3441, 344, '提成计算与确认', 'F', NULL, NULL, 'fin:commission:edit', NULL, 2, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 344), (3, 344), (9, 344), (9, 3440), (9, 3441), (11, 344);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(157, '提成依据', 'fin_commission_basis', 1, '销售提成的计算依据', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('fin_commission_basis', '销售额', 'SALES', 'blue', 1, 1, NOW(), NOW()),
('fin_commission_basis', '毛利', 'PROFIT', 'green', 2, 1, NOW(), NOW()),
('fin_commission_basis', '回款', 'RECEIPT', 'gold', 3, 1, NOW(), NOW());

-- 价格控制方式
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(158, '价格控制方式', 'scm_price_control_mode', 1, '超出最低销售价 / 最高采购价时的处理', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('scm_price_control_mode', '提示', 'WARN', 'orange', 1, 1, NOW(), NOW()),
('scm_price_control_mode', '禁止保存', 'BLOCK', 'red', 2, 1, NOW(), NOW());

-- 自定义字段
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(345, 1, '自定义字段', 'C', 'custom-field', 'system/custom-field/index', 'sys:customField:list', 'FormOutlined', 11, 1, 1, NOW(), NOW()),
(3450, 345, '自定义字段维护', 'F', NULL, NULL, 'sys:customField:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 345);
INSERT INTO sys_dict_type (id, name, type, status, remark, create_time, update_time) VALUES
(159, '自定义字段类型', 'sys_custom_field_type', 1, '自定义字段的输入类型', NOW(), NOW());
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tag_color, sort, status, create_time, update_time) VALUES
('sys_custom_field_type', '文本', 'TEXT', 'default', 1, 1, NOW(), NOW()),
('sys_custom_field_type', '多行文本', 'TEXTAREA', 'default', 2, 1, NOW(), NOW()),
('sys_custom_field_type', '数字', 'NUMBER', 'blue', 3, 1, NOW(), NOW()),
('sys_custom_field_type', '日期', 'DATE', 'cyan', 4, 1, NOW(), NOW()),
('sys_custom_field_type', '下拉选择', 'SELECT', 'purple', 5, 1, NOW(), NOW()),
('sys_custom_field_type', '是否', 'BOOL', 'green', 6, 1, NOW(), NOW());

-- 业务参数
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(346, 1, '业务参数', 'C', 'biz-rule', 'system/biz-rule/index', 'sys:bizRule:list', 'SlidersOutlined', 12, 1, 1, NOW(), NOW()),
(3460, 346, '业务参数修改', 'F', NULL, NULL, 'sys:bizRule:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 346);

-- 凭证科目设置：自动凭证各用途使用的科目
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(340, 936, '凭证科目设置', 'C', 'account-rule', 'fin/account-rule/index', 'fin:accountRule:list', 'NodeIndexOutlined', 5, 1, 1, NOW(), NOW()),
(3400, 340, '科目设置修改', 'F', NULL, NULL, 'fin:accountRule:edit', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 340), (9, 340), (9, 3400);

-- 出纳管理：现金 / 银行日记账、资金日报
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(341, 933, '出纳管理', 'C', 'cashier', 'fin/cashier/index', 'fin:cashier:list', 'WalletOutlined', 0, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 341), (3, 341), (9, 341), (10, 341);

-- 业务报表：收发存汇总（库存查询）、往来对账单（应收应付）
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(347, 912, '收发存汇总', 'C', 'in-out', 'wms/inout/index', 'wms:inout:list', 'SwapOutlined', 5, 1, 1, NOW(), NOW()),
(348, 932, '往来对账单', 'C', 'partner-statement', 'fin/partner-statement/index', 'fin:partnerStatement:list', 'AuditOutlined', 8, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 347), (3, 347), (4, 347), (6, 347), (9, 347),
(2, 348), (3, 348), (9, 348), (10, 348);

-- 数据大屏：生产车间、仓储物流、销售经营、质量管理，投屏链接
INSERT INTO sys_menu (id, parent_id, name, type, path, component, perms, icon, sort, visible, status, create_time, update_time) VALUES
(349, 11, '数据大屏', 'C', 'screen', 'bi/screen/index', 'bi:screen:list', 'FundProjectionScreenOutlined', 0, 1, 1, NOW(), NOW()),
(3490, 349, '投屏链接管理', 'F', NULL, NULL, 'bi:screen:share', NULL, 1, 1, 1, NOW(), NOW());
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 349), (3, 3490), (6, 349);

ALTER TABLE sys_dept AUTO_INCREMENT = 200;
ALTER TABLE sys_user AUTO_INCREMENT = 100;
ALTER TABLE sys_role AUTO_INCREMENT = 100;
ALTER TABLE sys_menu AUTO_INCREMENT = 10000;
ALTER TABLE sys_dict_type AUTO_INCREMENT = 300;
ALTER TABLE wms_warehouse AUTO_INCREMENT = 100;
ALTER TABLE wms_attribute AUTO_INCREMENT = 100;
ALTER TABLE scm_supplier AUTO_INCREMENT = 100;
ALTER TABLE scm_customer AUTO_INCREMENT = 100;
ALTER TABLE mes_work_center AUTO_INCREMENT = 100;
ALTER TABLE fin_account AUTO_INCREMENT = 1000;
ALTER TABLE hr_position AUTO_INCREMENT = 100;
ALTER TABLE hr_shift AUTO_INCREMENT = 100;
ALTER TABLE hr_employee AUTO_INCREMENT = 100;
ALTER TABLE bi_alert_rule AUTO_INCREMENT = 100;
