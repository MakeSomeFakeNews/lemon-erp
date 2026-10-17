package com.lemon.erp.modules.system.print;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 套打：在预印的单据纸上只打印数据，各字段按绝对位置（mm，以纸张左上角为原点）输出，不打印表格线和标签。
 * offsetX / offsetY 用于校准打印机的进纸偏差
 */
@Data
public class PrintOverlay {

    /** 对齐方式 */
    public enum Align {
        LEFT, CENTER, RIGHT
    }

    private double offsetX;

    private double offsetY;

    /** 定位的字段：key 为表头字段名，或 $title 标题、$no 单号、$company 公司名称、$note 备注、$total:列名 合计、$sign:名称 签字 */
    private List<Item> items = new ArrayList<>();

    /** 明细区：为空时不打印明细 */
    private Detail detail;

    @Data
    public static class Item {
        private String key;
        private double x;
        private double y;
        /** 宽度（mm），文字超出时截断 */
        private double width = 40;
        /** 字号（pt），为空用模板字号 */
        private Integer fontSize;
        private Align align;
    }

    @Data
    public static class Detail {
        /** 第一行明细的顶部位置（mm） */
        private double y;
        /** 行高（mm） */
        private double rowHeight = 8;
        /** 每页行数，超出时换页（每页重复打印定位字段） */
        private int rows = 10;
        private List<Column> columns = new ArrayList<>();
    }

    @Data
    public static class Column {
        /** 明细列名 */
        private String key;
        private double x;
        private double width = 20;
        private Align align;
    }
}
