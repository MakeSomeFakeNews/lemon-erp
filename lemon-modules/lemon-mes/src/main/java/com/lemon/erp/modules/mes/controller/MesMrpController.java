package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.lemon.erp.modules.mes.dto.MrpReleaseRequest;
import com.lemon.erp.modules.mes.dto.MrpResultRequest;
import com.lemon.erp.modules.mes.dto.MrpRunRequest;
import com.lemon.erp.modules.mes.entity.MesMrpResult;
import com.lemon.erp.modules.mes.entity.MesMrpRun;
import com.lemon.erp.modules.mes.service.MesMrpService;

@RestController
@RequestMapping("/mes/mrp")
@RequiredArgsConstructor
public class MesMrpController {

    private final MesMrpService mrpService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:mrp:list')")
    public R<PageResult<MesMrpRun>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size) {
        return R.ok(mrpService.page(current, size));
    }

    @GetMapping("/{runId}/results")
    @PreAuthorize("@ss.hasPerm('mes:mrp:list')")
    public R<List<MesMrpResult>> results(@PathVariable Long runId) {
        return R.ok(mrpService.results(runId));
    }

    /** @return 运算 ID */
    @PostMapping("/run")
    @PreAuthorize("@ss.hasPerm('mes:mrp:run')")
    @OperLog(title = "MRP 运算", type = OperType.INSERT)
    public R<Long> run(@Valid @RequestBody MrpRunRequest req) {
        return R.ok(mrpService.run(req));
    }

    @PutMapping("/result/{id}")
    @PreAuthorize("@ss.hasPerm('mes:mrp:release')")
    @OperLog(title = "MRP-调整建议", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateResult(@PathVariable Long id, @Valid @RequestBody MrpResultRequest req) {
        mrpService.updateResult(id, req);
        return R.ok();
    }

    /** @return 生成的工单号、采购订单号 */
    @PostMapping("/release")
    @PreAuthorize("@ss.hasPerm('mes:mrp:release')")
    @OperLog(title = "MRP-下达", type = OperType.INSERT) // i18n-ignore
    public R<List<String>> release(@Valid @RequestBody MrpReleaseRequest req) {
        return R.ok(mrpService.release(req));
    }
}
