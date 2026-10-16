package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.scm.dto.ReorderSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 补货提醒：预计库存不高于再订货点的物料，每天提醒采购人员（scm:reorder:list）一次
 */
@Component
@RequiredArgsConstructor
public class ScmReorderJob implements JobHandler {

    private final ScmReorderService reorderService;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "scm.reorder";
    }

    @Override
    public String name() {
        return "补货提醒";
    }

    @Override
    public String defaultCron() {
        return "0 30 8 * * *";
    }

    @Override
    public String run(JobParams params) {
        List<ReorderSuggestion> list = reorderService.suggestions(null, true);
        if (list.isEmpty()) {
            return "没有需要补货的物料";
        }
        String names = list.stream().limit(5).map(s -> s.code() + " " + s.name()).collect(Collectors.joining("、"));
        messageSender.sendToPermission("scm:reorder:list", Message.of("ALERT", list.size() + " 个物料低于再订货点",
                names + (list.size() > 5 ? " 等" : "") + "，请查看补货建议并生成请购单", "/scm/reorder")
                .dedup("reorder:" + LocalDate.now()));
        return "需要补货 " + list.size() + " 个物料";
    }
}
