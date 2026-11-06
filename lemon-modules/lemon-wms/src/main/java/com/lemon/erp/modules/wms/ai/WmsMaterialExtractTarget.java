package com.lemon.erp.modules.wms.ai;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.ai.AiPrefill;
import com.lemon.erp.framework.ai.AiPrefillBuilder;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 智能录入：物料（产品规格书、报价单中的单个商品、标签照片等），分类按业务字典 wms_material_category 的名称匹配，
 * 提示已有名称和规格相同的物料、编码已被使用
 */
@Component
@Order(50) // 目标列表中的顺序
@RequiredArgsConstructor
public class WmsMaterialExtractTarget implements AiExtractTarget {

    private static final String CATEGORY_DICT = "wms_material_category";
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");

    private final WmsMaterialService materialService;
    private final DictLookup dictLookup;

    @Override
    public String key() {
        return "wms.material";
    }

    @Override
    public String labelKey() {
        return "wms.label.material";
    }

    @Override
    public String permission() {
        return "wms:material:add";
    }

    @Override
    public Map<String, Object> schema() {
        List<String> categories = List.copyOf(dictLookup.options(CATEGORY_DICT).values());
        AiSchema category = categories.isEmpty() ? AiSchema.string("wms.ai.extractCategory").maxLength(64)
                : AiSchema.enumOf("wms.ai.extractCategory", categories.toArray(String[]::new));
        return AiSchema.object()
                .prop("code", AiSchema.string("wms.ai.extractCode").maxLength(64), false)
                .prop("name", AiSchema.string("wms.ai.extractName").maxLength(128), true)
                .prop("spec", AiSchema.string("wms.ai.extractSpec").maxLength(128), false)
                .prop("unit", AiSchema.string("wms.ai.extractUnit").maxLength(16), false)
                .prop("category", category, false)
                .prop("barcode", AiSchema.string("wms.ai.extractBarcode").maxLength(64), false)
                .prop("price", AiSchema.number("wms.ai.extractPrice"), false)
                .prop("remark", AiSchema.string("wms.ai.extractRemark").maxLength(255), false)
                .build();
    }

    @Override
    public String instructionKey() {
        return "wms.ai.extractInstruction";
    }

    @Override
    public AiPrefill prefill(Map<String, Object> ex) {
        AiPrefillBuilder b = new AiPrefillBuilder();
        String name = cut(AiPrefillBuilder.str(ex, "name"), 128);
        String spec = cut(AiPrefillBuilder.str(ex, "spec"), 128);
        b.required("name", name, Messages.get("wms.ai.labelName"));
        b.put("name", name).put("spec", spec).put("unit", cut(AiPrefillBuilder.str(ex, "unit"), 16))
                .put("barcode", cut(AiPrefillBuilder.str(ex, "barcode"), 64));

        String code = AiPrefillBuilder.str(ex, "code");
        if (code != null) {
            if (!CODE.matcher(code).matches()) {
                b.warn("code", code, "wms.ai.warnCodeInvalid", code);
            } else if (materialService.mapByCodes(List.of(code)).containsKey(code)) {
                b.warn("code", code, "wms.ai.warnCodeExists", code);
            } else {
                b.put("code", code);
            }
        }

        String category = AiPrefillBuilder.str(ex, "category");
        if (category != null) {
            String value = categoryValue(category);
            if (value == null) {
                b.warn("category", category, "wms.ai.warnCategoryNotFound", category);
            }
            b.put("category", value);
        }

        BigDecimal price = b.nonNegative("refCost", AiPrefillBuilder.dec(ex, "price"));
        b.put("refCost", price);
        b.put("remark", cut(AiPrefillBuilder.str(ex, "remark"), 255));

        if (name != null) {
            materialService.options(name).stream()
                    .filter(m -> m.getName().equals(name) && sameSpec(m.getSpec(), spec))
                    .findFirst()
                    .ifPresent(m -> b.warn("name", name, "wms.ai.warnDuplicate", label(m)));
        }
        return b.build();
    }

    /** 分类：按字典名称或键值匹配（忽略大小写），匹配不上为 null */
    private String categoryValue(String text) {
        for (Map.Entry<String, String> e : dictLookup.options(CATEGORY_DICT).entrySet()) {
            if (e.getValue().equals(text) || e.getKey().equalsIgnoreCase(text)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 规格相同：都为空，或忽略大小写和空白后相同 */
    private static boolean sameSpec(String a, String b) {
        String x = a == null ? "" : a.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
        String y = b == null ? "" : b.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
        return x.equals(y);
    }

    private static String label(WmsMaterial m) {
        return m.getCode() + " " + m.getName() + (m.getSpec() == null || m.getSpec().isBlank() ? "" : " " + m.getSpec());
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public String route() {
        return "/wms/material";
    }

    @Override
    public String form() {
        return "material";
    }
}
