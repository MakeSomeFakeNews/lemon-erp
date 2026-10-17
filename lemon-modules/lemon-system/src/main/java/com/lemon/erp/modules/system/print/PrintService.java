package com.lemon.erp.modules.system.print;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantProperties;
import com.lemon.erp.modules.system.service.SysPrintTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 单据打印：用 JasperReports 把前端组装的单据生成 PDF。
 * <p>
 * 租户上传了该单据的 JRXML 时按 JRXML 打印，否则按 {@link PrintDesigner} 的默认版式（打印模板中的纸张、字段、列配置）。
 * 编译后的报表按内容缓存在本机内存中（JasperReport 不可序列化，不放 CacheService）。
 * <p>
 * JRXML 中的表达式是 Java 代码、在服务器上执行，因此 SaaS 模式下只有平台租户能上传（{@link #checkUploadAllowed}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintService {

    private static final int MAX_DOCS = 1000;
    private static final int MAX_ROWS = 20_000;
    private static final int MAX_ITEMS = 60;
    private static final int MAX_NAME = 40;
    private static final int MAX_VALUE = 2000;
    private static final int MAX_JRXML = 1024 * 1024;
    private static final int CACHE_SIZE = 256;

    /** 名称中允许的字符：文字、数字、空格和少量标点（不含引号、反斜杠、花括号等），见 {@link PrintDesigner#literal} */
    private static final Pattern UNSAFE = Pattern.compile("[^\\p{L}\\p{N} _\\-/()（）.·:：%#×+、]");

    private static final JasperReportsContext CONTEXT = PrintContext.createContext();

    private final SysPrintTemplateService templateService;
    private final TenantProperties tenantProperties;

    /** 编译后的报表：设计内容摘要 → 报表 */
    private final Map<String, JasperReport> reports = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, JasperReport> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /** 生成 PDF：多张单据合并为一个文件，每张单独分页、编页码（工资条连续打印） */
    public byte[] pdf(PrintRequest request) {
        List<PrintDoc> docs = normalize(request);
        String jrxml = templateService.jrxml(request.getType());
        List<JasperPrint> prints = new ArrayList<>();
        try {
            if (docs.get(0).isSlip()) {
                JasperReport report = jrxml != null ? custom(jrxml) : compile(slipDesign(request, docs));
                Map<String, Object> params = baseParams();
                params.put(PrintDesigner.P_TITLE, docs.get(0).getTitle());
                List<Map<String, ?>> rows = new ArrayList<>();
                docs.forEach(d -> rows.add(values(d.getFields())));
                prints.add(fill(report, params, new JRMapCollectionDataSource(rows)));
            } else {
                PrintOverlay overlay = jrxml == null ? checkOverlay(request.getOverlay()) : null;
                for (PrintDoc doc : docs) {
                    JasperReport report = jrxml != null ? custom(jrxml)
                            : overlay != null ? compile(PrintDesigner.overlayDesign(reportName(request.getType()), doc, request.getLayout(), overlay))
                            : compile(design(request, doc));
                    prints.add(fill(report, params(doc), dataSource(doc)));
                }
            }
            return export(prints, request.getTitle());
        } catch (JRException e) {
            log.warn("打印失败：{}", e.getMessage(), e);
            throw new BizException("system.error.printingFailed", e.getMessage());
        }
    }

    /** 当前配置下的默认版式 JRXML（用第一张单据的结构生成），用 Jaspersoft Studio 修改后可上传为自定义模板 */
    public String defaultJrxml(PrintRequest request) {
        List<PrintDoc> docs = normalize(request);
        try {
            JasperDesign design = docs.get(0).isSlip() ? slipDesign(request, docs) : design(request, docs.get(0));
            return JRXmlWriter.writeReport(design, StandardCharsets.UTF_8.name());
        } catch (JRException e) {
            throw new BizException("system.error.failedGenerateTemplate", e.getMessage());
        }
    }

    /** 上传前校验 JRXML：能编译通过 */
    public void validateJrxml(String jrxml) {
        checkUploadAllowed();
        if (jrxml == null || jrxml.isBlank()) {
            throw new BizException("system.error.templateFileEmpty");
        }
        if (jrxml.length() > MAX_JRXML) {
            throw new BizException("system.text.templateFileCannotExceedMb");
        }
        try {
            custom(jrxml);
        } catch (JRException | RuntimeException e) {
            throw new BizException("system.error.templateCannotCompiledMustJaspersoftStudio", rootMessage(e));
        }
    }

    /** 上传 JRXML：校验文件大小、能编译通过后保存，之后该单据按 JRXML 打印 */
    public void uploadJrxml(String key, MultipartFile file) throws IOException {
        checkUploadAllowed();
        if (file.getSize() > MAX_JRXML) {
            throw new BizException("system.text.templateFileCannotExceedMb");
        }
        String jrxml = new String(file.getBytes(), StandardCharsets.UTF_8);
        validateJrxml(jrxml);
        templateService.saveJrxml(key, jrxml);
    }

    /** SaaS 模式下只有平台租户能上传 JRXML：其中的 Java 表达式在服务器上执行 */
    public void checkUploadAllowed() {
        if (tenantProperties.isEnabled() && !TenantContext.isPlatform()) {
            throw new BizException("system.error.saasModeTenantsCannotUploadJrxml");
        }
    }

    // ---------------------------------------------------------------- 编译、填充、导出

    private JasperDesign design(PrintRequest request, PrintDoc doc) throws JRException {
        return PrintDesigner.design(reportName(request.getType()), doc, request.getLayout());
    }

    private JasperDesign slipDesign(PrintRequest request, List<PrintDoc> docs) throws JRException {
        Map<String, PrintDoc.Item> fields = new LinkedHashMap<>();
        docs.forEach(d -> d.getFields().forEach(f -> fields.putIfAbsent(f.getKey(), f)));
        return PrintDesigner.slipDesign(reportName(request.getType()), new ArrayList<>(fields.values()), request.getLayout());
    }

    /** 默认版式按生成的 JRXML 缓存：同样结构、纸张的单据只编译一次 */
    private JasperReport compile(JasperDesign design) throws JRException {
        String xml = JRXmlWriter.writeReport(design, StandardCharsets.UTF_8.name());
        return cached("default:" + digest(xml), () -> JasperCompileManager.getInstance(CONTEXT).compile(design));
    }

    private JasperReport custom(String jrxml) throws JRException {
        return cached("custom:" + digest(jrxml), () -> JasperCompileManager.getInstance(CONTEXT)
                .compile(new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8))));
    }

    private interface Compiler {
        JasperReport compile() throws JRException;
    }

    private JasperReport cached(String key, Compiler compiler) throws JRException {
        synchronized (reports) {
            JasperReport report = reports.get(key);
            if (report != null) {
                return report;
            }
        }
        JasperReport report = compiler.compile();
        synchronized (reports) {
            reports.put(key, report);
        }
        return report;
    }

    private static JasperPrint fill(JasperReport report, Map<String, Object> params, JRDataSource ds) throws JRException {
        return JasperFillManager.getInstance(CONTEXT).fill(report, params, ds);
    }

    private static byte[] export(List<JasperPrint> prints, String title) throws JRException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter(CONTEXT);
        exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataTitle(title);
        config.setDisplayMetadataTitle(true);
        config.setMetadataCreator(Messages.get("framework.label.defaultSystemName"));
        exporter.setConfiguration(config);
        exporter.exportReport();
        return out.toByteArray();
    }

    private static Map<String, Object> baseParams() {
        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, Locale.SIMPLIFIED_CHINESE);
        return params;
    }

    private static Map<String, Object> params(PrintDoc doc) {
        Map<String, Object> params = baseParams();
        params.put(PrintDesigner.P_TITLE, doc.getTitle());
        params.put(PrintDesigner.P_NO, doc.getNo());
        params.put(PrintDesigner.P_COMPANY, doc.getCompanyName());
        params.put(PrintDesigner.P_NOTE, doc.getNote());
        params.put(PrintDesigner.P_FIELDS, values(doc.getFields()));
        params.put(PrintDesigner.P_SIGNS, values(doc.getSigns()));
        params.put(PrintDesigner.P_TOTALS, doc.getTotals());
        List<Map<String, Object>> sections = new ArrayList<>();
        for (PrintDoc.Section s : doc.getSections()) {
            Map<String, Object> section = new HashMap<>();
            section.put("title", s.getTitle());
            section.put("rows", new ArrayList<>(s.getRows()));
            sections.add(section);
        }
        params.put(PrintDesigner.P_SECTIONS, sections);
        return params;
    }

    private static JRDataSource dataSource(PrintDoc doc) {
        if (doc.getRows().isEmpty()) {
            return new JREmptyDataSource(0);
        }
        Collection<Map<String, ?>> rows = new ArrayList<>(doc.getRows());
        return new JRMapCollectionDataSource(rows);
    }

    private static Map<String, String> values(List<PrintDoc.Item> items) {
        Map<String, String> map = new LinkedHashMap<>();
        items.forEach(i -> map.put(i.getKey(), i.getValue()));
        return map;
    }

    // ---------------------------------------------------------------- 校验、规范化

    /**
     * 校验数量与纸张，并把名称规范化：key、显示名称只保留安全字符（会出现在默认版式的表达式中），
     * 明细行、合计中的 key 随之替换
     */
    private static List<PrintDoc> normalize(PrintRequest request) {
        List<PrintDoc> docs = request.getDocs();
        if (docs == null || docs.isEmpty()) {
            throw new BizException("system.error.noDocumentsPrint");
        }
        if (docs.size() > MAX_DOCS) {
            throw new BizException("system.error.upDocumentsPrintedTime", MAX_DOCS);
        }
        int rows = docs.stream().mapToInt(d -> d.getRows() == null ? 0 : d.getRows().size()).sum();
        if (rows > MAX_ROWS) {
            throw new BizException("system.error.upDetailRowsPrintedTime", MAX_ROWS);
        }
        checkLayout(request.getLayout());
        request.setTitle(text(request.getTitle() == null ? Messages.get("system.text.print") : request.getTitle()));
        docs.forEach(PrintService::normalize);
        return docs;
    }

    /** 套打设置：位置在纸张范围内，最多 80 个定位字段、30 个明细列，字段名只保留安全字符 */
    private static PrintOverlay checkOverlay(PrintOverlay o) {
        if (o == null) {
            return null;
        }
        if (o.getItems() == null) {
            o.setItems(List.of());
        }
        if (o.getItems().size() > 80) {
            throw new BizException("system.error.tooManyPositionedFieldsPrePrinted");
        }
        if (Math.abs(o.getOffsetX()) > 50 || Math.abs(o.getOffsetY()) > 50) {
            throw new BizException("system.error.prePrintedFormOffsetCannotExceed");
        }
        for (PrintOverlay.Item it : o.getItems()) {
            if (it.getFontSize() != null && (it.getFontSize() < 6 || it.getFontSize() > 36)) {
                it.setFontSize(null);
            }
            it.setWidth(Math.min(Math.max(it.getWidth(), 5), 500));
        }
        PrintOverlay.Detail d = o.getDetail();
        if (d != null) {
            if (d.getColumns() == null) {
                d.setColumns(List.of());
            }
            if (d.getColumns().size() > 30 || d.getRows() > 100 || d.getRowHeight() < 3 || d.getRowHeight() > 50) {
                throw new BizException("system.error.invalidDetailAreaSettingsPrePrinted");
            }
            d.getColumns().forEach(c -> c.setWidth(Math.min(Math.max(c.getWidth(), 5), 500)));
        }
        return o;
    }

    private static void checkLayout(PrintLayout l) {
        if (l == null) {
            throw new BizException("system.error.invalidPaperSettings");
        }
        if (l.getWidth() < 50 || l.getWidth() > 1000 || l.getHeight() < 50 || l.getHeight() > 2000) {
            throw new BizException("system.error.paperSizeMustBetweenMmMm");
        }
        if (l.getMargin() < 0 || l.getMargin() > 40 || l.getMargin() * 2 >= Math.min(l.getWidth(), l.getHeight()) - 20) {
            throw new BizException("system.error.invalidMargin");
        }
        if (l.getFontSize() < 6 || l.getFontSize() > 24) {
            throw new BizException("system.error.fontSizeMustBetween");
        }
    }

    private static void normalize(PrintDoc doc) {
        doc.setTitle(text(doc.getTitle()));
        doc.setNo(text(doc.getNo()));
        doc.setCompanyName(text(doc.getCompanyName()));
        doc.setNote(text(doc.getNote()));
        // 名称为空时的匹配键（如「字段1」），与模板中的名称一致，保持中文
        doc.setFields(items(doc.getFields(), "字段")); // i18n-ignore
        doc.setSigns(items(doc.getSigns(), "签字")); // i18n-ignore
        Map<String, String> keys = columns(doc.getColumns(), "列"); // i18n-ignore
        doc.setRows(rows(doc.getRows(), keys));
        doc.setTotals(doc.getTotals() == null ? Map.of() : rows(List.of(doc.getTotals()), keys).get(0));
        if (doc.getSections() == null) {
            doc.setSections(List.of());
        }
        if (doc.getSections().size() > 5) {
            throw new BizException("system.error.tooManyDetailTables");
        }
        for (PrintDoc.Section s : doc.getSections()) {
            s.setTitle(safeName(s.getTitle()));
            Map<String, String> sectionKeys = columns(s.getColumns(), "列"); // i18n-ignore
            s.setRows(rows(s.getRows(), sectionKeys));
        }
    }

    private static List<PrintDoc.Item> items(List<PrintDoc.Item> items, String fallback) {
        if (items == null) {
            return List.of();
        }
        if (items.size() > MAX_ITEMS) {
            throw new BizException("system.error.tooManyFields");
        }
        Set<String> used = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            PrintDoc.Item it = items.get(i);
            String key = unique(it.getKey() != null ? it.getKey() : it.getLabel(), fallback + (i + 1), used);
            it.setKey(key);
            it.setLabel(it.getLabel() == null ? key : safeName(it.getLabel()));
            it.setValue(text(it.getValue()));
        }
        return items;
    }

    /** 明细列：规范化 key，返回 原 key → 新 key */
    private static Map<String, String> columns(List<PrintDoc.Column> columns, String fallback) {
        if (columns == null) {
            throw new BizException("system.error.invalidDetailColumns");
        }
        if (columns.size() > MAX_ITEMS) {
            throw new BizException("system.error.tooManyDetailColumns");
        }
        Map<String, String> keys = new HashMap<>();
        Set<String> used = new HashSet<>();
        for (int i = 0; i < columns.size(); i++) {
            PrintDoc.Column c = columns.get(i);
            String original = c.getKey() != null ? c.getKey() : c.getTitle();
            String key = unique(original, fallback + (i + 1), used);
            keys.put(original, key);
            c.setKey(key);
            c.setTitle(c.getTitle() == null ? key : safeName(c.getTitle()));
            if (c.getWidth() != null && (c.getWidth() < 1 || c.getWidth() > 500)) {
                c.setWidth(null);
            }
        }
        return keys;
    }

    private static List<Map<String, String>> rows(List<Map<String, String>> rows, Map<String, String> keys) {
        if (rows == null) {
            return List.of();
        }
        Function<Map<String, String>, Map<String, String>> rename = row -> {
            Map<String, String> r = new HashMap<>();
            row.forEach((k, v) -> {
                String key = keys.get(k);
                if (key != null) {
                    r.put(key, text(v));
                }
            });
            return r;
        };
        return rows.stream().map(r -> r == null ? Map.<String, String>of() : rename.apply(r)).toList();
    }

    /**
     * 规范化后的 key 不能重复；Jasper 把字段名转为 Java 标识符（非字母数字都变为 _），转换后也不能重复
     */
    private static String unique(String name, String fallback, Set<String> used) {
        String key = safeName(name);
        if (key.isEmpty()) {
            key = fallback;
        }
        String base = key;
        for (int n = 2; !used.add(identifier(key)); n++) {
            key = base + n;
        }
        return key;
    }

    private static String identifier(String name) {
        return name.replaceAll("[^\\p{L}\\p{N}]", "_");
    }

    /** 名称（字段、列、签字栏）：只保留安全字符，最长 40 个字 */
    static String safeName(String s) {
        if (s == null) {
            return "";
        }
        String name = UNSAFE.matcher(s).replaceAll("").trim();
        return name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
    }

    /** 值：只限制长度（值通过参数、数据源传入，不进入表达式） */
    private static String text(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_VALUE ? s.substring(0, MAX_VALUE) : s;
    }

    private static String reportName(String type) {
        String name = type == null ? "" : type.replaceAll("[^A-Za-z0-9]", "_");
        return "lemon_" + (name.isEmpty() ? "doc" : name);
    }

    private static String digest(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        return msg.length() > 300 ? msg.substring(0, 300) + "…" : msg;
    }
}
