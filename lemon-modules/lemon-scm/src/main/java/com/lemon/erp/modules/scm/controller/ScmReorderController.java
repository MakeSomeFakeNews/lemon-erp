package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.ReorderSuggestion;
import com.lemon.erp.modules.scm.dto.RequisitionRequest;
import com.lemon.erp.modules.scm.service.ScmReorderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 补货建议（再订货点）
 */
@RestController
@RequestMapping("/scm/reorder")
@RequiredArgsConstructor
public class ScmReorderController {

    private final ScmReorderService reorderService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:reorder:list')")
    public R<List<ReorderSuggestion>> suggestions(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "true") boolean onlyNeeded) {
        return R.ok(reorderService.suggestions(keyword, onlyNeeded));
    }

    public record ToRequisitionRequest(@NotEmpty(message = "请选择要补货的物料") List<RequisitionRequest.@Valid Item> items) {
    }

    /** 选中的建议生成请购单（草稿），返回请购单 ID */
    @PostMapping("/requisition")
    @PreAuthorize("@ss.hasPerm('scm:requisition:add')")
    @OperLog(title = "补货建议生成请购单", type = OperType.INSERT)
    public R<Long> toRequisition(@Valid @RequestBody ToRequisitionRequest req) {
        return R.ok(reorderService.toRequisition(req.items()));
    }
}
