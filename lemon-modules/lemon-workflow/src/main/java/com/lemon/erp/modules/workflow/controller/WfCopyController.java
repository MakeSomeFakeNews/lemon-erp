package com.lemon.erp.modules.workflow.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.workflow.entity.WfCopy;
import com.lemon.erp.modules.workflow.service.WfCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抄送我的
 */
@RestController
@RequestMapping("/workflow/copy")
@RequiredArgsConstructor
public class WfCopyController {

    private final WfCopyService copyService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wf:copy:list')")
    public R<PageResult<WfCopy>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String name) {
        return R.ok(copyService.pageMine(name, current, size));
    }
}
