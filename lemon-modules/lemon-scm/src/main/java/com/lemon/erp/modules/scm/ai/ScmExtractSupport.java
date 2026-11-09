package com.lemon.erp.modules.scm.ai;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiPrefill;
import com.lemon.erp.framework.ai.AiPrefillBuilder;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialUnit;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 供应链智能录入（{@code AiExtractTarget}）的公共部分：采购 / 销售订单的输出结构和预填转换，往来单位、物料、仓库、币种的匹配。
 * 只查询（经各自模块的 Service），不写库
 */
@Component
@RequiredArgsConstructor
public class ScmExtractSupport {

    /** 订单明细最多预填的行数（与订单的明细上限一致） */
    static final int MAX_ITEMS = 500;
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Z]{3}$");
    private static final Set<String> CNY_ALIASES = Set.of("RMB", "CNY", "人民币", "¥", "￥", "元"); // i18n-ignore 识别人民币的写法
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final ScmSupplierService supplierService;
    private final ScmCustomerService customerService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final ExchangeRateProvider rateProvider;

    // ---------------------------------------------------------------- 订单

    /**
     * 订单的输出结构
     *
     * @param partnerField   往来单位字段名（supplierName / customerName）
     * @param partnerDescKey 往来单位的说明
     */
    Map<String, Object> orderSchema(String partnerField, String partnerDescKey) {
        AiSchema item = AiSchema.object()
                .prop("materialCode", AiSchema.string("scm.ai.extractMaterialCode").maxLength(64), false)
                .prop("materialName", AiSchema.string("scm.ai.extractMaterialName").maxLength(128), true)
                .prop("spec", AiSchema.string("scm.ai.extractSpec").maxLength(128), false)
                .prop("unit", AiSchema.string("scm.ai.extractUnit").maxLength(16), false)
                .prop("qty", AiSchema.number("scm.ai.extractQty"), false)
                .prop("price", AiSchema.number("scm.ai.extractPrice"), false)
                .prop("taxRate", AiSchema.number("scm.ai.extractTaxRate"), false)
                .prop("amount", AiSchema.number("scm.ai.extractAmount"), false)
                .prop("deliveryDate", AiSchema.date("scm.ai.extractItemDeliveryDate"), false)
                .prop("remark", AiSchema.string("scm.ai.extractItemRemark").maxLength(255), false);
        return AiSchema.object()
                .prop(partnerField, AiSchema.string(partnerDescKey).maxLength(128), false)
                .prop("partnerTaxNo", AiSchema.string("scm.ai.extractPartnerTaxNo").maxLength(32), false)
                .prop("docNo", AiSchema.string("scm.ai.extractDocNo").maxLength(64), false)
                .prop("orderDate", AiSchema.date("scm.ai.extractOrderDate"), false)
                .prop("deliveryDate", AiSchema.date("scm.ai.extractDeliveryDate"), false)
                .prop("warehouse", AiSchema.string("scm.ai.extractWarehouse").maxLength(64), false)
                .prop("currency", AiSchema.string("scm.ai.extractCurrency").maxLength(8), false)
                .prop("wholeDiscount", AiSchema.number("scm.ai.extractWholeDiscount"), false)
                .prop("totalAmount", AiSchema.number("scm.ai.extractTotalAmount"), false)
                .prop("remark", AiSchema.string("scm.ai.extractRemark").maxLength(255), false)
                .prop("items", AiSchema.array("scm.ai.extractItems", item).maxItems(MAX_ITEMS), true)
                .build();
    }

    /**
     * 订单的预填数据，字段与前端 OrderForm.openCreate 一致：数量、单价按物料的基本单位（单据用的是辅助单位时换算，并带上 unitName / unitRatio），
     * 单价为订单币种的含税单价；单据上的整单优惠 / 抹零 / 折让填入 wholeDiscount
     *
     * @param purchase true 采购订单（往来单位为供应商），false 销售订单（客户）
     */
    AiPrefill orderPrefill(Map<String, Object> ex, boolean purchase) {
        AiPrefillBuilder b = new AiPrefillBuilder();
        String partnerText = AiPrefillBuilder.str(ex, purchase ? "supplierName" : "customerName");
        String taxNo = AiPrefillBuilder.str(ex, "partnerTaxNo");
        PartnerOption partner = purchase ? supplier(b, partnerText, taxNo) : customer(b, partnerText, taxNo);
        if (partner != null) {
            b.put("partnerId", partner.id()).put("partnerName", partner.code() + " " + partner.name());
        } else if (partnerText == null && taxNo == null) {
            b.required("partnerId", null, Messages.get(purchase ? "scm.label.supplier" : "scm.label.customer"));
        }
        String orderDate = b.date("orderDate", AiPrefillBuilder.str(ex, "orderDate"));
        b.put("orderDate", orderDate);
        b.put("deliveryDate", b.date("deliveryDate", AiPrefillBuilder.str(ex, "deliveryDate")));
        OptionItem warehouse = warehouse(b, AiPrefillBuilder.str(ex, "warehouse"));
        if (warehouse != null) {
            b.put("warehouseId", warehouse.id());
        }
        currency(b, AiPrefillBuilder.str(ex, "currency"), orderDate == null ? LocalDate.now() : LocalDate.parse(orderDate));
        String docNo = AiPrefillBuilder.str(ex, "docNo");
        String remark = AiPrefillBuilder.str(ex, "remark");
        if (docNo != null) {
            String no = Messages.get("scm.ai.extractDocNoRemark", docNo);
            remark = remark == null ? no : no + "; " + remark;
        }
        b.put("remark", cut(remark, 255));

        List<Map<String, Object>> rows = b.list(ex, "items", "items", MAX_ITEMS);
        if (rows.isEmpty()) {
            b.warn("items", null, "scm.ai.warnNoItems");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal linesTotal = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String prefix = "items[" + i + "].";
            Map<String, Object> item = new LinkedHashMap<>();
            String name = AiPrefillBuilder.str(row, "materialName");
            String code = AiPrefillBuilder.str(row, "materialCode");
            String spec = AiPrefillBuilder.str(row, "spec");
            String unit = AiPrefillBuilder.str(row, "unit");
            WmsMaterial m = material(b, prefix + "materialId", code, name, spec);
            BigDecimal qty = b.qty(prefix + "qty", AiPrefillBuilder.dec(row, "qty"));
            BigDecimal price = b.price(prefix + "price", qty, AiPrefillBuilder.dec(row, "price"), AiPrefillBuilder.dec(row, "amount"));
            if (qty != null && price != null) {
                linesTotal = linesTotal.add(qty.multiply(price));
            }
            if (m != null) {
                item.put("materialId", m.getId());
                item.put("materialCode", m.getCode());
                item.put("materialName", m.getName());
                item.put("unit", m.getUnit());
                item.put("serialEnabled", m.getSerialEnabled());
                BigDecimal ratio = unitRatio(b, prefix + "qty", m, unit);
                if (ratio != null) {
                    item.put("unitName", unit);
                    item.put("unitRatio", ratio);
                    qty = qty == null ? null : AiPrefillBuilder.plain(qty.multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                    price = price == null ? null : AiPrefillBuilder.plain(price.divide(ratio, 4, RoundingMode.HALF_UP));
                }
            } else {
                item.put("aiText", String.join(" ", Stream.of(code, name, spec, unit).filter(Objects::nonNull).toList()));
            }
            item.put("qty", qty);
            item.put("price", price);
            item.put("taxRate", b.taxRate(prefix + "taxRate", AiPrefillBuilder.dec(row, "taxRate")));
            item.put("deliveryDate", b.date(prefix + "deliveryDate", AiPrefillBuilder.str(row, "deliveryDate")));
            item.put("remark", cut(AiPrefillBuilder.str(row, "remark"), 255));
            item.values().removeIf(Objects::isNull);
            items.add(item);
        }
        // 整单优惠（抹零、折让）：表单按行金额比例分摊；单据合计为优惠后的金额
        BigDecimal wholeDiscount = b.nonNegative("wholeDiscount", AiPrefillBuilder.dec(ex, "wholeDiscount"));
        if (wholeDiscount != null && wholeDiscount.signum() > 0) {
            wholeDiscount = AiPrefillBuilder.plain(wholeDiscount.setScale(2, RoundingMode.HALF_UP));
            b.put("wholeDiscount", wholeDiscount);
            linesTotal = linesTotal.subtract(wholeDiscount);
        }
        b.total("totalAmount", AiPrefillBuilder.dec(ex, "totalAmount"), linesTotal);
        b.put("items", items);
        return b.build();
    }

    // ---------------------------------------------------------------- 往来单位

    /** 按税号、编码 / 名称精确匹配供应商，再按名称模糊搜索（只匹配启用的） */
    PartnerOption supplier(AiPrefillBuilder b, String name, String taxNo) {
        return partner(b, name, taxNo, Messages.get("scm.label.supplier"), supplierService::findIdByTaxNo,
                supplierService::findIdByCodeOrName, supplierService::options, this::supplierOption);
    }

    PartnerOption customer(AiPrefillBuilder b, String name, String taxNo) {
        return partner(b, name, taxNo, Messages.get("scm.label.customer"), customerService::findIdByTaxNo,
                customerService::findIdByCodeOrName, customerService::options, this::customerOption);
    }

    /** 启用的供应商，停用的为 null */
    private PartnerOption supplierOption(Long id) {
        ScmSupplier s = supplierService.get(id);
        return s.getStatus() == EnableStatus.ENABLED ? new PartnerOption(s.getId(), s.getCode(), s.getName(), s.getContact(), s.getPhone(),
                s.getAddress(), s.getLevel() == null ? null : s.getLevel().name()) : null;
    }

    private PartnerOption customerOption(Long id) {
        ScmCustomer c = customerService.get(id);
        return c.getStatus() == EnableStatus.ENABLED ? new PartnerOption(c.getId(), c.getCode(), c.getName(), c.getContact(), c.getPhone(),
                c.getAddress(), c.getLevel()) : null;
    }

    private PartnerOption partner(AiPrefillBuilder b, String name, String taxNo, String kind, Function<String, Long> byTaxNo,
                                  Function<String, Long> byCodeOrName, Function<String, List<PartnerOption>> search,
                                  Function<Long, PartnerOption> enabled) {
        if (taxNo != null) {
            Long id = byTaxNo.apply(taxNo);
            PartnerOption hit = id == null ? null : enabled.apply(id);
            if (hit != null) {
                return hit;
            }
        }
        return b.match("partnerId", "partnerName", name, kind, t -> {
            Long id = byCodeOrName.apply(t);
            return id == null ? null : enabled.apply(id);
        }, search, p -> p.code() + " " + p.name());
    }

    // ---------------------------------------------------------------- 物料、单位、仓库、币种

    /** 物料：编码精确 → 名称（和规格）精确 → 名称模糊；只匹配启用的具体物料（不含多规格款式） */
    WmsMaterial material(AiPrefillBuilder b, String field, String code, String name, String spec) {
        if (code != null) {
            WmsMaterial byCode = materialService.mapByCodes(List.of(code)).get(code);
            if (byCode != null && byCode.getStatus() == EnableStatus.ENABLED && !byCode.isTemplateMaterial()) {
                return byCode;
            }
        }
        String text = name != null ? name : code;
        return b.match(field, null, text, Messages.get("wms.label.material"), t -> {
            List<WmsMaterial> same = materialService.options(t).stream().filter(m -> m.getName().equals(t)).toList();
            if (same.size() > 1 && spec != null) {
                same = same.stream().filter(m -> sameSpec(m.getSpec(), spec)).toList();
            }
            return same.size() == 1 ? same.get(0) : null;
        }, t -> {
            List<WmsMaterial> found = materialService.options(t);
            if (found.size() > 1 && spec != null) {
                List<WmsMaterial> bySpec = found.stream().filter(m -> sameSpec(m.getSpec(), spec)).toList();
                return bySpec.isEmpty() ? found : bySpec;
            }
            return found;
        }, m -> m.getCode() + " " + m.getName() + (m.getSpec() == null || m.getSpec().isBlank() ? "" : " " + m.getSpec()));
    }

    /** 规格比较：忽略大小写、空白，* x × 视为相同 */
    static boolean sameSpec(String a, String b) {
        return a != null && b != null && normalizeSpec(a).equals(normalizeSpec(b));
    }

    private static String normalizeSpec(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s", "").replace('×', 'x').replace('*', 'x'); // i18n-ignore 乘号
    }

    /**
     * 单据上的单位与物料基本单位不同时，按物料的辅助单位换算
     *
     * @return 辅助单位的换算率（1 辅助单位 = ratio 基本单位）；单位为空、与基本单位相同或匹配不上时为 null（匹配不上时提示）
     */
    BigDecimal unitRatio(AiPrefillBuilder b, String field, WmsMaterial m, String unit) {
        if (unit == null || m.getUnit() == null || unit.equalsIgnoreCase(m.getUnit())) {
            return null;
        }
        for (WmsMaterialUnit u : materialService.units(m.getId())) {
            if (unit.equalsIgnoreCase(u.getUnit()) && u.getRatio() != null && u.getRatio().signum() > 0) {
                return u.getRatio();
            }
        }
        b.warn(field, unit, "scm.ai.warnUnitMismatch", unit, m.getName(), m.getUnit());
        return null;
    }

    /** 仓库：编码 / 名称精确，否则名称包含（启用的） */
    OptionItem warehouse(AiPrefillBuilder b, String text) {
        if (text == null) {
            return null;
        }
        List<OptionItem> all = warehouseService.options();
        return b.match("warehouseId", null, text, Messages.get("wms.label.warehouse"),
                t -> all.stream().filter(w -> t.equals(w.code()) || t.equals(w.name())).findFirst().orElse(null),
                t -> all.stream().filter(w -> w.name().contains(t) || t.contains(w.name())).toList(),
                w -> w.name());
    }

    /** 币种：人民币不设置（表单默认）；外币须已维护汇率，否则提示并按人民币 */
    void currency(AiPrefillBuilder b, String raw, LocalDate date) {
        if (raw == null) {
            return;
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (CNY_ALIASES.contains(code) || ExchangeRateProvider.BASE.equals(code)) {
            return;
        }
        BigDecimal rate = CURRENCY_CODE.matcher(code).matches() ? rateProvider.rate(code, date) : null;
        if (rate == null) {
            b.warn("currency", raw, "scm.ai.warnCurrencyNoRate", raw);
            return;
        }
        b.put("currency", code).put("exchangeRate", rate);
    }

    // ---------------------------------------------------------------- 往来单位资料

    /**
     * 供应商 / 客户资料的输出结构（名片、营业执照、开票信息）
     *
     * @param supplier 供应商（多邮箱、开户行、账号）
     */
    Map<String, Object> partnerSchema(boolean supplier) {
        AiSchema s = AiSchema.object()
                .prop("name", AiSchema.string("scm.ai.extractCompanyName").maxLength(128), false)
                .prop("contact", AiSchema.string("scm.ai.extractContact").maxLength(64), false)
                .prop("phone", AiSchema.string("scm.ai.extractPhone").maxLength(32), false)
                .prop("email", AiSchema.string("scm.ai.extractEmail").maxLength(128), false)
                .prop("address", AiSchema.string("scm.ai.extractAddress").maxLength(255), false)
                .prop("taxNo", AiSchema.string("scm.ai.extractTaxNo").maxLength(32), false);
        if (supplier) {
            s.prop("bankName", AiSchema.string("scm.ai.extractBankName").maxLength(128), false)
                    .prop("bankAccount", AiSchema.string("scm.ai.extractBankAccount").maxLength(64), false);
        }
        return s.prop("remark", AiSchema.string("scm.ai.extractPartnerRemark").maxLength(255), false).build();
    }

    /**
     * 供应商 / 客户资料的预填数据，字段与新增表单一致；已有相同税号或名称的提示可能重复。客户没有邮箱字段，邮箱写入备注
     */
    AiPrefill partnerPrefill(Map<String, Object> ex, boolean supplier) {
        AiPrefillBuilder b = new AiPrefillBuilder();
        String name = cut(AiPrefillBuilder.str(ex, "name"), 128);
        String taxNo = cut(AiPrefillBuilder.str(ex, "taxNo"), 32);
        b.required("name", name, Messages.get(supplier ? "scm.ai.labelSupplierName" : "scm.ai.labelCustomerName"));
        b.put("name", name).put("contact", cut(AiPrefillBuilder.str(ex, "contact"), 64)).put("phone", cut(AiPrefillBuilder.str(ex, "phone"), 32))
                .put("address", cut(AiPrefillBuilder.str(ex, "address"), 255)).put("taxNo", taxNo);
        String remark = AiPrefillBuilder.str(ex, "remark");
        String email = AiPrefillBuilder.str(ex, "email");
        if (email != null && !EMAIL.matcher(email).matches()) {
            b.warn("email", email, "scm.ai.warnEmailInvalid", email);
            email = null;
        }
        if (supplier) {
            b.put("email", cut(email, 128)).put("bankName", cut(AiPrefillBuilder.str(ex, "bankName"), 128))
                    .put("bankAccount", cut(AiPrefillBuilder.str(ex, "bankAccount"), 64));
        } else if (email != null) {
            remark = remark == null ? email : email + "; " + remark;
        }
        b.put("remark", cut(remark, 255));
        String kind = Messages.get(supplier ? "scm.label.supplier" : "scm.label.customer");
        Long dupId = taxNo == null ? null : (supplier ? supplierService.findIdByTaxNo(taxNo) : customerService.findIdByTaxNo(taxNo));
        String dupBy = Messages.get("scm.ai.labelTaxNo");
        if (dupId == null && name != null) {
            dupId = supplier ? supplierService.findIdByName(name) : customerService.findIdByName(name);
            dupBy = Messages.get("scm.ai.labelName");
        }
        if (dupId != null) {
            String existing;
            if (supplier) {
                ScmSupplier s = supplierService.get(dupId);
                existing = label(s.getCode(), s.getName());
            } else {
                ScmCustomer c = customerService.get(dupId);
                existing = label(c.getCode(), c.getName());
            }
            b.warn("name", name, "scm.ai.warnDuplicatePartner", dupBy, kind, existing);
        }
        return b.build();
    }

    private static String label(String code, String name) {
        return code + " " + name;
    }

    static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
