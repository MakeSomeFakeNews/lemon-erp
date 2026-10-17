package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.hr.entity.HrContract;
import com.lemon.erp.modules.hr.mapper.HrContractMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 劳动合同：签订、续签（原合同变为已续签）、终止；到期日之后自动变为已到期；员工离职时生效中的合同自动终止
 */
@Service
@RequiredArgsConstructor
public class HrContractService {

    /** 无固定期限合同（字典 hr_contract_type 的内置值）：没有结束日期；其他类型（含租户新增的）必须填结束日期 */
    static final String OPEN_TERM = "OPEN";

    private final HrContractMapper contractMapper;
    private final HrEmployeeService employeeService;
    private final DocNoService docNoService;
    private final DictLookup dictLookup;

    public PageResult<HrContract> page(long current, long size, String keyword, String status, String type, Integer expireDays, Long employeeId) {
        Page<HrContract> page = Page.of(current, size);
        contractMapper.selectPageList(page, keyword, status, type,
                expireDays == null ? null : LocalDate.now().plusDays(expireDays), employeeId);
        page.getRecords().forEach(HrContractService::fillDaysLeft);
        return PageResult.of(page);
    }

    public HrContract detail(Long id) {
        HrContract c = contractMapper.selectDetail(id);
        if (c == null) {
            throw new BizException("hr.error.employmentContractDoesNotExist");
        }
        fillDaysLeft(c);
        return c;
    }

    @Transactional
    public Long create(HrContract req) {
        employeeService.get(req.getEmployeeId());
        HrContract c = normalize(req);
        c.setId(null);
        c.setContractNo(req.getContractNo() == null || req.getContractNo().isBlank() ? docNoService.next("HT") : req.getContractNo().trim());
        c.setStatus("ACTIVE");
        c.setPrevContractId(null);
        c.setCreateBy(SecurityUtils.getUserId());
        if (contractMapper.exists(Wrappers.<HrContract>lambdaQuery().eq(HrContract::getContractNo, c.getContractNo()))) {
            throw new BizException("hr.error.contractNoAlreadyExists");
        }
        contractMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void update(Long id, HrContract req) {
        HrContract old = detail(id);
        if (!"ACTIVE".equals(old.getStatus())) {
            throw new BizException("hr.error.onlyActiveContractsModified");
        }
        HrContract c = normalize(req);
        contractMapper.update(Wrappers.<HrContract>lambdaUpdate().set(HrContract::getContractType, c.getContractType())
                .set(HrContract::getSignDate, c.getSignDate()).set(HrContract::getStartDate, c.getStartDate())
                .set(HrContract::getEndDate, c.getEndDate()).set(HrContract::getProbationMonths, c.getProbationMonths())
                .set(HrContract::getRemark, c.getRemark()).eq(HrContract::getId, id));
    }

    /** 续签：原合同（生效中或已到期）变为已续签，新合同从原合同结束的次日开始（未指定时） */
    @Transactional
    public Long renew(Long id, HrContract req) {
        HrContract old = detail(id);
        if (!"ACTIVE".equals(old.getStatus()) && !"EXPIRED".equals(old.getStatus())) {
            throw new BizException("hr.error.onlyActiveExpiredContractsRenewed");
        }
        req.setEmployeeId(old.getEmployeeId());
        if (req.getStartDate() == null && old.getEndDate() != null) {
            req.setStartDate(old.getEndDate().plusDays(1));
        }
        if (req.getContractType() == null) {
            req.setContractType(old.getContractType());
        }
        Long newId = create(req);
        contractMapper.update(Wrappers.<HrContract>lambdaUpdate().set(HrContract::getStatus, "RENEWED").eq(HrContract::getId, id));
        contractMapper.update(Wrappers.<HrContract>lambdaUpdate().set(HrContract::getPrevContractId, id).eq(HrContract::getId, newId));
        return newId;
    }

    @Transactional
    public void terminate(Long id, LocalDate date, String reason) {
        HrContract c = detail(id);
        if (!"ACTIVE".equals(c.getStatus())) {
            throw new BizException("hr.error.onlyActiveContractsTerminated");
        }
        contractMapper.update(Wrappers.<HrContract>lambdaUpdate().set(HrContract::getStatus, "TERMINATED")
                .set(HrContract::getTerminateDate, date == null ? LocalDate.now() : date).set(HrContract::getTerminateReason, reason)
                .eq(HrContract::getId, id));
    }

    @Transactional
    public void delete(Long id) {
        HrContract c = detail(id);
        if (!"ACTIVE".equals(c.getStatus())) {
            throw new BizException("hr.error.onlyActiveContractsDeletedEntryErrors");
        }
        contractMapper.deleteById(id);
    }

    /** 员工离职：生效中的合同终止 */
    @EventListener
    public void onEmployeeLeft(EmployeeLeftEvent e) {
        contractMapper.update(Wrappers.<HrContract>lambdaUpdate().set(HrContract::getStatus, "TERMINATED")
                .set(HrContract::getTerminateDate, e.leaveDate()).set(HrContract::getTerminateReason, "员工离职")  // i18n-ignore 终止原因存库
                .eq(HrContract::getEmployeeId, e.employeeId()).eq(HrContract::getStatus, "ACTIVE"));
    }

    /** 到期日已过的生效中合同改为已到期，返回更新数 */
    public int markExpired() {
        return contractMapper.update(Wrappers.<HrContract>lambdaUpdate().set(HrContract::getStatus, "EXPIRED")
                .eq(HrContract::getStatus, "ACTIVE").isNotNull(HrContract::getEndDate).lt(HrContract::getEndDate, LocalDate.now()));
    }

    public List<HrContract> expiring(int days) {
        List<HrContract> list = contractMapper.selectExpiring(LocalDate.now().plusDays(days));
        list.forEach(HrContractService::fillDaysLeft);
        return list;
    }

    private HrContract normalize(HrContract req) {
        if (req.getContractType() == null || req.getContractType().isBlank()) {
            throw new BizException("hr.error.selectContractType");
        }
        dictLookup.check("hr_contract_type", req.getContractType(), "hr.label.contractType");
        if (req.getStartDate() == null) {
            throw new BizException("hr.error.selectContractStartDate");
        }
        if (OPEN_TERM.equals(req.getContractType())) {
            req.setEndDate(null);
        } else if (req.getEndDate() == null) {
            throw new BizException("hr.error.selectContractEndDateExceptOpen");
        }
        if (req.getEndDate() != null && req.getEndDate().isBefore(req.getStartDate())) {
            throw new BizException("hr.error.contractEndDateCannotEarlierStart");
        }
        if (req.getSignDate() == null) {
            req.setSignDate(req.getStartDate());
        }
        if (req.getProbationMonths() == null) {
            req.setProbationMonths(0);
        }
        if (req.getProbationMonths() < 0 || req.getProbationMonths() > 6) {
            throw new BizException("hr.error.probationMustMonths");
        }
        return req;
    }

    private static void fillDaysLeft(HrContract c) {
        c.setDaysLeft(c.getEndDate() == null ? null : (int) ChronoUnit.DAYS.between(LocalDate.now(), c.getEndDate()));
    }
}
