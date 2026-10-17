package com.lemon.erp.modules.wms.model;

import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 仓库模块的通用约定
 */
public final class WmsConstants {

    /** 未指定库位 */
    public static final long NO_LOCATION = 0L;

    /** 未启用批次管理时的批次号 */
    public static final String NO_BATCH = "";

    /** 收发类别（业务字典）：其他入库用入库类别，其他出库、报废出库用出库类别，成本凭证按类别取对方科目 */
    public static final String IN_CATEGORY_DICT = "wms_in_category";
    public static final String OUT_CATEGORY_DICT = "wms_out_category";
    /** 单据数据权限：制单人部门 + 制单人 */
    public static final DataPermissionRule STOCK_DOC_RULE = new DataPermissionRule("wms_stock_doc", "dept_id", "create_by");
    public static final DataPermissionRule CHECK_RULE = new DataPermissionRule("wms_check", "dept_id", "create_by");

    /** 序列号分隔符：逗号（含全角）、分号、空白、换行，方便扫码枪逐行录入 */
    private static final Pattern SERIAL_SEPARATOR = Pattern.compile("[,，;；\\s]+");

    private WmsConstants() {
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
