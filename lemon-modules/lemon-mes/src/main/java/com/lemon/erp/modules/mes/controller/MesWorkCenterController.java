package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.org.LaborCostProvider;
import com.lemon.erp.modules.mes.entity.MesWorkCenter;
import com.lemon.erp.modules.mes.service.MesWorkCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/mes/work-center")
@RequiredArgsConstructor
public class MesWorkCenterController {

    private final MesWorkCenterService workCenterService;
    private final LaborCostProvider laborCostProvider;

    /** 人工费率参考：生产岗位员工的平均小时人工成本（人力资源未建立薪资档案时为空） */
    @GetMapping("/labor-rate-reference")
    @PreAuthorize("@ss.hasPerm('mes:wc:list')")
    public R<BigDecimal> laborRateReference() {
        return R.ok(laborCostProvider.productionHourlyCost());
    }

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:wc:list')")
    public R<PageResult<MesWorkCenter>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        return R.ok(workCenterService.page(current, size, keyword, status));
    }

    /** 登录即可访问：BOM 工序、设备选择工作中心 */
    @GetMapping("/options")
    public R<List<MesWorkCenter>> options() {
        return R.ok(workCenterService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:wc:list')")
    public R<MesWorkCenter> get(@PathVariable Long id) {
        return R.ok(workCenterService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:wc:add')")
    @OperLog(title = "工作中心", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody MesWorkCenter wc) {
        workCenterService.create(wc);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:wc:edit')")
    @OperLog(title = "工作中心", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody MesWorkCenter wc) {
        workCenterService.update(id, wc);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('mes:wc:edit')")
    @OperLog(title = "工作中心-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        workCenterService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:wc:remove')")
    @OperLog(title = "工作中心", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        workCenterService.delete(ids);
        return R.ok();
    }
}
