package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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

import java.util.List;
import java.util.Map;
import com.lemon.erp.modules.mes.dto.MaintenanceFinishRequest;
import com.lemon.erp.modules.mes.dto.MaintenanceRequest;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesMaintenance;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.MaintenanceStatus;
import com.lemon.erp.modules.mes.model.MaintenanceType;
import com.lemon.erp.modules.mes.service.MesEquipmentService;

/**
 * 设备台账 /mes/equipment、维修保养 /mes/maintenance
 */
@RestController
@RequestMapping("/mes")
@RequiredArgsConstructor
public class MesEquipmentController {

    private final MesEquipmentService equipmentService;

    @GetMapping("/equipment")
    @PreAuthorize("@ss.hasPerm('mes:equipment:list')")
    public R<PageResult<MesEquipment>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) EquipmentStatus status,
                                            @RequestParam(required = false) Long workCenterId,
                                            @RequestParam(required = false) Integer dueDays) {
        return R.ok(equipmentService.page(current, size, keyword, status, workCenterId, dueDays));
    }

    /** 登录即可访问：维护单选择设备 */
    @GetMapping("/equipment/options")
    public R<List<MesEquipment>> options() {
        return R.ok(equipmentService.options());
    }

    @GetMapping("/equipment/{id}")
    @PreAuthorize("@ss.hasPerm('mes:equipment:list')")
    public R<MesEquipment> get(@PathVariable Long id) {
        return R.ok(equipmentService.get(id));
    }

    @PostMapping("/equipment")
    @PreAuthorize("@ss.hasPerm('mes:equipment:add')")
    @OperLog(title = "设备台账", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody MesEquipment equipment) {
        equipmentService.create(equipment);
        return R.ok();
    }

    @PutMapping("/equipment/{id}")
    @PreAuthorize("@ss.hasPerm('mes:equipment:edit')")
    @OperLog(title = "设备台账", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody MesEquipment equipment) {
        equipmentService.update(id, equipment);
        return R.ok();
    }

    @DeleteMapping("/equipment/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:equipment:remove')")
    @OperLog(title = "设备台账", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        equipmentService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 维修保养

    @GetMapping("/maintenance")
    @PreAuthorize("@ss.hasPerm('mes:maintain:list')")
    public R<PageResult<MesMaintenance>> maintenancePage(@RequestParam(defaultValue = "1") long current,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) Long equipmentId,
                                                         @RequestParam(required = false) MaintenanceType type,
                                                         @RequestParam(required = false) MaintenanceStatus status,
                                                         @RequestParam(required = false) String keyword) {
        return R.ok(equipmentService.maintenancePage(current, size, equipmentId, type, status, keyword));
    }

    @PostMapping("/maintenance")
    @PreAuthorize("@ss.hasPerm('mes:maintain:add')")
    @OperLog(title = "设备维护", type = OperType.INSERT)
    public R<Long> createMaintenance(@Valid @RequestBody MaintenanceRequest req) {
        return R.ok(equipmentService.createMaintenance(req));
    }

    /** 开始处理，body：{ handler } */
    @PostMapping("/maintenance/{id}/start")
    @PreAuthorize("@ss.hasPerm('mes:maintain:handle')")
    @OperLog(title = "设备维护-开始", type = OperType.UPDATE)
    public R<Void> start(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        equipmentService.startMaintenance(id, body == null ? null : body.get("handler"));
        return R.ok();
    }

    @PostMapping("/maintenance/{id}/finish")
    @PreAuthorize("@ss.hasPerm('mes:maintain:handle')")
    @OperLog(title = "设备维护-完成", type = OperType.UPDATE)
    public R<Void> finish(@PathVariable Long id, @Valid @RequestBody MaintenanceFinishRequest req) {
        equipmentService.finishMaintenance(id, req);
        return R.ok();
    }

    @DeleteMapping("/maintenance/{ids}")
    @PreAuthorize("@ss.hasPerm('mes:maintain:remove')")
    @OperLog(title = "设备维护", type = OperType.DELETE)
    public R<Void> deleteMaintenance(@PathVariable List<Long> ids) {
        equipmentService.deleteMaintenance(ids);
        return R.ok();
    }
}
