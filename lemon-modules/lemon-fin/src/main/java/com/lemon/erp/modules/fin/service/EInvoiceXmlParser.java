package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.dto.InvoiceImport;
import com.lemon.erp.modules.fin.model.InvoiceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 数电票（全面数字化的电子发票）XML 解析：按元素名取发票号码、开票日期、销售方 / 购买方、金额、税额和明细行。
 * 不同版本的元素层级略有差异，按名称在整个文档中查找；禁用 DTD 和外部实体，防止 XXE
 */
final class EInvoiceXmlParser {

    private EInvoiceXmlParser() {
    }

    static InvoiceImport parse(byte[] content) {
        Document doc = read(content);
        String invoiceNo = text(doc.getDocumentElement(), "InvoiceNumber", "EIid");
        if (invoiceNo == null) {
            throw new BizException("fin.error.notFullyDigitalizedEInvoiceXml");
        }
        String date = text(doc.getDocumentElement(), "IssueTime", "RequestTime");
        if (date == null || date.length() < 10) {
            throw new BizException("fin.error.eInvoiceHasNoIssueDate", invoiceNo);
        }
        BigDecimal net = decimal(text(doc.getDocumentElement(), "TotalAmWithoutTax"));
        BigDecimal tax = decimal(text(doc.getDocumentElement(), "TotalTaxAm"));
        BigDecimal total = decimal(text(doc.getDocumentElement(), "TotalTax-includedAmount"));
        if (total == null) {
            if (net == null) {
                throw new BizException("fin.error.eInvoiceHasNoAmount", invoiceNo);
            }
            total = net.add(tax == null ? BigDecimal.ZERO : tax);
        }
        if (tax == null) {
            tax = net == null ? BigDecimal.ZERO : total.subtract(net);
        }
        Element typeEl = first(doc.getDocumentElement(), "EInvoiceType");
        String typeName = typeEl == null ? null : text(typeEl, "LabelName");
        // 按 XML 中的发票类型名称识别
        String type = (typeName != null && typeName.contains("普通") ? InvoiceType.E_NORMAL : InvoiceType.E_SPECIAL).name(); // i18n-ignore
        List<InvoiceImport.Line> lines = new ArrayList<>();
        NodeList items = doc.getElementsByTagNameNS("*", "IssuItemInformation");
        if (items.getLength() == 0) {
            items = doc.getElementsByTagName("IssuItemInformation");
        }
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String name = text(item, "ItemName");
            if (name == null) {
                continue;
            }
            BigDecimal amount = decimal(text(item, "Amount"));
            BigDecimal lineTax = decimal(text(item, "ComTaxAm"));
            lines.add(new InvoiceImport.Line(name, text(item, "SpecMod"), text(item, "MeaUnits"), decimal(text(item, "Quantity")),
                    decimal(text(item, "UnPrice")), amount == null ? BigDecimal.ZERO : amount, rate(text(item, "TaxRate")),
                    lineTax == null ? BigDecimal.ZERO : lineTax));
        }
        return new InvoiceImport(invoiceNo, type, LocalDate.parse(date.substring(0, 10)),
                text(doc.getDocumentElement(), "SellerIdNum"), text(doc.getDocumentElement(), "SellerName"),
                text(doc.getDocumentElement(), "BuyerIdNum"), text(doc.getDocumentElement(), "BuyerName"),
                total.setScale(2, RoundingMode.HALF_UP), tax.setScale(2, RoundingMode.HALF_UP), lines);
    }

    private static Document read(byte[] content) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setNamespaceAware(true);
            DocumentBuilder builder = f.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(content));
        } catch (Exception e) {
            throw new BizException("fin.error.failedParseXmlFile", e.getMessage());
        }
    }

    /** 第一个名称匹配的元素（忽略命名空间） */
    private static Element first(Element root, String name) {
        NodeList list = root.getElementsByTagNameNS("*", name);
        if (list.getLength() == 0) {
            list = root.getElementsByTagName(name);
        }
        return list.getLength() == 0 ? null : (Element) list.item(0);
    }

    /** 依次按名称查找元素，返回第一个非空的文本 */
    private static String text(Element root, String... names) {
        for (String name : names) {
            Element el = first(root, name);
            if (el != null && hasOnlyText(el)) {
                String v = el.getTextContent();
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
        }
        return null;
    }

    private static boolean hasOnlyText(Element el) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal decimal(String v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.replace(",", "").replace("¥", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 税率：0.13、13% 或 13 都按 0.13 */
    private static BigDecimal rate(String v) {
        if (v == null) {
            return null;
        }
        BigDecimal r = decimal(v.replace("%", ""));
        if (r == null) {
            return null;
        }
        return r.compareTo(BigDecimal.ONE) > 0 || v.contains("%") ? r.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP) : r;
    }
}
