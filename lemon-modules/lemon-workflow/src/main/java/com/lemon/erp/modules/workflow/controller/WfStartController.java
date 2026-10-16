package com.lemon.erp.modules.workflow.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.workflow.dto.StartFormView;
import com.lemon.erp.modules.workflow.entity.WfModel;
import com.lemon.erp.modules.workflow.service.WfModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 发起申请：可发起的流程及其表单
 */
@RestController
@RequestMapping("/workflow/start")
@RequiredArgsConstructor
public class WfStartController {

    private final WfModelService modelService;

    @GetMapping("/models")
    @PreAuthorize("@ss.hasPerm('wf:start:list')")
    public R<List<WfModel>> models() {
        return R.ok(modelService.listStartable());
    }

    @GetMapping("/models/{modelKey}")
    @PreAuthorize("@ss.hasPerm('wf:start:list')")
    public R<StartFormView> form(@PathVariable String modelKey) {
        return R.ok(modelService.getStartForm(modelKey));
    }
}
