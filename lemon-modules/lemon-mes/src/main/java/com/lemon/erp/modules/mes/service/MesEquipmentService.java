package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.MaintenanceFinishRequest;
import com.lemon.erp.modules.mes.dto.MaintenanceRequest;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesMaintenance;
import com.lemon.erp.modules.mes.entity.MesEquipmentStop;
import com.lemon.erp.modules.mes.mapper.MesEquipmentMapper;
import com.lemon.erp.modules.mes.mapper.MesEquipmentStopMapper;
import com.lemon.erp.modules.mes.mapper.MesMaintenanceMapper;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.MaintenanceFinishedEvent;
import com.lemon.erp.modules.mes.model.MaintenanceStatus;
import com.lemon.erp.modules.mes.model.MaintenanceType;
import com.lemon.erp.modules.mes.model.StopReason;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 设备台账与维护：定期保养按周期计算下次保养日期并提醒；故障维修开始时设备变为维修中，完成后恢复运行并记录停机时长
 */
@Service
@RequiredArgsConstructor
public class MesEquipmentService {

    private final MesEquipmentMapper equipmentMapper;
    private final MesMaintenanceMapper maintenanceMapper;
    private final MesEquipmentStopMapper stopMapper;
    private final DocNoService docNoService;
    private final ApplicationEventPublisher eventPublisher;

    /** @param dueDays 只看 dueDays 天内到期（含已逾期）需要保养的设备 */
    public PageResult<MesEquipment> page(long current, long size, String keyword, EquipmentStatus status, Long workCenterId, Integer dueDays) {
        Page<MesEquipment> page = Page.of(current, size);
        equipmentMapper.selectPageList(page, keyword, status, workCenterId, dueDays == null ? null : LocalDate.now().plusDays(dueDays));
        return PageResult.of(page);
    }

