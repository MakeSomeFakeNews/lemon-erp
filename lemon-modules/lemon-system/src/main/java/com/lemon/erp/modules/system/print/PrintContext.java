package com.lemon.erp.modules.system.print;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.SimpleJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontFace;
import net.sf.jasperreports.engine.fonts.FontFamily;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 打印用的 Jasper 环境（字体、编译器）。
 * <p>
 * 字体：PDF 中使用阅读器内置的中文字体（宋体 STSong-Light，不嵌入字体文件，PDF 体积小、服务器无需安装字库），
 * 排版时用 JVM 中能显示中文的字体测量文字宽度来换行。
 * <p>
 * 注册为常用的中文字体名和 Java 逻辑字体名，默认版式和用 Jaspersoft Studio 设计的模板（选了宋体、黑体、微软雅黑等）都能正常显示中文。
 * 服务器没有任何中文字体时按 JVM 默认字体测量，中文宽度可能偏窄，建议安装中文字体（如 fonts-noto-cjk）。
 */
@Slf4j
final class PrintContext {

    /** 默认版式使用的字体名 */
    static final String DEFAULT_FONT = "宋体";

    private static final String PDF_FONT = "STSong-Light";
    private static final String PDF_ENCODING = "UniGB-UCS2-H";

    private static final List<String> FAMILY_NAMES = List.of(DEFAULT_FONT, "SimSun", "新宋体", "NSimSun", "黑体", "SimHei",
            "微软雅黑", "Microsoft YaHei", "仿宋", "FangSong", "楷体", "KaiTi", "PingFang SC", "Noto Sans SC", "Noto Serif SC",
            "Source Han Sans SC", "思源黑体", "思源宋体", "SansSerif", "Serif", "Dialog", "DejaVu Sans", "DejaVu Serif", "Arial");

    private PrintContext() {
    }

    static JasperReportsContext createContext() {
        Font measure = measureFont();
        FontFace normal = face(measure, PDF_FONT);
        FontFace bold = face(measure.deriveFont(Font.BOLD), PDF_FONT + ",Bold");
        SimpleJasperReportsContext context = new SimpleJasperReportsContext(DefaultJasperReportsContext.getInstance());
        context.setExtensions(FontFamily.class, FAMILY_NAMES.stream().map(name -> family(name, normal, bold)).toList());
        // 用 JDT 编译表达式：javac 在 Spring Boot 可执行 jar 中找不到 Jasper 等依赖的类
        context.setProperty("net.sf.jasperreports.compiler.java", "net.sf.jasperreports.jdt.JRJdtCompiler");
        context.setProperty("net.sf.jasperreports.default.font.name", DEFAULT_FONT);
        context.setProperty("net.sf.jasperreports.default.pdf.font.name", PDF_FONT);
        context.setProperty("net.sf.jasperreports.default.pdf.encoding", PDF_ENCODING);
        return context;
    }

    /** 测量用的字体：优先 Java 逻辑字体（会自动回退到系统中文字体），否则找一个能显示中文的系统字体 */
    private static Font measureFont() {
        Font logical = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        if (logical.canDisplay('中')) {
            return logical;
        }
        Font found = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
                .filter(f -> f.canDisplay('中'))
                .findFirst()
                .orElse(null);
        if (found == null) {
            log.warn("服务器没有能显示中文的字体，打印单据时中文换行可能不准确，建议安装中文字体（如 fonts-noto-cjk）");
            return logical;
        }
        return found.deriveFont(Font.PLAIN, 1f);
    }

    private static FontFace face(Font font, String pdf) {
        return new FontFace() {
            @Override
            public String getName() {
                return font.getFontName();
            }

            @Override
            public String getTtf() {
                return null;
            }

            @Override
            public Font getFont() {
                return font;
            }

            @Override
            public String getPdf() {
                return pdf;
            }

            @Override
            public String getEot() {
                return null;
            }

            @Override
            public String getSvg() {
                return null;
            }

            @Override
            public String getWoff() {
                return null;
            }
        };
    }

    private static FontFamily family(String name, FontFace normal, FontFace bold) {
        return new FontFamily() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public FontFace getNormalFace() {
                return normal;
            }

            @Override
            public FontFace getBoldFace() {
                return bold;
            }

            @Override
            public FontFace getItalicFace() {
                return normal;
            }

            @Override
            public FontFace getBoldItalicFace() {
                return bold;
            }

            @Override
            public String getPdfEncoding() {
                return PDF_ENCODING;
            }

            @Override
            public Boolean isPdfEmbedded() {
                return false;
            }

            @Override
            public String getExportFont(String key) {
                return null;
            }

            @Override
            public boolean supportsLocale(Locale locale) {
                return true;
            }

            @Override
            public boolean isVisible() {
                return true;
            }
        };
    }
}
