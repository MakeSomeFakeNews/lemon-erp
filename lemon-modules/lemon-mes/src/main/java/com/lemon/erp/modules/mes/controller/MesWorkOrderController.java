package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.ReportRequest;
import com.lemon.erp.modules.mes.dto.WoDocRequest;
import com.lemon.erp.modules.mes.dto.WorkOrderQuery;
import com.lemon.erp.modules.mes.dto.WorkOrderRequest;
import com.lemon.erp.modules.mes.entity.MesReport;
import com.lemon.erp.modules.mes.entity.MesWoDoc;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.model.WoDocKind;
import com.lemon.erp.modules.mes.service.MesWoDocService;
import com.lemon.erp.modules.mes.service.MesWorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 生产工单：车间控制（下达、领料 / 退料、报工、完工入库、关闭）
 */
@RestController
@RequestMapping("/mes/wo")
@RequiredArgsConstructor
public class MesWorkOrderController {

    private final MesWorkOrderService workOrderService;
    private final MesWoDocService docService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('mes:wo:list')")
    public R<PageResult<MesWorkOrder>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            WorkOrderQuery query) {
        return R.ok(workOrderService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:wo:list')")
    public R<MesWorkOrder> detail(@PathVariable Long id) {
        return R.ok(workOrderService.detail(id));
    }

    /** @return 新工单 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('mes:wo:add')")
    @OperLog(title = "生产工单", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody WorkOrderRequest req) {
        return R.ok(workOrderService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('mes:wo:edit')")
    @OperLog(title = "生产工单", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WorkOrderRequest req) {
        workOrderService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("@ss.hasPerm('mes:wo:release')")
    @OperLog(title = "生产工单-下达", type = OperType.UPDATE) // i18n-ignore
    public R<Void> release(@PathVariable Long id) {
        workOrderService.release(id);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('mes:wo:release')")
    @OperLog(title = "生产工单-关闭", type = OperType.UPDATE) // i18n-ignore
    public R<Void> close(@PathVariable Long id) {
        workOrderService.close(id);
        return R.ok();
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("@ss.hasPerm('mes:wo:report')")
    @OperLog(title = "生产工单-报工", type = OperType.INSERT) // i18n-ignore
    public R<Void> report(@PathVariable Long id, @Valid @RequestBody ReportRequest req) {
        workOrderService.report(id, req);
        return R.ok();
    }

    /** 报工记录：按工单或报工日期查询 */
    @GetMapping("/reports")
    @PreAuthorize("@ss.hasPerm('mes:wo:list')")
    public R<PageResult<MesReport>> reports(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long woId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(workOrderService.reports(current, size, woId, beginDate, endDate));
    }

    /** 领料 issue / 退料 return（权限 mes:wo:issue）、完工入库 finish（权限 mes:wo:finish），返回单据 ID */
    @PostMapping("/{id}/{kind:issue|return|finish|scrap}")
    @PreAuthorize("@ss.hasPerm(#kind == 'finish' || #kind == 'scrap' ? 'mes:wo:finish' : 'mes:wo:issue')")
    @OperLog(title = "生产工单-领料退料完工", type = OperType.INSERT) // i18n-ignore
    public R<Long> createDoc(@PathVariable Long id, @PathVariable String kind, @Valid @RequestBody WoDocRequest req) {
        return R.ok(docService.create(id, WoDocKind.ofPath(kind), req));
    }

    @GetMapping("/docs")
    @PreAuthorize("@ss.hasPerm('mes:wo:list')")
    public R<PageResult<MesWoDoc>> docs(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) Long woId,
                                        @RequestParam(required = false) String kind) {
        return R.ok(docService.page(current, size, woId, kind));
    }

    @GetMapping("/docs/{docId}")
    @PreAuthorize("@ss.hasPerm('mes:wo:list')")
    public R<MesWoDoc> doc(@PathVariable Long docId) {
        return R.ok(docService.detail(docId));
    }

    public record QtyRequest(@jakarta.validation.constraints.NotNull(message = "请填写数量")
                             @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
                             @jakarta.validation.constraints.Digits(integer = 14, fraction = 4, message = "数量最多4位小数")
                             java.math.BigDecimal qty,
                             java.time.LocalDate planStart, java.time.LocalDate planEnd) {
    }

    /** 修改已下达工单的数量 */
    @PutMapping("/{id}/qty")
    @PreAuthorize("@ss.hasPerm('mes:wo:change')")
    @OperLog(title = "生产工单改量", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeQty(@PathVariable Long id, @Valid @RequestBody QtyRequest req) {
        workOrderService.changeQty(id, req.qty());
        return R.ok();
    }

    /** 拆分工单，返回新工单 ID */
    @PostMapping("/{id}/split")
    @PreAuthorize("@ss.hasPerm('mes:wo:change')")
    @OperLog(title = "生产工单拆分", type = OperType.INSERT) // i18n-ignore
    public R<Long> split(@PathVariable Long id, @Valid @RequestBody QtyRequest req) {
        return R.ok(workOrderService.split(id, req.qty(), req.planStart(), req.planEnd()));
    }

    /** 新建返工工单（草稿），sourceWoId 为返工来源工单（选填） */
    @PostMapping("/rework")
    @PreAuthorize("@ss.hasPerm('mes:wo:add')")
    @OperLog(title = "返工工单", type = OperType.INSERT)
    public R<Long> rework(@Valid @RequestBody WorkOrderRequest req, @RequestParam(required = false) Long sourceWoId) {
        return R.ok(workOrderService.createRework(req, sourceWoId));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:wo:remove')")
    @OperLog(title = "生产工单", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        workOrderService.delete(ids);
        return R.ok();
    }
}
