package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.system.entity.SysPrintTemplate;
import com.lemon.erp.modules.system.mapper.SysPrintTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 打印模板：只保存租户修改过的配置，配置内容由前端解释（显示哪些字段、顺序、名称、列宽、签字栏等）
 */
@Service
@RequiredArgsConstructor
public class SysPrintTemplateService {

    /** 单个模板配置的最大长度，防止异常数据 */
    private static final int MAX_CONFIG_LENGTH = 20_000;

    private final SysPrintTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    /** 全部已修改的模板：模板标识 → 配置 */
    public Map<String, Object> all() {
        Map<String, Object> result = new LinkedHashMap<>();
        templateMapper.selectList(Wrappers.emptyWrapper())
                .forEach(t -> result.put(t.getTemplateKey(), objectMapper.readValue(t.getConfig(), Object.class)));
        return result;
    }

    public void save(String key, Map<String, Object> config) {
        checkKey(key);
        String json = objectMapper.writeValueAsString(config);
        if (json.length() > MAX_CONFIG_LENGTH) {
            throw new BizException("模板配置过大");
        }
        SysPrintTemplate t = templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery().eq(SysPrintTemplate::getTemplateKey, key));
        if (t == null) {
            t = new SysPrintTemplate();
            t.setTemplateKey(key);
            t.setConfig(json);
            templateMapper.insert(t);
        } else {
            t.setConfig(json);
            templateMapper.updateById(t);
        }
    }

    /** 恢复默认版式 */
    public void reset(String key) {
        checkKey(key);
        templateMapper.delete(Wrappers.<SysPrintTemplate>lambdaQuery().eq(SysPrintTemplate::getTemplateKey, key));
    }

    private static void checkKey(String key) {
        if (key == null || !key.matches("^[A-Za-z_][A-Za-z0-9_.]{0,63}$")) {
            throw new BizException("模板标识不正确");
        }
    }
}
