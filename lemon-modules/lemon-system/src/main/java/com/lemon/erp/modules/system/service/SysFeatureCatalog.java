package com.lemon.erp.modules.system.service;

import java.util.List;

/**
 * 功能模块与行业模板：新建租户、编辑套餐时按模块勾选功能，按行业一键选择常用模块。
 * <p>
 * 模块按菜单 ID 定义（sys_menu 中的模块目录、侧边栏分组或页面），包含其下的全部页面和按钮；
 * 新增页面时放在已有分组下会自动归入对应模块，放在新位置时需要在这里登记（{@code FeatureCatalogTests} 检查每个页面都属于某个模块）
 */
final class SysFeatureCatalog {

    private SysFeatureCatalog() {
    }

    /**
     * @param key      标识
     * @param group    分类（页面上分组显示）
     * @param required 必选（基础平台）
     * @param menus    包含的菜单（含其下的全部菜单）
     * @param requires 依赖的模块，勾选时一并勾选
     */
    record Feature(String key, String name, String group, String description, boolean required, List<Long> menus, List<String> requires) {
    }

    /** @param features 包含的模块（依赖的模块和基础平台自动加入） */
    record Industry(String key, String name, String description, List<String> features) {
    }

    private static Feature f(String key, String name, String group, String description, List<Long> menus, String... requires) {
        return new Feature(key, name, group, description, false, menus, List.of(requires));
    }

    static final List<Feature> FEATURES = List.of(
            new Feature("CORE", "基础平台", "基础", "系统管理、流程审批、物料与仓库、客户与供应商等基础资料", true,
                    List.of(1L, 2L, 3L, 914L, 903L), List.of()),
            // 供应链
            f("PURCHASE", "采购管理", "供应链", "请购、采购订单、送货通知、采购入库与退货、补货建议", List.of(901L), "INVENTORY"),
            f("SOURCING", "采购寻源", "供应链", "询价比价、招标采购、采购合同", List.of(904L), "PURCHASE"),
            f("SUPPLIER_PORTAL", "供应商门户", "供应链", "供应商在线确认订单、送货、对账、投标、处理索赔", List.of(13L), "PURCHASE"),
            f("SALES", "销售管理", "供应链", "报价、价目表、销售订单、发货通知与波次拣货、出库退货、物流发运、欠交跟踪",
                    List.of(269L, 270L, 143L, 271L, 315L, 144L, 145L, 146L, 272L), "INVENTORY"),
            f("CHANNEL", "渠道分销", "供应链", "促销、返利、渠道库存、寄售结算、经销商门户", List.of(312L, 313L, 314L, 287L, 14L), "SALES"),
            f("COMMISSION", "销售提成", "供应链", "按销售额、毛利、回款计算业务员提成", List.of(344L), "SALES"),
            // 仓储
            f("INVENTORY", "库存管理", "仓储", "出入库、调拨、盘点、组装拆卸，库存查询、流水、批次序列号、收发存汇总", List.of(911L, 912L)),
            f("WAREHOUSE_OPS", "仓储作业", "仓储", "库存预留与冻结、手机 / PDA 扫码收货、上架、拣货、盘点", List.of(913L, 15L), "INVENTORY"),
            // 生产与质量
            f("PRODUCTION", "生产管理", "生产与质量", "生产计划、MRP、产能排程、生产工单、齐套分析、物料清单、工作中心",
                    List.of(921L, 922L, 166L, 167L), "INVENTORY", "PURCHASE"),
            f("ENGINEERING", "工程技术", "生产与质量", "工程变更、技术文档", List.of(305L, 306L), "PRODUCTION"),
            f("EQUIPMENT", "设备管理", "生产与质量", "设备台账、设备维护、设备 OEE、模具工装", List.of(164L, 165L, 310L, 311L), "PRODUCTION"),
            f("QUALITY", "质量管理", "生产与质量", "检验方案与检验、不合格品处理、纠正预防、供应商索赔、质量分析与 SPC", List.of(12L), "INVENTORY"),
            // 财务
            f("GL", "总账", "财务", "凭证、账簿、财务报表、期末结账、会计科目与期初", List.of(931L, 936L)),
            f("ARAP", "应收应付", "财务", "应收应付、收付款、预收预付、发票、对账", List.of(932L), "GL"),
            f("FUND", "资金与费用", "财务", "出纳、费用报销、员工借款、银行对账、票据、资金计划", List.of(933L), "GL"),
            f("ASSET", "资产管理", "财务", "固定资产与折旧、资产盘点、低值易耗品", List.of(187L, 307L, 308L), "GL"),
            f("COST", "成本核算", "财务", "存货成本核算、标准成本与差异分析", List.of(188L, 309L), "GL", "INVENTORY"),
            f("BUDGET", "预算与分析", "财务", "预算编制与执行、经营分析", List.of(935L), "GL"),
            // 客户与项目
            f("CRM", "客户关系", "客户与项目", "客户、线索、商机、跟进、合同、服务工单、市场活动", List.of(9L)),
            f("PROJECT", "项目管理", "客户与项目", "项目计划进度、资源、工时、成本与核算", List.of(10L)),
            // 人力资源
            f("HR", "人事管理", "人力资源", "员工档案、劳动合同、招聘培训、考勤假勤、绩效", List.of(951L, 952L, 953L)),
            f("PAYROLL", "薪酬福利", "人力资源", "薪资档案、薪酬核算、社保、专项附加扣除", List.of(954L), "HR"),
            // 分析
            f("BI", "商务智能", "分析", "经营驾驶舱、多维分析、自定义报表与仪表盘、决策支持、数据仓库",
                    List.of(260L, 261L, 262L, 263L, 264L, 265L)),
            f("SCREEN", "数据大屏", "分析", "生产车间、仓储物流、销售经营、质量管理大屏，投屏链接", List.of(349L)));

