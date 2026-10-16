package com.lemon.erp.modules.wms.service;

import com.lemon.erp.modules.wms.mapper.WmsDocSequenceMapper;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 单据号 / 主数据编码：按编码规则（前缀 + 日期 + 流水，默认如 RK202610140001）生成，规则见 {@link CodeRuleService}。
 * 在新增单据的事务中取号，流水行锁到事务结束，单据保存失败时流水号一并回滚，不会跳号。
 */
@Service
@RequiredArgsConstructor
public class DocNoService {

    private final WmsDocSequenceMapper sequenceMapper;
    private final CodeRuleService ruleService;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(StockBizType type) {
        return next(type.getPrefix());
    }

    /**
     * 各模块的单据共用同一套流水表，规则标识各不相同（如采购订单 CG、生产工单 SC），需在模块的 DocNoCatalog 中声明
     *
     * @param ruleKey 编码规则标识（默认前缀）
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String ruleKey) {
        CodeRuleService.Rule rule = ruleService.rule(ruleKey);
        LocalDate today = LocalDate.now();
        String key = rule.sequenceKey(today);
        if (sequenceMapper.increment(key) == 0) {
            try {
                sequenceMapper.insertFirst(key);
            } catch (DuplicateKeyException e) {
                // 并发时另一个事务先插入了本周期的第一条
                sequenceMapper.increment(key);
            }
        }
        return rule.format(today, sequenceMapper.selectValue(key));
    }
}
