package com.lemon.erp.modules.crm.service;

import com.lemon.erp.framework.org.EmployeeLeftEvent;
import com.lemon.erp.modules.crm.mapper.CrmHandoverMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 离职交接：负责的客户移交给交接人（没有交接人时退回公海），线索、商机、服务工单的负责人改为交接人
 */
@Service
@RequiredArgsConstructor
public class CrmHandoverService {

    private final CrmHandoverMapper handoverMapper;

    @EventListener
    public void onEmployeeLeft(EmployeeLeftEvent e) {
        if (e.userId() == null) {
            return;
        }
        Long to = e.handoverUserId();
        handoverMapper.handoverCustomers(e.userId(), to);
        if (to == null) {
            return;
        }
        handoverMapper.handoverLeads(e.userId(), to);
        handoverMapper.handoverOpportunities(e.userId(), to);
        handoverMapper.handoverTickets(e.userId(), to);
    }
}
