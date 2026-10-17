package com.lemon.erp.modules.hr.service;

import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.event.VoucherDeletedEvent;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import com.lemon.erp.modules.hr.dto.TrainingRecordRequest;
import com.lemon.erp.modules.hr.entity.HrTraining;
import com.lemon.erp.modules.hr.entity.HrTrainingRecord;
import com.lemon.erp.modules.hr.mapper.HrTrainingMapper;
import com.lemon.erp.modules.hr.mapper.HrTrainingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 培训：计划中 → 进行中 → 已结束；计划中、进行中可以取消。结束前录入出勤和成绩，员工自助可以查看自己的培训记录和学时
 */
@Service
@RequiredArgsConstructor
public class HrTrainingService {

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "ONGOING", Set.of("PLANNED"),
            "FINISHED", Set.of("ONGOING"),
            "CANCELED", Set.of("PLANNED", "ONGOING"));

    private final HrTrainingMapper trainingMapper;
    private final HrTrainingRecordMapper recordMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;

    public PageResult<HrTraining> page(long current, long size, String keyword, String category, String status, Integer year) {
        Page<HrTraining> page = Page.of(current, size);
        trainingMapper.selectPageList(page, keyword, category, status,
                year == null ? null : LocalDate.of(year, 1, 1), year == null ? null : LocalDate.of(year, 12, 31));
        return PageResult.of(page);
    }

    public HrTraining get(Long id) {
        HrTraining t = id == null ? null : trainingMapper.selectById(id);
        if (t == null) {
            throw new BizException("hr.error.trainingDoesNotExist");
        }
        return t;
    }

    public List<HrTrainingRecord> records(Long trainingId) {
        get(trainingId);
        return recordMapper.selectByTraining(trainingId);
    }

    public void create(HrTraining t) {
        check(t);
        t.setId(null);
        t.setStatus("PLANNED");
        t.setCreateBy(SecurityUtils.getUserId());
        trainingMapper.insert(t);
    }

    public void update(Long id, HrTraining t) {
        HrTraining old = get(id);
        if (!"PLANNED".equals(old.getStatus()) && !"ONGOING".equals(old.getStatus())) {
            throw new BizException("hr.error.finishedCanceledTrainingCannotModified");
        }
        check(t);
        t.setId(id);
        t.setStatus(null);
        t.setCreateBy(null);
        trainingMapper.updateById(t);
    }

    /** 只能删除计划中或已取消的培训 */
    @Transactional
    public void delete(List<Long> ids) {
        for (HrTraining t : trainingMapper.selectByIds(ids)) {
            if (!"PLANNED".equals(t.getStatus()) && !"CANCELED".equals(t.getStatus())) {
                throw new BizException("hr.error.trainingHasStartedCannotDeleted", t.getTitle());
            }
        }
        recordMapper.delete(Wrappers.<HrTrainingRecord>lambdaQuery().in(HrTrainingRecord::getTrainingId, ids));
        trainingMapper.deleteByIds(ids);
    }

    /** 状态流转：开始、结束、取消 */
    @Transactional
    public void changeStatus(Long id, String status) {
        HrTraining t = get(id);
        Set<String> from = TRANSITIONS.get(status);
        if (from == null || !from.contains(t.getStatus())) {
            throw new BizException("hr.error.operationNotAllowedCurrentTrainingStatus");
        }
        trainingMapper.update(Wrappers.<HrTraining>lambdaUpdate().set(HrTraining::getStatus, status)
                .set(HrTraining::getUpdateTime, LocalDateTime.now())
                .eq(HrTraining::getId, id).eq(HrTraining::getStatus, t.getStatus()));
        // 培训结束：有费用时生成培训费凭证（借 管理费用-培训费 / 贷 其他应付款）
        if ("FINISHED".equals(status) && t.getCost() != null && t.getCost().signum() > 0 && t.getVoucherId() == null) {
            String summary = "培训费 " + t.getTitle();  // i18n-ignore 凭证摘要存库
            LocalDate date = t.getEndDate() != null ? t.getEndDate() : LocalDate.now();
            Long voucherId = voucherService.createGenerated(date, VoucherSource.EXPENSE, null, List.of(
                    VoucherLine.debit(summary, accountRules.accountId(AccountRole.TRAINING_EXPENSE), null, t.getCost()),
                    VoucherLine.credit(summary, accountRules.accountId(AccountRole.OTHER_PAYABLE), null, t.getCost())));
            trainingMapper.update(Wrappers.<HrTraining>lambdaUpdate().set(HrTraining::getVoucherId, voucherId).eq(HrTraining::getId, id));
        }
    }

    @EventListener
    public void onVoucherDeleted(VoucherDeletedEvent e) {
        trainingMapper.update(Wrappers.<HrTraining>lambdaUpdate().set(HrTraining::getVoucherId, null)
                .in(HrTraining::getVoucherId, e.voucherIds()));
    }

    /** 添加学员（已在名单中的跳过） */
    @Transactional
    public int addTrainees(Long id, List<Long> employeeIds) {
        checkEditable(get(id));
        Set<Long> exist = recordMapper.selectList(Wrappers.<HrTrainingRecord>lambdaQuery()
                        .select(HrTrainingRecord::getEmployeeId).eq(HrTrainingRecord::getTrainingId, id))
                .stream().map(HrTrainingRecord::getEmployeeId).collect(Collectors.toSet());
        Set<Long> toAdd = new HashSet<>(employeeIds);
        toAdd.removeAll(exist);
        if (!toAdd.isEmpty()) {
            recordMapper.insertBatch(id, toAdd);
        }
        return toAdd.size();
    }

    @Transactional
    public void removeTrainees(Long id, List<Long> recordIds) {
        checkEditable(get(id));
        recordMapper.delete(Wrappers.<HrTrainingRecord>lambdaQuery()
                .eq(HrTrainingRecord::getTrainingId, id).in(HrTrainingRecord::getId, recordIds));
    }

    /** 录入出勤与成绩：进行中、已结束的培训 */
    @Transactional
    public void saveRecords(Long id, List<TrainingRecordRequest> items) {
        HrTraining t = get(id);
        if (!"ONGOING".equals(t.getStatus()) && !"FINISHED".equals(t.getStatus())) {
            throw new BizException("hr.error.attendanceScoresOnlyRecordedAfterTraining");
        }
        Map<Long, HrTrainingRecord> records = recordMapper.selectList(Wrappers.<HrTrainingRecord>lambdaQuery()
                        .eq(HrTrainingRecord::getTrainingId, id))
                .stream().collect(Collectors.toMap(HrTrainingRecord::getId, Function.identity()));
        for (TrainingRecordRequest req : items) {
            if (!records.containsKey(req.id())) {
                throw new BizException("hr.error.attendanceRecordDoesNotBelongTraining");
            }
            boolean attended = Integer.valueOf(1).equals(req.attended());
            recordMapper.update(Wrappers.<HrTrainingRecord>lambdaUpdate()
                    .set(HrTrainingRecord::getAttended, attended ? 1 : 0)
                    .set(HrTrainingRecord::getScore, attended ? req.score() : null)
                    .set(HrTrainingRecord::getPassed, attended ? req.passed() : Integer.valueOf(0))
                    .set(HrTrainingRecord::getFeedback, req.feedback())
                    .set(HrTrainingRecord::getUpdateTime, LocalDateTime.now())
                    .eq(HrTrainingRecord::getId, req.id()));
        }
    }

    public List<HrTrainingRecord> employeeRecords(Long employeeId) {
        return recordMapper.selectByEmployee(employeeId);
    }

    /** 本年已完成的培训学时 */
    public BigDecimal yearHours(Long employeeId, int year) {
        return recordMapper.sumHours(employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private static void check(HrTraining t) {
        if (t.getEndDate().isBefore(t.getStartDate())) {
            throw new BizException("hr.error.endDateCannotEarlierStartDate");
        }
        if (t.getHours() == null) t.setHours(BigDecimal.ZERO);
        if (t.getCost() == null) t.setCost(BigDecimal.ZERO);
    }

    private static void checkEditable(HrTraining t) {
        if (!"PLANNED".equals(t.getStatus()) && !"ONGOING".equals(t.getStatus())) {
            throw new BizException("hr.error.traineesCannotChangedFinishedCanceledTraining");
        }
    }
}
