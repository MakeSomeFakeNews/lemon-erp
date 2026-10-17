package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.code.DocNoCatalog;
import com.lemon.erp.framework.code.DocNoCatalog.DateFormat;
import com.lemon.erp.framework.code.DocNoCatalog.ResetCycle;
import com.lemon.erp.modules.wms.dto.CodeRuleRequest;
import com.lemon.erp.modules.wms.dto.CodeRuleView;
import com.lemon.erp.modules.wms.entity.CodeRule;
import com.lemon.erp.modules.wms.mapper.CodeRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 编码规则：各模块声明的单据编号类型（DocNoCatalog）+ 租户修改过的规则。修改只影响之后的新编号
 */
@Service
@RequiredArgsConstructor
public class CodeRuleService {

    private final CodeRuleMapper ruleMapper;
    private final List<DocNoCatalog> catalogs;

    /** 生效中的规则 */
    public record Rule(String key, String prefix, DateFormat dateFormat, int seqLength, ResetCycle resetCycle) {

        /** 按日期和流水号生成编号 */
        public String format(LocalDate date, long seq) {
            String datePart = dateFormat.pattern().isEmpty() ? "" : date.format(DateTimeFormatter.ofPattern(dateFormat.pattern()));
            return prefix + datePart + String.format("%0" + seqLength + "d", seq);
        }

        /**
         * 流水的计数键：按重置周期区分。按天重置时为「前缀 + yyyyMMdd」（与历史格式一致，已有流水接着使用）
         */
        public String sequenceKey(LocalDate date) {
            return switch (resetCycle) {
                case DAY -> prefix + date.format(DateTimeFormatter.BASIC_ISO_DATE);
                case MONTH -> prefix + "@M" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
                case YEAR -> prefix + "@Y" + date.getYear();
                case NEVER -> prefix + "@N";
            };
        }
    }

    public List<CodeRuleView> list() {
        Map<String, CodeRule> custom = customRules();
        return catalogs.stream().flatMap(c -> c.types().stream().map(t -> {
            CodeRule r = custom.get(t.key());
            Rule rule = r == null ? defaultRule(t) : toRule(r);
            return new CodeRuleView(t.key(), I18n.t(t.name()), I18n.t(c.module()), rule.prefix(), rule.dateFormat(), rule.seqLength(),
                    rule.resetCycle(), r != null, rule.format(LocalDate.now(), 1));
        })).toList();
    }

    /** 取号时使用的规则 */
    public Rule rule(String key) {
        CodeRule r = ruleMapper.selectOne(Wrappers.<CodeRule>lambdaQuery().eq(CodeRule::getRuleKey, key));
        if (r != null) {
            return toRule(r);
        }
        DocNoCatalog.Type type = types().get(key);
        // 未在目录中声明的键按单据默认规则处理
        return type == null ? new Rule(key, key, DateFormat.YYYYMMDD, 4, ResetCycle.DAY) : defaultRule(type);
    }

    public void update(String key, CodeRuleRequest req) {
        DocNoCatalog.Type type = requireType(key);
        if (!req.dateFormat().supports(req.resetCycle())) {
            throw new BizException("日期部分不够精确：流水按「{0}」重置时，日期格式至少要精确到{1}，否则编号会重复",
                    I18n.t(cycleLabel(req.resetCycle())), I18n.t(cycleUnit(req.resetCycle())));
        }
        list().stream()
                .filter(v -> !v.key().equals(key) && v.prefix().equals(req.prefix()))
                .findFirst()
                .ifPresent(v -> {
                    throw new BizException("前缀「{0}」已被「{1}」使用", req.prefix(), v.name());
                });
        CodeRule rule = ruleMapper.selectOne(Wrappers.<CodeRule>lambdaQuery().eq(CodeRule::getRuleKey, key));
        if (rule == null) {
            rule = new CodeRule();
            rule.setRuleKey(type.key());
        }
        rule.setPrefix(req.prefix());
        rule.setDateFormat(req.dateFormat());
        rule.setSeqLength(req.seqLength());
        rule.setResetCycle(req.resetCycle());
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
    }

    /** 恢复默认规则 */
    public void reset(String key) {
        requireType(key);
        ruleMapper.delete(Wrappers.<CodeRule>lambdaQuery().eq(CodeRule::getRuleKey, key));
    }

    private Map<String, CodeRule> customRules() {
        return ruleMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(CodeRule::getRuleKey, Function.identity()));
    }

    private Map<String, DocNoCatalog.Type> types() {
        Map<String, DocNoCatalog.Type> map = new LinkedHashMap<>();
        catalogs.forEach(c -> c.types().forEach(t -> map.put(t.key(), t)));
        return map;
    }

    private DocNoCatalog.Type requireType(String key) {
        DocNoCatalog.Type type = types().get(key);
        if (type == null) {
            throw new BizException("编码规则不存在");
        }
        return type;
    }

    private static Rule defaultRule(DocNoCatalog.Type t) {
        return new Rule(t.key(), t.key(), t.dateFormat(), t.seqLength(), t.resetCycle());
    }

    private static Rule toRule(CodeRule r) {
        return new Rule(r.getRuleKey(), r.getPrefix(), r.getDateFormat(), r.getSeqLength(), r.getResetCycle());
    }

    private static String cycleLabel(ResetCycle cycle) {
        return switch (cycle) {
            case DAY -> "每天";
            case MONTH -> "每月";
            case YEAR -> "每年";
            case NEVER -> "不重置";
        };
    }

    /** 重置周期对应的日期精度（提示用） */
    private static String cycleUnit(ResetCycle cycle) {
        return switch (cycle) {
            case DAY -> "天";
            case MONTH -> "月";
            case YEAR -> "年";
            case NEVER -> "不重置";
        };
    }
}
