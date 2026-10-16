package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.dto.StopRequest;
import com.lemon.erp.modules.mes.entity.MesEquipmentStop;
import com.lemon.erp.modules.mes.model.StopReason;
import com.lemon.erp.modules.mes.service.MesOeeService;
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
 * 设备 OEE 与停机记录
 */
@RestController
@RequestMapping("/mes")
@RequiredArgsConstructor
public class MesOeeController {

    private final MesOeeService oeeService;

    /** 期间内各设备的 OEE */
    @GetMapping("/oee")
    @PreAuthorize("@ss.hasPerm('mes:oee:list')")
    public R<List<MesOeeService.Oee>> summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                              @RequestParam(required = false) Long workCenterId) {
        return R.ok(oeeService.summary(beginDate, endDate, workCenterId));
    }

    /** 设备 OEE 明细：期间合计、每天、停机原因 */
    @GetMapping("/oee/{equipmentId}")
    @PreAuthorize("@ss.hasPerm('mes:oee:list')")
    public R<MesOeeService.Detail> detail(@PathVariable Long equipmentId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(oeeService.detail(equipmentId, beginDate, endDate));
    }

    @GetMapping("/equipment-stop")
    @PreAuthorize("@ss.hasPerm('mes:oee:list')")
    public R<PageResult<MesEquipmentStop>> stops(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) Long equipmentId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                 @RequestParam(required = false) StopReason reason) {
        return R.ok(oeeService.stops(current, size, equipmentId, beginDate, endDate, reason));
    }

    @PostMapping("/equipment-stop")
    @PreAuthorize("@ss.hasPerm('mes:equipment:stop')")
    @OperLog(title = "设备停机", type = OperType.INSERT)
    public R<Long> createStop(@Valid @RequestBody StopRequest req) {
        return R.ok(oeeService.createStop(req));
    }

    @PutMapping("/equipment-stop/{id}")
    @PreAuthorize("@ss.hasPerm('mes:equipment:stop')")
    @OperLog(title = "设备停机", type = OperType.UPDATE)
    public R<Void> updateStop(@PathVariable Long id, @Valid @RequestBody StopRequest req) {
        oeeService.updateStop(id, req);
        return R.ok();
    }

    @DeleteMapping("/equipment-stop/{id}")
    @PreAuthorize("@ss.hasPerm('mes:equipment:stop')")
    @OperLog(title = "设备停机", type = OperType.DELETE)
    public R<Void> deleteStop(@PathVariable Long id) {
        oeeService.deleteStop(id);
        return R.ok();
    }
}
