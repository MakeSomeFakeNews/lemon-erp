package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.fin.event.VoucherDeletedEvent;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.mes.entity.MesMaintenance;
import com.lemon.erp.modules.mes.mapper.MesMaintenanceMapper;
import com.lemon.erp.modules.mes.model.MaintenanceFinishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务费用生成凭证（未审核）：设备维修保养费 借 制造费用 / 贷 其他应付款（报销、付款时冲销）
 */
@Service
@RequiredArgsConstructor
public class FinExpenseService {

    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final MesMaintenanceMapper maintenanceMapper;

    @EventListener
    public void onMaintenanceFinished(MaintenanceFinishedEvent e) {
        String summary = "设备维护费 " + e.orderNo() + " " + e.equipmentName(); // i18n-ignore
        Long voucherId = voucherService.createGenerated(e.date(), VoucherSource.EXPENSE, e.orderNo(), List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.MANUFACTURING_OVERHEAD), null, e.cost()),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.OTHER_PAYABLE), null, e.cost())));
        maintenanceMapper.update(Wrappers.<MesMaintenance>lambdaUpdate().set(MesMaintenance::getVoucherId, voucherId)
                .eq(MesMaintenance::getId, e.maintenanceId()));
    }

    @EventListener
    public void onVoucherDeleted(VoucherDeletedEvent e) {
        maintenanceMapper.update(Wrappers.<MesMaintenance>lambdaUpdate().set(MesMaintenance::getVoucherId, null)
                .in(MesMaintenance::getVoucherId, e.voucherIds()));
    }
}
