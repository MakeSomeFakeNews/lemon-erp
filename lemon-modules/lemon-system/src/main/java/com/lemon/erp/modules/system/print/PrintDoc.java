package com.lemon.erp.modules.system.print;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一张待打印的单据，由前端 utils/print.js 组装（已按打印模板隐藏、排序、改名），值都是格式化后的文本。
 * <p>
 * key 是单据中的原始名称（表头字段、明细列、签字栏的名称），上传的 JRXML 按 key 取值；label / title 是打印时显示的名称。
 */
@Data
public class PrintDoc {

    /** 单据名称，如 采购订单 */
    private String title;

    /** 单号 */
    private String no;

    /** 公司名称，为空时不显示 */
    private String companyName;

    private String note;

    /** 条状单据（工资条）：一页打印多条，之间用虚线分隔 */
    private boolean slip;

    /** 表头字段 */
    private List<Item> fields = new ArrayList<>();

    /** 明细列 */
    private List<Column> columns = new ArrayList<>();

    /** 明细行：列 key → 文本 */
    private List<Map<String, String>> rows = new ArrayList<>();

    /** 合计行：列 key → 文本，为空时不显示合计行 */
    private Map<String, String> totals = new LinkedHashMap<>();

    /** 签字栏，value 为已签的人 */
    private List<Item> signs = new ArrayList<>();

    /** 主明细之后的其他明细表（如生产任务单的工序） */
    private List<Section> sections = new ArrayList<>();

    @Data
    public static class Item {
        private String key;
        private String label;
        private String value;
        /** 表头字段占整行 */
        private boolean wide;
    }

    @Data
    public static class Column {
        private String key;
        private String title;
        /** 右对齐（数量、金额） */
        private boolean right;
        /** 列宽（mm），为空时按剩余宽度分配 */
        private Integer width;
    }

    @Data
    public static class Section {
        private String title;
        private List<Column> columns = new ArrayList<>();
        private List<Map<String, String>> rows = new ArrayList<>();
    }
}
