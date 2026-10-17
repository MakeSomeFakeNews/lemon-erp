package com.lemon.erp.modules.wms.model;

import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 仓库模块的通用约定
 */
public final class WmsConstants {

    /** 未指定库位 */
    public static final long NO_LOCATION = 0L;

    /** 未启用批次管理时的批次号 */
    public static final String NO_BATCH = "";

    /**
     * 入库类型（字典 wms_in_type）：采购入库、生产入库、销售退货、生产退料、委外收回由业务单据生成；手工只能做其他入库
     */
    public static final Set<String> IN_TYPES = Set.of("PURCHASE", "PRODUCTION", "SALES_RETURN", "PRODUCTION_RETURN", "OUTSOURCE_RETURN", "SCRAP_IN", "CONSIGN_IN", "OPENING", "ASSEMBLY", "OTHER");
    /** 期初入库：系统启用时导入的期初库存，日期为启用期间的前一天 */
    public static final String OPENING = "OPENING";
    /** 组装拆卸：组装拆卸单生成的出库、入库单 */
    public static final String ASSEMBLY = "ASSEMBLY";
    /** 出库类型（字典 wms_out_type）：销售出库、生产领料、采购退货、项目领料、委外发料、服务领用、部门领用（低值易耗品）由业务单据生成；手工只能做其他出库 */
    public static final Set<String> OUT_TYPES = Set.of("SALES", "PRODUCTION", "PURCHASE_RETURN", "PROJECT", "OUTSOURCE", "SERVICE", "SCRAP", "CONSIGN", "CONSUME", "ASSEMBLY", "OTHER");
    /** 其他入库 / 其他出库、报废出库、寄售入库：可以手工新增的类型 */
    public static final String OTHER = "OTHER";
    public static final String SCRAP = "SCRAP";
    public static final String CONSIGN_IN = "CONSIGN_IN";
    /** 手工入库允许的类型：其余类型由业务单据生成，手工做会绕过供应链、生产的应收应付和成本归集 */
    public static final Set<String> MANUAL_IN_TYPES = Set.of(OTHER, CONSIGN_IN);
    /** 手工出库允许的类型 */
    public static final Set<String> MANUAL_OUT_TYPES = Set.of(OTHER, SCRAP);
    /** 收发类别（业务字典）：其他入库用入库类别，其他出库、报废出库用出库类别，成本凭证按类别取对方科目 */
    public static final String IN_CATEGORY_DICT = "wms_in_category";
    public static final String OUT_CATEGORY_DICT = "wms_out_category";
    /** 不计入可用库存的仓库类型：不良品仓、客户寄售仓、供应商寄售仓 */
    public static final Set<String> UNAVAILABLE_WAREHOUSE_TYPES = Set.of("DEFECT", "CONSIGN_OUT", "CONSIGN_IN");
    /** 不能发出过期批次的出库类型（报废、其他出库、采购退货可以） */
    public static final Set<String> NO_EXPIRED_OUT_TYPES = Set.of("SALES", "PRODUCTION", "PROJECT", "OUTSOURCE", "SERVICE");

    /** 单据数据权限：制单人部门 + 制单人 */
    public static final DataPermissionRule STOCK_DOC_RULE = new DataPermissionRule("wms_stock_doc", "dept_id", "create_by");
    public static final DataPermissionRule CHECK_RULE = new DataPermissionRule("wms_check", "dept_id", "create_by");

    /** 序列号分隔符：逗号（含全角）、分号、空白、换行，方便扫码枪逐行录入 */
    private static final Pattern SERIAL_SEPARATOR = Pattern.compile("[,，;；\\s]+");

    private WmsConstants() {
    }

    /** 手工单据允许的出入库类型；调拨为空 */
    public static Set<String> manualTypes(StockBizType kind) {
        return switch (kind) {
            case IN -> MANUAL_IN_TYPES;
            case OUT -> MANUAL_OUT_TYPES;
            default -> Set.of();
        };
    }

    /** 该出入库类型使用的收发类别字典，不能填写收发类别时为空 */
    public static String ioCategoryDict(StockBizType kind, String bizType) {
        if (kind == StockBizType.IN && OTHER.equals(bizType)) {
            return IN_CATEGORY_DICT;
        }
        if (kind == StockBizType.OUT && (OTHER.equals(bizType) || SCRAP.equals(bizType))) {
            return OUT_CATEGORY_DICT;
        }
        return null;
    }

    public static long locationOrNone(Long locationId) {
        return locationId == null ? NO_LOCATION : locationId;
    }

    public static String batchOrNone(String batchNo) {
        return batchNo == null ? NO_BATCH : batchNo.trim();
    }

    /** 拆分序列号文本，去掉空项，保留顺序（不去重，由调用方检查重复） */
    public static List<String> splitSerials(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return Arrays.stream(SERIAL_SEPARATOR.split(text.trim())).filter(StringUtils::hasText).toList();
    }

    public static String joinSerials(List<String> serials) {
        return serials == null || serials.isEmpty() ? null : String.join(",", serials);
    }
}
