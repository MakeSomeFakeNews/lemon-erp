package com.lemon.erp.modules.bi.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.bi.service.BiTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 单据追溯
 */
@RestController
@RequestMapping("/bi/trace")
@RequiredArgsConstructor
public class BiTraceController {

    private final BiTraceService traceService;

    /** 订单的下游单据：kind = purchase / sales，权限同查看订单 */
    @GetMapping("/order/{kind:purchase|sales}/{id}")
    @PreAuthorize("#kind == 'purchase' ? @ss.hasPerm('scm:purchase:list') : @ss.hasPerm('scm:sales:list')")
    public R<Map<String, List<Map<String, Object>>>> order(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(traceService.order(kind, id));
    }
}
