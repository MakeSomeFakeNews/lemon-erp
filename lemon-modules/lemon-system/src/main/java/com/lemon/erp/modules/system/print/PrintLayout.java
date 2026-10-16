package com.lemon.erp.modules.system.print;

import lombok.Data;

/**
 * 默认版式的纸张与字号（打印模板中设置）；使用上传的 JRXML 时以 JRXML 为准
 */
@Data
public class PrintLayout {

    /** 纸张宽度（mm，已按纸张方向换算），默认 A4 纵向 */
    private int width = 210;

    private int height = 297;

    /** 页边距（mm） */
    private int margin = 10;

    /** 正文字号（pt） */
    private int fontSize = 10;
}