    static final List<Industry> INDUSTRIES = List.of(
            new Industry("DISCRETE", "离散制造", "机械、电子、设备组装等按订单或计划生产的企业",
                    List.of("PURCHASE", "SOURCING", "SUPPLIER_PORTAL", "SALES", "INVENTORY", "WAREHOUSE_OPS", "PRODUCTION", "ENGINEERING",
                            "EQUIPMENT", "QUALITY", "GL", "ARAP", "FUND", "ASSET", "COST", "BUDGET", "HR", "PAYROLL", "BI", "SCREEN")),
            new Industry("PROCESS", "流程加工", "食品、化工、医药等按配方批量生产，重视批次与保质期",
                    List.of("PURCHASE", "SALES", "INVENTORY", "WAREHOUSE_OPS", "PRODUCTION", "EQUIPMENT", "QUALITY",
                            "GL", "ARAP", "FUND", "ASSET", "COST", "BUDGET", "HR", "PAYROLL", "BI", "SCREEN")),
            new Industry("TRADING", "商贸批发", "以采购、仓储、分销为主，没有生产环节",
                    List.of("PURCHASE", "SOURCING", "SALES", "CHANNEL", "COMMISSION", "INVENTORY", "WAREHOUSE_OPS",
                            "GL", "ARAP", "FUND", "BUDGET", "CRM", "BI", "SCREEN")),
            new Industry("RETAIL", "电商零售", "多渠道接单、仓储发货为主",
                    List.of("PURCHASE", "SALES", "CHANNEL", "INVENTORY", "WAREHOUSE_OPS", "GL", "ARAP", "FUND", "BI", "SCREEN")),
            new Industry("PROJECT", "工程项目", "建筑安装、系统集成等按项目采购、施工和核算",
                    List.of("PURCHASE", "SALES", "INVENTORY", "PROJECT", "CRM", "GL", "ARAP", "FUND", "ASSET", "BUDGET", "HR", "PAYROLL", "BI")),
            new Industry("SERVICE", "专业服务", "软件、咨询、设计等以人工和项目为主",
                    List.of("SALES", "CRM", "PROJECT", "GL", "ARAP", "FUND", "BUDGET", "HR", "PAYROLL", "BI")),
            new Industry("FULL", "全部功能", "开通全部功能模块",
                    FEATURES.stream().map(Feature::key).toList()));
}
