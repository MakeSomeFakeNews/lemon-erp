package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ai.dto.AiCleanseApplyResult;
import com.lemon.erp.modules.ai.dto.AiCleanseIdsRequest;
import com.lemon.erp.modules.ai.dto.AiCleanseSourceView;
import com.lemon.erp.modules.ai.dto.AiCleanseStartRequest;
import com.lemon.erp.modules.ai.dto.AiCleanseSuggestionQuery;
import com.lemon.erp.modules.ai.dto.AiCleanseTaskView;
import com.lemon.erp.modules.ai.entity.AiCleanseSuggestion;
import com.lemon.erp.modules.ai.entity.AiCleanseTask;
import com.lemon.erp.modules.ai.model.AiCleanseKind;
import com.lemon.erp.modules.ai.model.AiSuggestionStatus;
import com.lemon.erp.modules.ai.service.AiCleanseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据整理：数据类型、任务（发起、进度、取消）、建议（查看、采纳、忽略）。
 * 查看需要 ai:cleanse:list 和数据类型的查看权限；采纳 / 忽略需要 ai:cleanse:apply 和数据类型的应用权限（如 wms:material:edit）
 */
@RestController
@RequestMapping("/ai/cleanse")
@RequiredArgsConstructor
@PreAuthorize("@ss.hasPerm('ai:cleanse:list')")
public class AiCleanseController {

    private final AiCleanseService cleanseService;

    @GetMapping("/sources")
    public R<List<AiCleanseSourceView>> sources() {
        return R.ok(cleanseService.sources());
    }

    @GetMapping("/tasks")
    public R<PageResult<AiCleanseTask>> tasks(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size) {
        return R.ok(cleanseService.tasks(current, size));
    }

    @PostMapping("/tasks")
    @OperLog(title = "数据整理-发起", type = OperType.INSERT) // i18n-ignore 操作日志存库
    public R<AiCleanseTask> start(@Valid @RequestBody AiCleanseStartRequest req) {
        return R.ok(cleanseService.start(req.sourceKey(), req.keyword()));
    }

    @GetMapping("/tasks/{id}")
    public R<AiCleanseTaskView> task(@PathVariable Long id) {
        return R.ok(cleanseService.task(id));
    }

    @PostMapping("/tasks/{id}/cancel")
    @OperLog(title = "数据整理-取消", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> cancel(@PathVariable Long id) {
        cleanseService.cancel(id);
        return R.ok();
    }

    /**
     * @param kinds 种类（可多个，如 NORMALIZE,MISSING），为空不限
     */
    @GetMapping("/tasks/{id}/suggestions")
    public R<PageResult<AiCleanseSuggestion>> suggestions(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "1") long current,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(required = false) List<AiCleanseKind> kinds,
                                                          @RequestParam(required = false) AiSuggestionStatus status,
                                                          @RequestParam(required = false) AiCleanseField field,
                                                          @RequestParam(required = false) Integer minConfidence,
                                                          @RequestParam(required = false) String keyword) {
        return R.ok(cleanseService.suggestions(current, size,
                new AiCleanseSuggestionQuery(id, kinds, status, field, minConfidence, keyword == null || keyword.isBlank() ? null : keyword.strip())));
    }

    /** 采纳（规范化建议修改业务数据，疑似重复停用记录），逐条返回结果 */
    @PostMapping("/suggestions/apply")
    @PreAuthorize("@ss.hasPerm('ai:cleanse:apply')")
    @OperLog(title = "数据整理-采纳", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<List<AiCleanseApplyResult>> apply(@Valid @RequestBody AiCleanseIdsRequest req) {
        return R.ok(cleanseService.apply(req.ids()));
    }

    @PostMapping("/suggestions/ignore")
    @PreAuthorize("@ss.hasPerm('ai:cleanse:apply')")
    @OperLog(title = "数据整理-忽略", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Integer> ignore(@Valid @RequestBody AiCleanseIdsRequest req) {
        return R.ok(cleanseService.ignore(req.ids()));
    }
}
