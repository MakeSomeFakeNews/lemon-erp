package com.lemon.erp.modules.workflow.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.workflow.entity.WfModel;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import com.lemon.erp.modules.workflow.service.WfModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程模型：设计、发布
 */
@RestController
@RequestMapping("/workflow/model")
@RequiredArgsConstructor
public class WfModelController {

    private final WfModelService modelService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wf:model:list')")
    public R<PageResult<WfModel>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String category) {
        return R.ok(modelService.page(current, size, name, category));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wf:model:list')")
    public R<WfModel> detail(@PathVariable Long id) {
        return R.ok(modelService.detail(id));
    }

    /** @return 新模型 ID，前端据此直接进入设计 */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('wf:model:add')")
    @OperLog(title = "流程模型", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody WfModel model) {
        return R.ok(modelService.create(model));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('wf:model:edit')")
    @OperLog(title = "流程模型", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WfModel model) {
        modelService.update(id, model);
        return R.ok();
    }

    @PutMapping("/{id}/design")
    @PreAuthorize("@ss.hasPerm('wf:model:edit')")
    // 设计 JSON 可能很大，不记录请求参数
    @OperLog(title = "流程模型-保存设计", type = OperType.UPDATE, saveParams = false)
    public R<Void> saveDesign(@PathVariable Long id, @RequestBody FlowDesign design) {
        modelService.saveDesign(id, design);
        return R.ok();
    }

    /** @return 发布后的版本号 */
    @PostMapping("/{id}/deploy")
    @PreAuthorize("@ss.hasPerm('wf:model:deploy')")
    @OperLog(title = "流程模型-发布", type = OperType.UPDATE)
    public R<Integer> deploy(@PathVariable Long id) {
        return R.ok(modelService.deploy(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('wf:model:edit')")
    @OperLog(title = "流程模型-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        modelService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('wf:model:remove')")
    @OperLog(title = "流程模型", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        modelService.delete(ids);
        return R.ok();
    }
}