    public MesEquipment get(Long id) {
        MesEquipment equipment = id == null ? null : equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BizException("mes.error.equipmentDoesNotExist");
        }
        return equipment;
    }

    /** 未报废的设备，维护单选择 */
    public List<MesEquipment> options() {
        return equipmentMapper.selectList(Wrappers.<MesEquipment>lambdaQuery()
                .select(MesEquipment::getId, MesEquipment::getCode, MesEquipment::getName, MesEquipment::getStatus, MesEquipment::getWorkCenterId)
                .ne(MesEquipment::getStatus, EquipmentStatus.SCRAPPED)
                .orderByAsc(MesEquipment::getCode));
    }

    public void create(MesEquipment equipment) {
        checkCodeUnique(equipment.getCode(), null);
        equipment.setId(null);
        normalize(equipment);
        equipmentMapper.insert(equipment);
    }

    public void update(Long id, MesEquipment equipment) {
        get(id);
        checkCodeUnique(equipment.getCode(), id);
        normalize(equipment);
        equipmentMapper.update(Wrappers.<MesEquipment>lambdaUpdate()
                .set(MesEquipment::getCode, equipment.getCode())
                .set(MesEquipment::getName, equipment.getName())
                .set(MesEquipment::getModel, equipment.getModel())
                .set(MesEquipment::getWorkCenterId, equipment.getWorkCenterId())
                .set(MesEquipment::getLocation, equipment.getLocation())
                .set(MesEquipment::getPurchaseDate, equipment.getPurchaseDate())
                .set(MesEquipment::getStatus, equipment.getStatus())
                .set(MesEquipment::getMaintainCycleDays, equipment.getMaintainCycleDays())
                .set(MesEquipment::getLastMaintainDate, equipment.getLastMaintainDate())
                .set(MesEquipment::getNextMaintainDate, equipment.getNextMaintainDate())
                .set(MesEquipment::getRemark, equipment.getRemark())
                .set(MesEquipment::getUpdateTime, LocalDateTime.now())
                .eq(MesEquipment::getId, id));
    }

    /** 有维护记录的设备不能删除，可以报废 */
    public void delete(List<Long> ids) {
        for (MesEquipment e : equipmentMapper.selectByIds(ids)) {
            if (maintenanceMapper.exists(Wrappers.<MesMaintenance>lambdaQuery().eq(MesMaintenance::getEquipmentId, e.getId()))) {
                throw new BizException("mes.error.equipmentHasMaintenanceRecordsCannotDeleted", e.getName());
            }
        }
        equipmentMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 维护单

    public PageResult<MesMaintenance> maintenancePage(long current, long size, Long equipmentId, MaintenanceType type, MaintenanceStatus status,
                                                     String keyword) {
        Page<MesMaintenance> page = Page.of(current, size);
        maintenanceMapper.selectPageList(page, equipmentId, type, status, keyword);
        return PageResult.of(page);
    }

    @Transactional
    public Long createMaintenance(MaintenanceRequest req) {
        MesEquipment equipment = get(req.equipmentId());
        if (equipment.getStatus() == EquipmentStatus.SCRAPPED) {
            throw new BizException("mes.error.equipmentHasScrapped");
        }
        MesMaintenance m = new MesMaintenance();
        m.setOrderNo(docNoService.next(MesDocNos.MAINTENANCE));
        m.setEquipmentId(req.equipmentId());
        m.setType(req.type());
        m.setContent(req.content().trim());
        m.setPlanDate(req.planDate() == null ? LocalDate.now() : req.planDate());
        m.setHandler(req.handler());
        m.setStatus(MaintenanceStatus.PENDING);
        m.setCreateBy(SecurityUtils.getUserId());
        maintenanceMapper.insert(m);
        return m.getId();
    }

    /** 开始处理：故障维修时设备变为维修中 */
    @Transactional
    public void startMaintenance(Long id, String handler) {
        MesMaintenance m = getMaintenance(id);
        MesEquipment equipment = get(m.getEquipmentId());
        if (!StatusTransitions.tryChange(maintenanceMapper, MesMaintenance::getId, id, MesMaintenance::getStatus,
                MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS,
                w -> w.set(MesMaintenance::getPrevStatus, equipment.getStatus())
                        .set(MesMaintenance::getStartTime, LocalDateTime.now())
                        .set(handler != null && !handler.isBlank(), MesMaintenance::getHandler, handler))) {
            throw new BizException("mes.error.onlyPendingMaintenanceOrdersStarted");
        }
        if (m.getType() == MaintenanceType.REPAIR) {
            setEquipmentStatus(m.getEquipmentId(), EquipmentStatus.REPAIR);
        }
    }

    /**
     * 完成：维修 → 设备恢复维修前的状态、记录停机时长；保养 → 更新上次 / 下次保养日期。
     * 开始到完成的时间记为设备停机（维修为故障停机，保养为计划停机），用于设备 OEE
     */
    @Transactional
    public void finishMaintenance(Long id, MaintenanceFinishRequest req) {
        MesMaintenance m = getMaintenance(id);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal downtime = m.getType() == MaintenanceType.REPAIR && m.getStartTime() != null
                ? BigDecimal.valueOf(Duration.between(m.getStartTime(), now).toMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                : null;
        if (!StatusTransitions.tryChange(maintenanceMapper, MesMaintenance::getId, id, MesMaintenance::getStatus,
                MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.DONE,
                w -> w.set(MesMaintenance::getFinishTime, now)
                        .set(MesMaintenance::getDowntime, downtime)
                        .set(MesMaintenance::getCost, req.cost())
                        .set(MesMaintenance::getResult, req.result().trim())
                        .set(req.handler() != null && !req.handler().isBlank(), MesMaintenance::getHandler, req.handler()))) {
            throw new BizException("mes.error.onlyProgressMaintenanceOrdersFinished");
        }
        MesEquipment equipment = get(m.getEquipmentId());
        if (req.cost() != null && req.cost().signum() > 0) {
            eventPublisher.publishEvent(new MaintenanceFinishedEvent(id, m.getOrderNo(), equipment.getName(), req.cost(), now.toLocalDate()));
        }
        if (m.getStartTime() != null && now.isAfter(m.getStartTime())) {
            MesEquipmentStop stop = new MesEquipmentStop();
            stop.setEquipmentId(equipment.getId());
            stop.setStartTime(m.getStartTime());
            stop.setEndTime(now);
            stop.setHours(BigDecimal.valueOf(Duration.between(m.getStartTime(), now).toSeconds()).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP));
            stop.setReason(m.getType() == MaintenanceType.REPAIR ? StopReason.BREAKDOWN.name() : StopReason.MAINTAIN.name());
            stop.setMaintenanceId(id);
            stop.setRemark(m.getContent());
            stop.setCreateBy(SecurityUtils.getUserId());
            stopMapper.insert(stop);
        }
        if (m.getType() == MaintenanceType.REPAIR) {
            // 恢复维修前的状态（闲置的仍为闲置），没有记录或原本就在维修中的恢复为运行
            EquipmentStatus prev = m.getPrevStatus();
            setEquipmentStatus(equipment.getId(), prev == null || prev == EquipmentStatus.REPAIR ? EquipmentStatus.RUNNING : prev);
        } else {
            LocalDate today = now.toLocalDate();
            equipmentMapper.update(Wrappers.<MesEquipment>lambdaUpdate()
                    .set(MesEquipment::getLastMaintainDate, today)
                    .set(MesEquipment::getNextMaintainDate, equipment.getMaintainCycleDays() == null ? null
                            : today.plusDays(equipment.getMaintainCycleDays()))
                    .eq(MesEquipment::getId, equipment.getId()));
        }
    }

    /**
     * 记录维护单生成的费用凭证，由财务模块在收到 {@link MaintenanceFinishedEvent} 并生成凭证后调用
     */
    public void bindVoucher(Long maintenanceId, Long voucherId) {
        maintenanceMapper.update(Wrappers.<MesMaintenance>lambdaUpdate().set(MesMaintenance::getVoucherId, voucherId)
                .eq(MesMaintenance::getId, maintenanceId));
    }

    /**
     * 凭证被删除时清除维护单上的凭证关联，由财务模块调用；ids 为空时不处理
     */
    public void unbindVouchers(List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return;
        }
        maintenanceMapper.update(Wrappers.<MesMaintenance>lambdaUpdate().set(MesMaintenance::getVoucherId, null)
                .in(MesMaintenance::getVoucherId, voucherIds));
    }

    /** 只能删除待处理的 */
    public void deleteMaintenance(List<Long> ids) {
        for (Long id : ids) {
            if (getMaintenance(id).getStatus() != MaintenanceStatus.PENDING) {
                throw new BizException("mes.error.onlyPendingMaintenanceOrdersDeleted");
            }
        }
        maintenanceMapper.deleteByIds(ids);
    }

    private MesMaintenance getMaintenance(Long id) {
        MesMaintenance m = maintenanceMapper.selectById(id);
        if (m == null) {
            throw new BizException("mes.error.maintenanceOrderDoesNotExist");
        }
        return m;
    }

    private void setEquipmentStatus(Long id, EquipmentStatus status) {
        equipmentMapper.update(Wrappers.<MesEquipment>lambdaUpdate()
                .set(MesEquipment::getStatus, status)
                .eq(MesEquipment::getId, id)
                .ne(MesEquipment::getStatus, EquipmentStatus.SCRAPPED));
    }

    /** 默认运行状态；有保养周期和上次保养日期时计算下次保养日期 */
    private static void normalize(MesEquipment e) {
        if (e.getStatus() == null) {
            e.setStatus(EquipmentStatus.RUNNING);
        }
        if (e.getMaintainCycleDays() == null) {
            e.setNextMaintainDate(null);
        } else {
            LocalDate base = e.getLastMaintainDate() != null ? e.getLastMaintainDate()
                    : e.getPurchaseDate() != null ? e.getPurchaseDate() : LocalDate.now();
            e.setNextMaintainDate(base.plusDays(e.getMaintainCycleDays()));
        }
    }

    private void checkCodeUnique(String code, Long id) {
        MesEquipment exist = equipmentMapper.selectOne(Wrappers.<MesEquipment>lambdaQuery().eq(MesEquipment::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("mes.error.equipmentCodeAlreadyExists");
        }
    }
}
