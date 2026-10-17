package com.lemon.erp.modules.system.print;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印请求：一次可打印多张同类单据（每张单独分页、单独编页码，工资条除外）
 */
@Data
public class PrintRequest {

    /** 打印模板标识（前端 PRINT_TYPES 的 key），租户上传了该单据的 JRXML 时按 JRXML 打印 */
    private String type;

    /** 打印任务标题（PDF 文件名、文档标题） */
    private String title;

    private PrintLayout layout = new PrintLayout();

    /** 套打设置，为空时按默认版式（上传了 JRXML 时按 JRXML） */
    private PrintOverlay overlay;

    private List<PrintDoc> docs = new ArrayList<>();
}
