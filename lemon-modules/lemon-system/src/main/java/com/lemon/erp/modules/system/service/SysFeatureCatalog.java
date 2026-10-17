package com.lemon.erp.modules.system.service;

import java.util.Arrays;
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

    /** 功能模块标识 */
    enum Key {
        /** 基础平台 */
        CORE,
        /** 采购 */
        PURCHASE,
        /** 寻源 */
        SOURCING,
        /** 供应商门户 */
        SUPPLIER_PORTAL,
        /** 销售 */
        SALES,
        /** 渠道分销 */
        CHANNEL,
        /** 销售提成 */
        COMMISSION,
        /** 库存 */
        INVENTORY,
        /** 仓储作业 */
        WAREHOUSE_OPS,
        /** 生产 */
        PRODUCTION,
        /** 工程 */
        ENGINEERING,
        /** 设备 */
        EQUIPMENT,
        /** 质量 */
        QUALITY,
        /** 总账 */
        GL,
        /** 应收应付 */
        ARAP,
        /** 资金费用 */
        FUND,
        /** 资产 */
        ASSET,
        /** 成本核算 */
        COST,
        /** 预算分析 */
        BUDGET,
        /** 客户关系 */
        CRM,
        /** 项目 */
        PROJECT,
        /** 人力资源 */
        HR,
        /** 薪酬福利 */
        PAYROLL,
        /** 商业智能 */
        BI,
        /** 数据大屏 */
        SCREEN
    }

    /**
     * 名称、分类、说明是消息键（system 资源包），输出时 {@code Messages.get}
     *
     * @param key      标识
     * @param group    分类（页面上分组显示）
     * @param required 必选（基础平台）
     * @param menus    包含的菜单（含其下的全部菜单）
     * @param requires 依赖的模块，勾选时一并勾选
     */
    record Feature(String key, String name, String group, String description, boolean required, List<Long> menus, List<String> requires) {
    }

    /**
     * 名称、说明是消息键
     *
     * @param features 包含的模块（依赖的模块和基础平台自动加入）
     */
    record Industry(String key, String name, String description, List<String> features) {
    }

    private static Feature f(Key key, String name, String group, String description, List<Long> menus, Key... requires) {
        return new Feature(key.name(), name, group, description, false, menus, names(requires));
    }

    private static List<String> names(Key... keys) {
        return Arrays.stream(keys).map(Key::name).toList();
    }

    static final List<Feature> FEATURES = List.of(
            new Feature(Key.CORE.name(), "system.label.corePlatform", "system.label.core", "system.label.systemAdministrationApprovalWorkflowsMasterData", true,
                    List.of(1L, 2L, 3L, 914L, 903L), List.of()),
            // 供应链
            f(Key.PURCHASE, "system.label.purchasing", "system.label.supplyChain", "system.label.purchaseRequisitionsPurchaseOrdersDeliveryNotices", List.of(901L), Key.INVENTORY),
            f(Key.SOURCING, "system.label.sourcing", "system.label.supplyChain", "system.label.rfqsPriceComparisonTenderingPurchaseContracts", List.of(904L), Key.PURCHASE),
            f(Key.SUPPLIER_PORTAL, "system.label.supplierPortal", "system.label.supplyChain", "system.label.suppliersConfirmOrdersShipReconcileBid", List.of(13L), Key.PURCHASE),
            f(Key.SALES, "system.label.sales", "system.label.supplyChain", "system.label.quotationsPriceListsSalesOrdersDelivery",
                    List.of(269L, 270L, 143L, 271L, 315L, 144L, 145L, 146L, 272L), Key.INVENTORY),
            f(Key.CHANNEL, "system.label.channelDistribution", "system.label.supplyChain", "system.label.promotionsRebatesChannelInventoryConsignmentSettlement", List.of(312L, 313L, 314L, 287L, 14L), Key.SALES),
            f(Key.COMMISSION, "system.label.salesCommission", "system.label.supplyChain", "system.label.calculateSalespersonCommissionSalesGrossProfit", List.of(344L), Key.SALES),
            // 仓储
            f(Key.INVENTORY, "system.label.inventory", "system.label.warehousing", "system.label.inboundOutboundTransfersStockCountsAssembly", List.of(911L, 912L)),
            f(Key.WAREHOUSE_OPS, "system.label.warehouseOperations", "system.label.warehousing", "system.label.stockReservationFreezingMobilePdaScanning", List.of(913L, 15L), Key.INVENTORY),
            // 生产与质量
            f(Key.PRODUCTION, "system.label.production", "system.label.productionQuality", "system.label.productionPlanningMrpCapacitySchedulingWork",
                    List.of(921L, 922L, 166L, 167L), Key.INVENTORY, Key.PURCHASE),
            f(Key.ENGINEERING, "system.label.engineering", "system.label.productionQuality", "system.label.engineeringChangesTechnicalDocuments", List.of(305L, 306L), Key.PRODUCTION),
            f(Key.EQUIPMENT, "system.label.equipment", "system.label.productionQuality", "system.label.equipmentLedgerMaintenanceOeeMoldsTooling", List.of(164L, 165L, 310L, 311L), Key.PRODUCTION),
            f(Key.QUALITY, "system.label.quality", "system.label.productionQuality", "system.label.inspectionPlansInspectionsNonconformanceHandlingCapa", List.of(12L), Key.INVENTORY),
            // 财务
            f(Key.GL, "system.label.generalLedger", "system.label.finance", "system.label.vouchersLedgersFinancialStatementsPeriodClose", List.of(931L, 936L)),
            f(Key.ARAP, "system.label.receivablesPayables", "system.label.finance", "system.label.receivablesPayablesReceiptsPaymentsAdvancesInvoices", List.of(932L), Key.GL),
            f(Key.FUND, "system.label.fundsExpenses", "system.label.finance", "system.label.cashierExpenseReimbursementEmployeeLoansBank", List.of(933L), Key.GL),
            f(Key.ASSET, "system.label.assets", "system.label.finance", "system.label.fixedAssetsDepreciationAssetCountsLow", List.of(187L, 307L, 308L), Key.GL),
            f(Key.COST, "system.label.costAccounting", "system.label.finance", "system.label.inventoryCostingStandardCostVarianceAnalysis", List.of(188L, 309L), Key.GL, Key.INVENTORY),
            f(Key.BUDGET, "system.label.budgetAnalysis", "system.label.finance", "system.label.budgetPreparationExecutionBusinessAnalysis", List.of(935L), Key.GL),
            // 客户与项目
            f(Key.CRM, "system.label.crm", "system.label.customersProjects", "system.label.customersLeadsOpportunitiesFollowUpsContracts", List.of(9L)),
            f(Key.PROJECT, "system.label.projects", "system.label.customersProjects", "system.label.projectPlansProgressResourcesTimesheetsCosts", List.of(10L)),
            // 人力资源
            f(Key.HR, "system.label.hr", "system.label.humanResources", "system.label.employeeRecordsLaborContractsRecruitingTraining", List.of(951L, 952L, 953L)),
            f(Key.PAYROLL, "system.label.payrollBenefits", "system.label.humanResources", "system.label.salaryProfilesPayrollSocialInsuranceSpecial", List.of(954L), Key.HR),
            // 分析
            f(Key.BI, "system.label.businessIntelligence", "system.label.analytics", "system.label.businessCockpitPivotAnalysisCustomReports",
                    List.of(260L, 261L, 262L, 263L, 264L, 265L)),
            f(Key.SCREEN, "system.label.dataScreens", "system.label.analytics", "system.label.dataScreensShopFloorWarehousingLogistics", List.of(349L)));

    static final List<Industry> INDUSTRIES = List.of(
            new Industry("DISCRETE", "system.label.discreteManufacturing", "system.label.machineryElectronicsEquipmentAssemblyOtherMake",
                    names(Key.PURCHASE, Key.SOURCING, Key.SUPPLIER_PORTAL, Key.SALES, Key.INVENTORY, Key.WAREHOUSE_OPS,
                            Key.PRODUCTION, Key.ENGINEERING, Key.EQUIPMENT, Key.QUALITY, Key.GL, Key.ARAP, Key.FUND,
                            Key.ASSET, Key.COST, Key.BUDGET, Key.HR, Key.PAYROLL, Key.BI, Key.SCREEN)),
            new Industry("PROCESS", "system.label.processManufacturing", "system.label.foodChemicalsPharmaceuticalsOtherBatchProduction",
                    names(Key.PURCHASE, Key.SALES, Key.INVENTORY, Key.WAREHOUSE_OPS, Key.PRODUCTION, Key.EQUIPMENT,
                            Key.QUALITY, Key.GL, Key.ARAP, Key.FUND, Key.ASSET, Key.COST, Key.BUDGET, Key.HR, Key.PAYROLL,
                            Key.BI, Key.SCREEN)),
            new Industry("TRADING", "system.label.tradingWholesale", "system.label.mainlyPurchasingWarehousingDistributionWithoutProduction",
                    names(Key.PURCHASE, Key.SOURCING, Key.SALES, Key.CHANNEL, Key.COMMISSION, Key.INVENTORY,
                            Key.WAREHOUSE_OPS, Key.GL, Key.ARAP, Key.FUND, Key.BUDGET, Key.CRM, Key.BI, Key.SCREEN)),
            new Industry("RETAIL", "system.label.eCommerceRetail", "system.label.mainlyMultiChannelOrdersWarehousingShipping",
                    names(Key.PURCHASE, Key.SALES, Key.CHANNEL, Key.INVENTORY, Key.WAREHOUSE_OPS, Key.GL, Key.ARAP, Key.FUND,
                            Key.BI, Key.SCREEN)),
            new Industry("PROJECT", "system.label.engineeringProjects", "system.label.constructionInstallationSystemIntegrationOtherProject",
                    names(Key.PURCHASE, Key.SALES, Key.INVENTORY, Key.PROJECT, Key.CRM, Key.GL, Key.ARAP, Key.FUND,
                            Key.ASSET, Key.BUDGET, Key.HR, Key.PAYROLL, Key.BI)),
            new Industry("SERVICE", "system.label.professionalServices", "system.label.softwareConsultingDesignOtherLaborProject",
                    names(Key.SALES, Key.CRM, Key.PROJECT, Key.GL, Key.ARAP, Key.FUND, Key.BUDGET, Key.HR, Key.PAYROLL, Key.BI)),
            new Industry("FULL", "system.label.allFeatures", "system.label.enableAllFeatureModules",
                    FEATURES.stream().map(Feature::key).toList()));
}
