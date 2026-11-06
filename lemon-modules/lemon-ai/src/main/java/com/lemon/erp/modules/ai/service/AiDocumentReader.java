package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ai.client.LlmImage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 智能录入：把上传的内容读成给模型的输入（文字 + 图片）。只在内存中处理，不保存文件（请求结束后 Spring 删除临时文件）。
 * <ul>
 *   <li>图片（jpg / png / webp，按文件头识别，不信任扩展名）：最多 {@value #MAX_IMAGES} 张、每张不超过 5MB，交给识图模型</li>
 *   <li>PDF：用 openpdf 提取文字（最多前 {@value #MAX_PDF_PAGES} 页）；提取不到文字的（扫描件）提示用户截图后以图片上传。
 *       服务端不把 PDF 渲染成图片：openpdf 不支持渲染，引入 PDFBox 等渲染库体积和内存开销都较大，而截图上传的效果相同</li>
 *   <li>txt / csv：按 UTF-8 解码，不是合法 UTF-8 时按 GB18030（Excel 另存的中文 CSV）</li>
 *   <li>Excel 由前端解析成表格文字后作为文本提交（前端已有 exceljs，不给后端增加依赖）</li>
 * </ul>
 * 文字合计超过 {@value #MAX_TEXT_CHARS} 个字符时截断（{@link Document#truncated()}）。
 */
@Slf4j
@Component
public class AiDocumentReader {

    public static final int MAX_IMAGES = 5;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_PDF_BYTES = 10L * 1024 * 1024;
    public static final long MAX_TEXT_FILE_BYTES = 2L * 1024 * 1024;
    public static final int MAX_FILES = 6;
    public static final int MAX_PDF_PAGES = 20;
    public static final int MAX_TEXT_CHARS = 20000;
    /** PDF 中可见字符少于该数时视为扫描件 */
    private static final int MIN_PDF_CHARS = 10;

    /** 输入的来源，前端显示用 */
    public enum Source { TEXT, IMAGE, PDF, FILE }

    /**
     * @param text      文字内容（可能为空串）
     * @param images    图片
     * @param source    主要来源
     * @param truncated 文字过长被截断，或 PDF 只读取了前若干页
     */
    public record Document(String text, List<LlmImage> images, Source source, boolean truncated) {
        public boolean hasImages() {
            return !images.isEmpty();
        }
    }

    /**
     * @param text  粘贴的文字（或前端解析 Excel 得到的表格文字），可为 null
     * @param files 上传的文件，可为 null
     * @throws BizException 没有内容、文件类型不支持、超过大小或数量、PDF 无法读取或没有文字
     */
    public Document read(String text, List<MultipartFile> files) {
        StringBuilder sb = new StringBuilder();
        List<LlmImage> images = new ArrayList<>();
        Source source = Source.TEXT;
        boolean truncated = false;
        List<MultipartFile> list = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (list.size() > MAX_FILES) {
            throw new BizException("ai.error.extractTooManyFiles", MAX_FILES);
        }
        for (MultipartFile file : list) {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            byte[] bytes = bytes(file);
            String imageType = imageType(bytes);
            if (imageType != null) {
                if (images.size() >= MAX_IMAGES) {
                    throw new BizException("ai.error.extractTooManyImages", MAX_IMAGES);
                }
                if (bytes.length > MAX_IMAGE_BYTES) {
                    throw new BizException("ai.error.extractFileTooLarge", name, MAX_IMAGE_BYTES / 1024 / 1024);
                }
                images.add(new LlmImage(imageType, bytes));
                source = Source.IMAGE;
            } else if (isPdf(bytes)) {
                if (bytes.length > MAX_PDF_BYTES) {
                    throw new BizException("ai.error.extractFileTooLarge", name, MAX_PDF_BYTES / 1024 / 1024);
                }
                PdfText pdf = pdfText(name, bytes);
                append(sb, pdf.text());
                truncated |= pdf.truncated();
                if (source != Source.IMAGE) {
                    source = Source.PDF;
                }
            } else if (isTextFile(name)) {
                if (bytes.length > MAX_TEXT_FILE_BYTES) {
                    throw new BizException("ai.error.extractFileTooLarge", name, MAX_TEXT_FILE_BYTES / 1024 / 1024);
                }
                append(sb, decode(bytes));
                if (source == Source.TEXT) {
                    source = Source.FILE;
                }
            } else {
                throw new BizException("ai.error.extractUnsupportedFile", name);
            }
        }
        if (StringUtils.hasText(text)) {
            append(sb, text.trim());
        }
        String all = sb.toString();
        if (all.length() > MAX_TEXT_CHARS) {
            all = all.substring(0, MAX_TEXT_CHARS);
            truncated = true;
        }
        if (!StringUtils.hasText(all) && images.isEmpty()) {
            throw new BizException("ai.error.extractEmpty");
        }
        return new Document(all, List.copyOf(images), source, truncated);
    }

    private static void append(StringBuilder sb, String s) {
        if (!StringUtils.hasText(s)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(s.strip());
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BizException("ai.error.extractReadFailed", file.getOriginalFilename());
        }
    }

    /** 按文件头识别图片类型，不是支持的图片时返回 null */
    static String imageType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    static boolean isPdf(byte[] b) {
        return b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
    }

    private static boolean isTextFile(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".txt") || n.endsWith(".csv") || n.endsWith(".tsv");
    }

    /** UTF-8（去掉 BOM），不是合法 UTF-8 时按 GB18030 */
    static String decode(byte[] bytes) {
        int offset = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
        ByteBuffer buf = ByteBuffer.wrap(bytes, offset, bytes.length - offset);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(buf).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, offset, bytes.length - offset, Charset.forName("GB18030"));
        }
    }

    private record PdfText(String text, boolean truncated) {
    }

    private static PdfText pdfText(String name, byte[] bytes) {
        PdfReader reader = null;
        try {
            reader = new PdfReader(bytes);
            int pages = reader.getNumberOfPages();
            int read = Math.min(pages, MAX_PDF_PAGES);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= read; i++) {
                String page = extractor.getTextFromPage(i);
                if (StringUtils.hasText(page)) {
                    sb.append(page.strip()).append('\n');
                }
            }
            if (sb.toString().replaceAll("\\s", "").length() < MIN_PDF_CHARS) {
                throw new BizException("ai.error.pdfNoText", name);
            }
            return new PdfText(sb.toString(), pages > read);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 加密、损坏的 PDF
            log.info("PDF 无法读取：{}，{}", name, e.toString());
            throw new BizException("ai.error.pdfUnreadable", name);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
