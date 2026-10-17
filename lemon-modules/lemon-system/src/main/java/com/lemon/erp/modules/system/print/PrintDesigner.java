package com.lemon.erp.modules.system.print;

import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JRDesignDataset;
import net.sf.jasperreports.engine.design.JRDesignDatasetRun;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextElement;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JRDesignVariable;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.CalculationEnum;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.LineStyleEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.ResetTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 默认版式：按单据的结构（表头字段、明细列、签字栏）和纸张生成 Jasper 设计，下载的「默认 JRXML」也由此生成。
 * <ul>
 *   <li>页眉（每页）：公司名称、标题、单号、表头字段；列头（每页重复）；明细</li>
 *   <li>明细之后（组尾）：合计行、其他明细表（表格组件）、备注；末页页脚：签字栏、页码；其他页页脚：页码</li>
 *   <li>工资条：每条一个明细区（不跨页拆开），之间用虚线分隔</li>
 * </ul>
 * 表达式中只出现经过 {@link PrintService} 过滤的名称（{@link #literal}），单据中的值都通过参数和数据源传入。
 */
final class PrintDesigner {

    /** 参数：标题、单号、公司名称、备注（文本），表头字段、签字栏、合计（key → 文本），其他明细表（[{title, rows}]） */
    static final String P_TITLE = "TITLE";
    static final String P_NO = "NO";
    static final String P_COMPANY = "COMPANY_NAME";
    static final String P_NOTE = "NOTE";
    static final String P_FIELDS = "FIELDS";
    static final String P_SIGNS = "SIGNS";
    static final String P_TOTALS = "TOTALS";
    static final String P_SECTIONS = "SECTIONS";

    private static final float MM = 72f / 25.4f;
    private static final float BORDER = 0.5f;
    private static final int PADDING = 3;
    private static final int FIELDS_PER_ROW = 3;
    private static final int SLIP_FIELDS_PER_ROW = 4;
    private static final Color HEAD_BG = new Color(0xF2, 0xF2, 0xF2);

    /** 自动列宽时内容长度的上限（字符数），更长的换行显示 */
    private static final int MAX_WEIGHT = 30;

    /** 本页页码（页尾求值），与 PAGE_NUMBER（报表结束时求值即总页数）配合显示「第 x 页 / 共 y 页」 */
    private static final String V_PAGE = "本页页码";

    private final PrintLayout layout;
    private final int fontSize;
    private final int width;
    private int uuidSeq;

    private PrintDesigner(PrintLayout layout) {
        this.layout = layout;
        this.fontSize = layout.getFontSize();
        this.width = pt(layout.getWidth()) - 2 * pt(layout.getMargin());
    }

    /** 普通单据的设计；doc 只用于确定结构，不读取其中的值 */
    static JasperDesign design(String name, PrintDoc doc, PrintLayout layout) throws JRException {
        return new PrintDesigner(layout).build(name, doc);
    }

    /** 工资条：一页打印多条，fields 为所有条目的表头字段（按出现顺序合并） */
    static JasperDesign slipDesign(String name, List<PrintDoc.Item> fields, PrintLayout layout) throws JRException {
        return new PrintDesigner(layout).buildSlip(name, fields);
    }

    // ---------------------------------------------------------------- 普通单据

    private JasperDesign build(String name, PrintDoc doc) throws JRException {
        JasperDesign d = base(name);
        d.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        addParameter(d, P_TITLE, String.class);
        addParameter(d, P_NO, String.class);
        addParameter(d, P_COMPANY, String.class);
        addParameter(d, P_NOTE, String.class);
        addMapParameter(d, P_FIELDS);
        addMapParameter(d, P_SIGNS);
        addMapParameter(d, P_TOTALS);
        addParameter(d, P_SECTIONS, List.class);
        for (PrintDoc.Column c : doc.getColumns()) {
            addField(d, c.getKey());
        }

        d.setPageHeader(header(doc));
        List<PrintDoc.Column> columns = doc.getColumns();
        int[] widths = columnWidths(columns, doc.getRows());
        if (!columns.isEmpty()) {
            d.setColumnHeader(tableRow(columns, widths, (c, i) -> literal(c.getTitle()), true));
            ((JRDesignSection) d.getDetailSection()).addBand(tableRow(columns, widths, (c, i) -> "$F{" + c.getKey() + "}", false));
        }
        d.addGroup(ending(d, doc, widths));
        addPageVariable(d);
        d.setPageFooter(footer(List.of()));
        d.setLastPageFooter(footer(doc.getSigns()));
        return d;
    }

    private JRDesignBand header(PrintDoc doc) {
        JRDesignBand band = band(SplitTypeEnum.STRETCH);
        int y = 0;
        if (has(doc.getCompanyName())) {
            y = add(band, centered(text("$P{" + P_COMPANY + "}", 0, y, width, line(fontSize + 1)), fontSize + 1), y);
        }
        JRDesignTextField title = centered(text("$P{" + P_TITLE + "}", 0, y, width, line(fontSize + 8)), fontSize + 8);
        title.setBold(true);
        y = add(band, title, y);
        if (has(doc.getNo())) {
            y = add(band, centered(text("\"No. \" + $P{" + P_NO + "}", 0, y, width, line(fontSize)), fontSize), y);
        }
        y += 4;
        int cell = width / FIELDS_PER_ROW;
        int col = 0;
        for (PrintDoc.Item f : doc.getFields()) {
            if (f.isWide() && col > 0) {
                y += line(fontSize);
                col = 0;
            }
            int w = f.isWide() ? width : (col == FIELDS_PER_ROW - 1 ? width - cell * col : cell);
            JRDesignTextField t = text(labelled(f.getLabel(), P_FIELDS, f.getKey(), "-"), cell * col, y, w, line(fontSize));
            band.addElement(t);
            col = f.isWide() ? FIELDS_PER_ROW : col + 1;
            if (col == FIELDS_PER_ROW) {
                y += line(fontSize);
                col = 0;
            }
        }
        if (col > 0) {
            y += line(fontSize);
        }
        band.setHeight(y + 4);
        return band;
    }

    /**
     * 明细之后的内容：放在整张单据一个分组的组尾，合计行、每个明细表、备注各占一个区，
     * 放不下时只把放不下的区移到下一页（汇总区 summary 只能整体移动）
     */
    private JRDesignGroup ending(JasperDesign d, PrintDoc doc, int[] widths) throws JRException {
        JRDesignGroup group = new JRDesignGroup();
        group.setName("单据");
        group.setExpression(new JRDesignExpression("Boolean.TRUE"));
        JRDesignSection footer = (JRDesignSection) group.getGroupFooterSection();
        List<PrintDoc.Column> columns = doc.getColumns();
        if (!columns.isEmpty() && doc.getTotals() != null && !doc.getTotals().isEmpty()) {
            JRDesignBand band = band(SplitTypeEnum.PREVENT);
            int x = 0;
            int h = line(fontSize);
            for (int i = 0; i < columns.size(); i++) {
                PrintDoc.Column c = columns.get(i);
                String value = "$P{" + P_TOTALS + "}.get(" + literal(c.getKey()) + ")";
                // 第一列没有合计值时显示「合计」
                String expr = i == 0 ? "java.util.Objects.toString(" + value + ", \"合计\")" : value;
                JRDesignTextField t = cell(text(expr, x, 0, widths[i], h), c.isRight());
                t.setBold(true);
                band.addElement(t);
                x += widths[i];
            }
            band.setHeight(h);
            footer.addBand(band);
        }
        for (int s = 0; s < doc.getSections().size(); s++) {
            PrintDoc.Section sec = doc.getSections().get(s);
            if (sec.getColumns().isEmpty()) {
                continue;
            }
            JRDesignBand band = band(SplitTypeEnum.STRETCH);
            int y = 6;
            JRDesignStaticText title = staticText(sec.getTitle(), 0, y, width, line(fontSize));
            title.setBold(true);
            y = add(band, title, y);
            y = add(band, sectionTable(d, s, sec, y), y);
            band.setHeight(y);
            footer.addBand(band);
        }
        if (has(doc.getNote())) {
            JRDesignBand band = band(SplitTypeEnum.STRETCH);
            band.setHeight(add(band, text("\"备注：\" + $P{" + P_NOTE + "}", 0, 4, width, line(fontSize)), 4));
            footer.addBand(band);
        }
        return group;
    }

    /** 其他明细表：用表格组件（子数据集）实现，跨页时重复列头，下载的 JRXML 自包含 */
    private JRDesignComponentElement sectionTable(JasperDesign d, int index, PrintDoc.Section sec, int y) throws JRException {
        JRDesignDataset dataset = new JRDesignDataset(DefaultJasperReportsContext.getInstance(), false);
        dataset.setName("section" + index);
        for (PrintDoc.Column c : sec.getColumns()) {
            JRDesignField f = new JRDesignField();
            f.setName(c.getKey());
            f.setValueClass(String.class);
            dataset.addField(f);
        }
        d.addDataset(dataset);

        JRDesignDatasetRun run = new JRDesignDatasetRun();
        run.setDatasetName(dataset.getName());
        run.setDataSourceExpression(new JRDesignExpression("new net.sf.jasperreports.engine.data.JRMapCollectionDataSource("
                + "(java.util.Collection<java.util.Map<String, ?>>) ((java.util.Map<?, ?>) $P{" + P_SECTIONS + "}.get(" + index + ")).get(\"rows\"))"));

        StandardTable table = new StandardTable();
        table.setDatasetRun(run);
        int[] widths = columnWidths(sec.getColumns(), sec.getRows());
        int h = line(fontSize);
        for (int i = 0; i < sec.getColumns().size(); i++) {
            PrintDoc.Column c = sec.getColumns().get(i);
            StandardColumn column = new StandardColumn();
            column.setWidth(widths[i]);
            DesignCell head = new DesignCell();
            head.setHeight(h);
            head.addElement(headCell(text(literal(c.getTitle()), 0, 0, widths[i], h)));
            column.setColumnHeader(head);
            DesignCell detail = new DesignCell();
            detail.setHeight(h);
            detail.addElement(cell(text("$F{" + c.getKey() + "}", 0, 0, widths[i], h), c.isRight()));
            column.setDetailCell(detail);
            table.addColumn(column);
        }

        JRDesignComponentElement element = new JRDesignComponentElement(d);
        element.setUUID(nextUuid());
        element.setX(0);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(h * 2);
        element.setPositionType(PositionTypeEnum.FLOAT);
        element.setComponent(table);
        return element;
    }

    /** 页脚：签字栏（末页）+ 页码 */
    private JRDesignBand footer(List<PrintDoc.Item> signs) {
        JRDesignBand band = band(SplitTypeEnum.PREVENT);
        int y = 0;
        if (!signs.isEmpty()) {
            y += 8;
            int cell = width / signs.size();
            for (int i = 0; i < signs.size(); i++) {
                PrintDoc.Item s = signs.get(i);
                int w = i == signs.size() - 1 ? width - cell * i : cell;
                band.addElement(text(labelled(s.getLabel(), P_SIGNS, s.getKey(), ""), cell * i, y, w, line(fontSize)));
            }
            y += line(fontSize);
        }
        int h = line(fontSize - 1);
        // Auto：本页页码在页尾求值，总页数在报表结束时求值
        JRDesignTextField pages = centered(text("\"第 \" + $V{" + V_PAGE + "} + \" 页 / 共 \" + $V{PAGE_NUMBER} + \" 页\"",
                0, y, width, h), fontSize - 1);
        pages.setEvaluationTime(EvaluationTimeEnum.AUTO);
        band.addElement(pages);
        band.setHeight(y + h);
        return band;
    }

    // ---------------------------------------------------------------- 套打

    /** 套打：页边距为 0，定位字段放在背景区（每页都打印），明细按行高逐行输出，每页行数满后换页 */
    static JasperDesign overlayDesign(String name, PrintDoc doc, PrintLayout layout, PrintOverlay overlay) throws JRException {
        PrintLayout noMargin = new PrintLayout();
        noMargin.setWidth(layout.getWidth());
        noMargin.setHeight(layout.getHeight());
        noMargin.setMargin(0);
        noMargin.setFontSize(layout.getFontSize());
        return new PrintDesigner(noMargin).buildOverlay(name, doc, overlay);
    }

    private JasperDesign buildOverlay(String name, PrintDoc doc, PrintOverlay o) throws JRException {
        JasperDesign d = base(name);
        d.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        addParameter(d, P_TITLE, String.class);
        addParameter(d, P_NO, String.class);
        addParameter(d, P_COMPANY, String.class);
        addParameter(d, P_NOTE, String.class);
        addMapParameter(d, P_FIELDS);
        addMapParameter(d, P_SIGNS);
        addMapParameter(d, P_TOTALS);
        addParameter(d, P_SECTIONS, List.class);
        int pageHeight = d.getPageHeight();
        JRDesignBand background = new JRDesignBand();
        background.setHeight(pageHeight);
        for (PrintOverlay.Item it : o.getItems()) {
            int size = it.getFontSize() == null ? fontSize : it.getFontSize();
            JRDesignTextField t = fixed(overlayExpression(it.getKey()), ptd(it.getX() + o.getOffsetX()), ptd(it.getY() + o.getOffsetY()),
                    Math.max(ptd(it.getWidth()), 10), line(size), size, it.getAlign(), pageHeight);
            if (t != null) {
                background.addElement(t);
            }
        }
        d.setBackground(background);
        PrintOverlay.Detail detail = o.getDetail();
        Set<String> columnKeys = new java.util.HashSet<>();
        doc.getColumns().forEach(c -> columnKeys.add(c.getKey()));
        if (detail != null && !detail.getColumns().isEmpty() && detail.getRows() > 0) {
            int top = Math.max(0, ptd(detail.getY() + o.getOffsetY()));
            int rowHeight = Math.max(ptd(detail.getRowHeight()), line(fontSize - 2));
            int area = rowHeight * detail.getRows();
            if (top + area > pageHeight) {
                area = (pageHeight - top) / rowHeight * rowHeight;
            }
            JRDesignBand header = new JRDesignBand();
            header.setHeight(top);
            d.setPageHeader(header);
            JRDesignBand row = band(SplitTypeEnum.PREVENT);
            row.setHeight(rowHeight);
            for (PrintOverlay.Column c : detail.getColumns()) {
                String key = PrintService.safeName(c.getKey());
                if (!columnKeys.contains(key)) {
                    continue;
                }
                addField(d, key);
                JRDesignTextField t = fixed("$F{" + key + "}", ptd(c.getX() + o.getOffsetX()), 0, Math.max(ptd(c.getWidth()), 10), rowHeight,
                        fontSize, c.getAlign(), rowHeight);
                if (t != null) {
                    row.addElement(t);
                }
            }
            ((JRDesignSection) d.getDetailSection()).addBand(row);
            JRDesignBand footer = new JRDesignBand();
            footer.setHeight(Math.max(0, pageHeight - top - area));
            d.setPageFooter(footer);
        }
        return d;
    }

    /** 定位字段的表达式；不认识的特殊标识返回 null */
    private static String overlayExpression(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case "$title" -> "$P{" + P_TITLE + "}";
            case "$no" -> "$P{" + P_NO + "}";
            case "$company" -> "$P{" + P_COMPANY + "}";
            case "$note" -> "$P{" + P_NOTE + "}";
            default -> {
                if (key.startsWith("$total:")) {
                    yield "$P{" + P_TOTALS + "}.get(" + literal(key.substring(7)) + ")";
                }
                if (key.startsWith("$sign:")) {
                    yield "$P{" + P_SIGNS + "}.get(" + literal(key.substring(6)) + ")";
                }
                yield key.startsWith("$") ? null : "$P{" + P_FIELDS + "}.get(" + literal(key) + ")";
            }
        };
    }

    /** 固定位置、不拉伸的文字（超出宽度截断）；超出带区的不输出 */
    private JRDesignTextField fixed(String expression, int x, int y, int w, int h, int size, PrintOverlay.Align align, int bandHeight) {
        if (expression == null || x < 0 || y < 0 || y + h > bandHeight) {
            return null;
        }
        JRDesignTextField t = text(expression, x, y, w, h);
        t.setTextAdjust(TextAdjustEnum.CUT_TEXT);
        t.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_TOP);
        t.setFontSize((float) size);
        t.setHorizontalTextAlign(align == PrintOverlay.Align.RIGHT ? HorizontalTextAlignEnum.RIGHT
                : align == PrintOverlay.Align.CENTER ? HorizontalTextAlignEnum.CENTER : HorizontalTextAlignEnum.LEFT);
        return t;
    }

    /** mm（可有小数）→ pt */
    private static int ptd(double mm) {
        return (int) Math.round(mm * MM);
    }

    // ---------------------------------------------------------------- 工资条

    private JasperDesign buildSlip(String name, List<PrintDoc.Item> fields) throws JRException {
        JasperDesign d = base(name);
        addParameter(d, P_TITLE, String.class);
        for (PrintDoc.Item f : fields) {
            addField(d, f.getKey());
        }
        JRDesignBand band = band(SplitTypeEnum.PREVENT);
        int y = 0;
        JRDesignTextField title = centered(text("$P{" + P_TITLE + "}", 0, y, width, line(fontSize + 3)), fontSize + 3);
        title.setBold(true);
        y = add(band, title, y) + 2;
        int cell = width / SLIP_FIELDS_PER_ROW;
        int col = 0;
        int h = line(fontSize);
        for (PrintDoc.Item f : fields) {
            if (f.isWide() && col > 0) {
                fillRow(band, cell, col, y, h);
                y += h;
                col = 0;
            }
            int w = f.isWide() || col == SLIP_FIELDS_PER_ROW - 1 ? width - cell * col : cell;
            String value = "$F{" + f.getKey() + "}";
            JRDesignTextField t = cell(text(literal(f.getLabel() + "：") + " + java.util.Objects.toString(" + value + ", \"\")",
                    cell * col, y, w, h), false);
            if (f.isWide()) {
                // 整行的字段（如备注）没有值时不占行
                t.setPrintWhenExpression(new JRDesignExpression(value + " != null && !" + value + ".isEmpty()"));
                t.setRemoveLineWhenBlank(true);
            }
            band.addElement(t);
            col = f.isWide() ? SLIP_FIELDS_PER_ROW : col + 1;
            if (col == SLIP_FIELDS_PER_ROW) {
                y += h;
                col = 0;
            }
        }
        if (col > 0) {
            fillRow(band, cell, col, y, h);
            y += h;
        }
        y += 8;
        JRDesignLine cut = new JRDesignLine();
        cut.setUUID(nextUuid());
        cut.setX(0);
        cut.setY(y);
        cut.setWidth(width);
        cut.setHeight(1);
        cut.setPositionType(PositionTypeEnum.FLOAT);
        cut.getLinePen().setLineWidth(BORDER);
        cut.getLinePen().setLineStyle(LineStyleEnum.DASHED);
        cut.getLinePen().setLineColor(Color.GRAY);
        band.addElement(cut);
        band.setHeight(y + 10);
        ((JRDesignSection) d.getDetailSection()).addBand(band);
        return d;
    }

    /** 一行没排满时补上空单元格，表格保持整齐 */
    private void fillRow(JRDesignBand band, int cell, int col, int y, int h) {
        band.addElement(cell(staticText("", cell * col, y, width - cell * col, h), false));
    }

    // ---------------------------------------------------------------- 元素

    private JasperDesign base(String name) throws JRException {
        JasperDesign d = new JasperDesign();
        d.setName(name);
        d.setUUID(nextUuid());
        int margin = pt(layout.getMargin());
        d.setPageWidth(pt(layout.getWidth()));
        d.setPageHeight(pt(layout.getHeight()));
        d.setLeftMargin(margin);
        d.setRightMargin(margin);
        d.setTopMargin(margin);
        d.setBottomMargin(margin);
        d.setColumnWidth(width);
        JRDesignStyle style = new JRDesignStyle();
        style.setName("正文");
        style.setDefault(true);
        style.setFontName(PrintContext.DEFAULT_FONT);
        style.setFontSize((float) fontSize);
        style.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        d.addStyle(style);
        return d;
    }

    /** 明细表的一行（列头或明细）：带边框的单元格，同一行等高 */
    private JRDesignBand tableRow(List<PrintDoc.Column> columns, int[] widths, CellExpression expr, boolean head) {
        JRDesignBand band = band(SplitTypeEnum.PREVENT);
        int h = line(fontSize);
        int x = 0;
        for (int i = 0; i < columns.size(); i++) {
            PrintDoc.Column c = columns.get(i);
            JRDesignTextField t = text(expr.of(c, i), x, 0, widths[i], h);
            band.addElement(head ? headCell(t) : cell(t, c.isRight()));
            x += widths[i];
        }
        band.setHeight(h);
        return band;
    }

    private interface CellExpression {
        String of(PrintDoc.Column column, int index);
    }

    /**
     * 列宽（pt）：指定了宽度的列按 mm 换算；其余按内容长度（列名和各行文字中最长的，中文算 2 个字符，最多 30）分配剩余宽度，
     * 不窄于列名；固定列宽合计超出纸张时按比例缩小
     */
    int[] columnWidths(List<PrintDoc.Column> columns, List<Map<String, String>> rows) {
        int n = columns.size();
        int[] widths = new int[n];
        if (n == 0) {
            return widths;
        }
        float charWidth = fontSize / 2f;
        float fixed = 0;
        float weights = 0;
        float autoMin = 0;
        float[] w = new float[n];
        float[] min = new float[n];
        boolean[] auto = new boolean[n];
        for (int i = 0; i < n; i++) {
            PrintDoc.Column c = columns.get(i);
            min[i] = Math.min(displayLength(c.getTitle()), MAX_WEIGHT) * charWidth + 2 * PADDING + 2;
            if (c.getWidth() != null && c.getWidth() > 0) {
                w[i] = pt(c.getWidth());
                fixed += w[i];
            } else {
                auto[i] = true;
                int len = displayLength(c.getTitle());
                for (Map<String, String> r : rows) {
                    len = Math.max(len, displayLength(r.get(c.getKey())));
                }
                // 取偶数，减少不同单据之间设计的差异（同样的设计只编译一次）
                w[i] = Math.min(MAX_WEIGHT, (len + 1) / 2 * 2) + 2;
                weights += w[i];
                autoMin += min[i];
            }
        }
        float scale = fixed > 0 && fixed > width - autoMin ? Math.max(0, width - autoMin) / fixed : 1f;
        float rest = width - fixed * scale;
        // 自动列先按权重分，比列名窄的补足到列名宽度，从其他列中扣除
        float extra = 0;
        float flexible = 0;
        for (int i = 0; i < n; i++) {
            if (auto[i]) {
                float v = weights > 0 ? rest * w[i] / weights : 0;
                if (v < min[i]) {
                    extra += min[i] - v;
                } else {
                    flexible += v - min[i];
                }
            }
        }
        int used = 0;
        for (int i = 0; i < n; i++) {
            float v;
            if (auto[i]) {
                v = rest * w[i] / weights;
                v = v < min[i] ? min[i] : v - (flexible > 0 ? (v - min[i]) * Math.min(1f, extra / flexible) : 0);
            } else {
                v = w[i] * scale;
            }
            widths[i] = Math.max(8, Math.round(v));
            used += widths[i];
        }
        // 舍入误差补到最后一列，保证表格与正文同宽
        widths[n - 1] = Math.max(8, widths[n - 1] + width - used);
        return widths;
    }

    /** 显示长度：中文等全角字符算 2 */
    private static int displayLength(String s) {
        if (s == null) {
            return 0;
        }
        int len = 0;
        for (int i = 0; i < s.length() && len < MAX_WEIGHT * 2; i++) {
            len += s.charAt(i) < 0x2E80 ? 1 : 2;
        }
        return len;
    }

    private JRDesignBand band(SplitTypeEnum split) {
        JRDesignBand band = new JRDesignBand();
        band.setSplitType(split);
        return band;
    }

    private JRDesignTextField text(String expression, int x, int y, int w, int h) {
        JRDesignTextField t = new JRDesignTextField();
        t.setUUID(nextUuid());
        t.setX(x);
        t.setY(y);
        t.setWidth(w);
        t.setHeight(h);
        t.setExpression(new JRDesignExpression(expression));
        t.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
        t.setPositionType(PositionTypeEnum.FLOAT);
        t.setBlankWhenNull(true);
        return t;
    }

    private JRDesignStaticText staticText(String value, int x, int y, int w, int h) {
        JRDesignStaticText t = new JRDesignStaticText();
        t.setUUID(nextUuid());
        t.setX(x);
        t.setY(y);
        t.setWidth(w);
        t.setHeight(h);
        t.setText(value);
        t.setPositionType(PositionTypeEnum.FLOAT);
        return t;
    }

    private static JRDesignTextField centered(JRDesignTextField t, int size) {
        t.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        t.setFontSize((float) size);
        return t;
    }

    /** 表格单元格：边框、内边距，同一行中按最高的单元格拉伸 */
    private static <T extends JRDesignTextElement> T cell(T t, boolean right) {
        JRLineBox box = t.getLineBox();
        box.getPen().setLineWidth(BORDER);
        box.setLeftPadding(PADDING);
        box.setRightPadding(PADDING);
        box.setTopPadding(1);
        box.setBottomPadding(1);
        t.setStretchType(StretchTypeEnum.CONTAINER_HEIGHT);
        if (right) {
            t.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        }
        return t;
    }

    private static <T extends JRDesignTextElement> T headCell(T t) {
        cell(t, false);
        t.setBold(true);
        t.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        t.setMode(ModeEnum.OPAQUE);
        t.setBackcolor(HEAD_BG);
        return t;
    }

    /** 加入元素，返回下一行的 y */
    private static int add(JRDesignBand band, JRDesignElement element, int y) {
        band.addElement(element);
        return y + element.getHeight();
    }

    /** 「名称：值」，值为空时显示 empty */
    private static String labelled(String label, String param, String key, String empty) {
        return literal(label + "：") + " + java.util.Objects.toString($P{" + param + "}.get(" + literal(key) + "), " + literal(empty) + ")";
    }

    /** 一行文字的高度（pt） */
    private static int line(int size) {
        return Math.round(size * 1.4f) + 4;
    }

    private static int pt(int mm) {
        return Math.round(mm * MM);
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static void addParameter(JasperDesign d, String name, Class<?> type) throws JRException {
        JRDesignParameter p = new JRDesignParameter();
        p.setName(name);
        p.setValueClass(type);
        d.addParameter(p);
    }

    private static void addMapParameter(JasperDesign d, String name) throws JRException {
        JRDesignParameter p = new JRDesignParameter();
        p.setName(name);
        p.setValueClass(Map.class);
        p.setNestedType(String.class);
        d.addParameter(p);
    }

    private static void addPageVariable(JasperDesign d) throws JRException {
        JRDesignVariable v = new JRDesignVariable();
        v.setName(V_PAGE);
        v.setValueClass(Integer.class);
        v.setResetType(ResetTypeEnum.PAGE);
        v.setCalculation(CalculationEnum.NOTHING);
        v.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
        d.addVariable(v);
    }

    private static void addField(JasperDesign d, String name) throws JRException {
        JRDesignField f = new JRDesignField();
        f.setName(name);
        f.setValueClass(String.class);
        d.addField(f);
    }

    /** 固定的 UUID：同样结构的设计生成的 JRXML 相同 */
    private UUID nextUuid() {
        return UUID.nameUUIDFromBytes(("lemon-print-" + uuidSeq++).getBytes());
    }

    /**
     * Java 字符串字面量。名称已由 {@link PrintService} 过滤（只保留文字、数字和少量标点），这里再去掉引号、反斜杠等兜底，
     * 保证表达式中不会出现可执行的代码
     */
    static String literal(String s) {
        return "\"" + PrintService.safeName(s) + "\"";
    }
}
