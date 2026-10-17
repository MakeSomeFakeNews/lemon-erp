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

    private static Feature f(String key, String name, String group, String description, List<Long> menus, String... requires) {
        return new Feature(key, name, group, description, false, menus, List.of(requires));
    }

    static final List<Feature> FEATURES = List.of(
            new Feature("CORE", "system.label.corePlatform", "system.label.core", "system.label.systemAdministrationApprovalWorkflowsMasterData", true,
                    List.of(1L, 2L, 3L, 914L, 903L), List.of()),
            // 供应链
            f("PURCHASE", "system.label.purchasing", "system.label.supplyChain", "system.label.purchaseRequisitionsPurchaseOrdersDeliveryNotices", List.of(901L), "INVENTORY"),
            f("SOURCING", "system.label.sourcing", "system.label.supplyChain", "system.label.rfqsPriceComparisonTenderingPurchaseContracts", List.of(904L), "PURCHASE"),
            f("SUPPLIER_PORTAL", "system.label.supplierPortal", "system.label.supplyChain", "system.label.suppliersConfirmOrdersShipReconcileBid", List.of(13L), "PURCHASE"),
            f("SALES", "system.label.sales", "system.label.supplyChain", "system.label.quotationsPriceListsSalesOrdersDelivery",
                    List.of(269L, 270L, 143L, 271L, 315L, 144L, 145L, 146L, 272L), "INVENTORY"),
            f("CHANNEL", "system.label.channelDistribution", "system.label.supplyChain", "system.label.promotionsRebatesChannelInventoryConsignmentSettlement", List.of(312L, 313L, 314L, 287L, 14L), "SALES"),
            f("COMMISSION", "system.label.salesCommission", "system.label.supplyChain", "system.label.calculateSalespersonCommissionSalesGrossProfit", List.of(344L), "SALES"),
            // 仓储
            f("INVENTORY", "system.label.inventory", "system.label.warehousing", "system.label.inboundOutboundTransfersStockCountsAssembly", List.of(911L, 912L)),
            f("WAREHOUSE_OPS", "system.label.warehouseOperations", "system.label.warehousing", "system.label.stockReservationFreezingMobilePdaScanning", List.of(913L, 15L), "INVENTORY"),
            // 生产与质量
            f("PRODUCTION", "system.label.production", "system.label.productionQuality", "system.label.productionPlanningMrpCapacitySchedulingWork",
                    List.of(921L, 922L, 166L, 167L), "INVENTORY", "PURCHASE"),
            f("ENGINEERING", "system.label.engineering", "system.label.productionQuality", "system.label.engineeringChangesTechnicalDocuments", List.of(305L, 306L), "PRODUCTION"),
            f("EQUIPMENT", "system.label.equipment", "system.label.productionQuality", "system.label.equipmentLedgerMaintenanceOeeMoldsTooling", List.of(164L, 165L, 310L, 311L), "PRODUCTION"),
            f("QUALITY", "system.label.quality", "system.label.productionQuality", "system.label.inspectionPlansInspectionsNonconformanceHandlingCapa", List.of(12L), "INVENTORY"),
            // 财务
            f("GL", "system.label.generalLedger", "system.label.finance", "system.label.vouchersLedgersFinancialStatementsPeriodClose", List.of(931L, 936L)),
            f("ARAP", "system.label.receivablesPayables", "system.label.finance", "system.label.receivablesPayablesReceiptsPaymentsAdvancesInvoices", List.of(932L), "GL"),
            f("FUND", "system.label.fundsExpenses", "system.label.finance", "system.label.cashierExpenseReimbursementEmployeeLoansBank", List.of(933L), "GL"),
            f("ASSET", "system.label.assets", "system.label.finance", "system.label.fixedAssetsDepreciationAssetCountsLow", List.of(187L, 307L, 308L), "GL"),
            f("COST", "system.label.costAccounting", "system.label.finance", "system.label.inventoryCostingStandardCostVarianceAnalysis", List.of(188L, 309L), "GL", "INVENTORY"),
            f("BUDGET", "system.label.budgetAnalysis", "system.label.finance", "system.label.budgetPreparationExecutionBusinessAnalysis", List.of(935L), "GL"),
            // 客户与项目
            f("CRM", "system.label.crm", "system.label.customersProjects", "system.label.customersLeadsOpportunitiesFollowUpsContracts", List.of(9L)),
            f("PROJECT", "system.label.projects", "system.label.customersProjects", "system.label.projectPlansProgressResourcesTimesheetsCosts", List.of(10L)),
            // 人力资源
            f("HR", "system.label.hr", "system.label.humanResources", "system.label.employeeRecordsLaborContractsRecruitingTraining", List.of(951L, 952L, 953L)),
            f("PAYROLL", "system.label.payrollBenefits", "system.label.humanResources", "system.label.salaryProfilesPayrollSocialInsuranceSpecial", List.of(954L), "HR"),
            // 分析
            f("BI", "system.label.businessIntelligence", "system.label.analytics", "system.label.businessCockpitPivotAnalysisCustomReports",
                    List.of(260L, 261L, 262L, 263L, 264L, 265L)),
            f("SCREEN", "system.label.dataScreens", "system.label.analytics", "system.label.dataScreensShopFloorWarehousingLogistics", List.of(349L)));

    static final List<Industry> INDUSTRIES = List.of(
            new Industry("DISCRETE", "system.label.discreteManufacturing", "system.label.machineryElectronicsEquipmentAssemblyOtherMake",
                    List.of("PURCHASE", "SOURCING", "SUPPLIER_PORTAL", "SALES", "INVENTORY", "WAREHOUSE_OPS", "PRODUCTION", "ENGINEERING",
                            "EQUIPMENT", "QUALITY", "GL", "ARAP", "FUND", "ASSET", "COST", "BUDGET", "HR", "PAYROLL", "BI", "SCREEN")),
            new Industry("PROCESS", "system.label.processManufacturing", "system.label.foodChemicalsPharmaceuticalsOtherBatchProduction",
                    List.of("PURCHASE", "SALES", "INVENTORY", "WAREHOUSE_OPS", "PRODUCTION", "EQUIPMENT", "QUALITY",
                            "GL", "ARAP", "FUND", "ASSET", "COST", "BUDGET", "HR", "PAYROLL", "BI", "SCREEN")),
            new Industry("TRADING", "system.label.tradingWholesale", "system.label.mainlyPurchasingWarehousingDistributionWithoutProduction",
                    List.of("PURCHASE", "SOURCING", "SALES", "CHANNEL", "COMMISSION", "INVENTORY", "WAREHOUSE_OPS",
                            "GL", "ARAP", "FUND", "BUDGET", "CRM", "BI", "SCREEN")),
            new Industry("RETAIL", "system.label.eCommerceRetail", "system.label.mainlyMultiChannelOrdersWarehousingShipping",
                    List.of("PURCHASE", "SALES", "CHANNEL", "INVENTORY", "WAREHOUSE_OPS", "GL", "ARAP", "FUND", "BI", "SCREEN")),
            new Industry("PROJECT", "system.label.engineeringProjects", "system.label.constructionInstallationSystemIntegrationOtherProject",
                    List.of("PURCHASE", "SALES", "INVENTORY", "PROJECT", "CRM", "GL", "ARAP", "FUND", "ASSET", "BUDGET", "HR", "PAYROLL", "BI")),
            new Industry("SERVICE", "system.label.professionalServices", "system.label.softwareConsultingDesignOtherLaborProject",
                    List.of("SALES", "CRM", "PROJECT", "GL", "ARAP", "FUND", "BUDGET", "HR", "PAYROLL", "BI")),
            new Industry("FULL", "system.label.allFeatures", "system.label.enableAllFeatureModules",
                    FEATURES.stream().map(Feature::key).toList()));
}
