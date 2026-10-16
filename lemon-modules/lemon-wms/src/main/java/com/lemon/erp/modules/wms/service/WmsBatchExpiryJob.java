package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.framework.message.MessageSender.Message;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.wms.mapper.WmsJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 批次临期提醒：days 天内（默认 30）到期且有库存的批次，提醒仓库人员（wms:stock:list，每个批次一次）
 */
@Component
@RequiredArgsConstructor
public class WmsBatchExpiryJob implements JobHandler {

    private final WmsJobMapper jobMapper;
    private final MessageSender messageSender;

    @Override
    public String key() {
        return "wms.batchExpiry";
    }

    @Override
    public String name() {
        return "批次临期提醒";
    }

    @Override
    public String defaultCron() {
        return "0 20 8 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=30";
    }

    @Override
    public String run(JobParams params) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = jobMapper.selectExpiringBatches(today.plusDays(params.getInt("days", 30)));
        for (Map<String, Object> r : rows) {
            boolean expired = LocalDate.parse(String.valueOf(r.get("expiryDate")).substring(0, 10)).isBefore(today);
            messageSender.sendToPermission("wms:stock:list", Message.of(expired ? "ALERT" : "REMIND",
                    (expired ? "批次已过期：" : "批次即将到期：") + r.get("code") + " " + r.get("name") + " 批次 " + r.get("batchNo"),
                    "有效期至 " + r.get("expiryDate") + "，库存 " + r.get("qty"), "/wms/trace")
                    .dedup("batch-expiry:" + r.get("materialId") + ":" + r.get("batchNo") + (expired ? ":expired" : "")));
        }
        return "提醒 " + rows.size() + " 个批次";
    }
}
