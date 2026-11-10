package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 呆滞物料：有库存但超过指定天数没有出库的物料，按库存金额降序，复用 {@link BiDecisionService#slowMoving}（需要全部数据权限）
 */
@Component
@RequiredArgsConstructor
public class BiSlowMovingTool implements AiTool {

    /** 呆滞物料，金额单位元；lastOut 最近出库的业务日期，idleDays 闲置天数 */
    record SlowItem(String code, String name, String category, String unit, BigDecimal qty, BigDecimal amount, String lastOut, Long idleDays) {
    }

    private final BiDecisionService decisionService;

    @Override
    public String name() {
        return "bi_slow_moving";
    }

    @Override
    public String labelKey() {
        return "bi.ai.tool.slowMoving";
    }

    @Override
    public String descriptionKey() {
        return "bi.ai.tool.slowMoving.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("days", AiSchema.integer("bi.ai.param.slowDays").min(7).max(1095), false)
                .prop("top", AiSchema.integer("bi.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "bi:decision:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        BiAiSupport.requireAllData();
        List<BiInsight.SlowMovingRow> list = decisionService.slowMoving(args.getInt("days", BiDecisionService.SLOW_DAYS));
        List<SlowItem> rows = list.stream().limit(args.getInt("top", 20))
                .map(r -> new SlowItem(r.getCode(), r.getName(), r.getCategory(), r.getUnit(), r.getQty(), r.getAmount(), r.getLastOut(), r.getIdleDays()))
                .toList();
        return AiToolResult.rows(rows, (long) list.size());
    }
}
