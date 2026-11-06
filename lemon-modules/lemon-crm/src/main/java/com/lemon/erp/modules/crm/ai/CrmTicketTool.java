package com.lemon.erp.modules.crm.ai;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.crm.entity.CrmTicket;
import com.lemon.erp.modules.crm.model.TicketPriority;
import com.lemon.erp.modules.crm.model.TicketStatus;
import com.lemon.erp.modules.crm.service.CrmAnalysisService;
import com.lemon.erp.modules.crm.service.CrmTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 服务工单：按类型统计（工单数、未解决、平均解决时长、及时率、满意度），或按条件列出工单（逾期、状态、类型），
 * 复用 {@link CrmTicketService#page} 与 {@link CrmAnalysisService#tickets}；权限与「服务工单」页面一致，不返回联系人电话和问题描述
 */
@Component
@RequiredArgsConstructor
public class CrmTicketTool implements AiTool {

    /** 查询方式：按类型统计 / 列出工单 */
    enum View {
        BY_TYPE, LIST
    }

    /** 一个类型的工单统计；avgMinutes 为平均解决时长（分钟） */
    record TypeStat(String type, Long count, Long open, BigDecimal avgMinutes, Long onTime, Long resolved, BigDecimal satisfaction) {
    }

    /** 工单（不含联系人电话、问题描述） */
    record Ticket(String ticketNo, String title, String type, TicketPriority priority, TicketStatus status, String customerName,
                  String assigneeName, LocalDateTime createTime, LocalDateTime dueTime, Boolean overdue) {
    }

    private final CrmTicketService ticketService;
    private final CrmAnalysisService analysisService;

    @Override
    public String name() {
        return "crm_service_tickets";
    }

    @Override
    public String labelKey() {
        return "crm.ai.tool.tickets";
    }

    @Override
    public String descriptionKey() {
        return "crm.ai.tool.tickets.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("view", AiSchema.enumOf("crm.ai.param.ticketView", View.LIST.name(), View.BY_TYPE.name()), false)
                .prop("status", AiSchema.enumOf("crm.ai.param.ticketStatus", TicketStatus.OPEN.name(), TicketStatus.PROCESSING.name(),
                        TicketStatus.RESOLVED.name(), TicketStatus.CLOSED.name()), false)
                .prop("overdueOnly", AiSchema.bool("crm.ai.param.overdueOnly"), false)
                .prop("type", AiSchema.string("crm.ai.param.ticketType").maxLength(32), false)
                .prop("keyword", AiSchema.string("crm.ai.param.ticketKeyword").maxLength(64), false)
                .prop("top", AiSchema.integer("crm.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "crm:ticket:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        View view = args.has("view") ? View.valueOf(args.getString("view")) : View.LIST;
        if (view == View.BY_TYPE) {
            return AiToolResult.rows(analysisService.tickets().stream()
                    .map(t -> new TypeStat(t.getType(), t.getCount(), t.getOpen(), t.getAvgMinutes(), t.getOnTime(), t.getResolved(), t.getSatisfaction()))
                    .toList());
        }
        Boolean overdue = Boolean.TRUE.equals(args.getBoolean("overdueOnly")) ? Boolean.TRUE : null;
        PageResult<CrmTicket> page = ticketService.page(1, args.getInt("top", 50), args.getString("keyword"), args.getString("type"),
                null, args.getString("status"), null, null, overdue);
        return AiToolResult.rows(page.records().stream()
                .map(t -> new Ticket(t.getTicketNo(), t.getTitle(), t.getType(), t.getPriority(), t.getStatus(), t.getCustomerName(),
                        t.getAssigneeName(), t.getCreateTime(), t.getDueTime(), t.getOverdue()))
                .toList(), page.total());
    }
}
