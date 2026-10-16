package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.system.entity.SysPrintTemplate;
import com.lemon.erp.modules.system.mapper.SysPrintTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 打印模板：只保存租户修改过的配置，配置内容由前端解释（纸张、显示哪些字段、顺序、名称、列宽、签字栏等）；
 * 也可以上传 JRXML 完全自定义版式（校验和使用见 print/PrintService）
 */
@Service
@RequiredArgsConstructor
public class SysPrintTemplateService {

    /** 单个模板配置的最大长度，防止异常数据 */
    private static final int MAX_CONFIG_LENGTH = 20_000;

    /** 只上传了 JRXML、没有修改过配置的模板 */
    private static final String EMPTY_CONFIG = "{}";

    /** all() 中标记上传了 JRXML 的模板 */
    public static final String JRXML_FLAG = "jrxml";

    private final SysPrintTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    /** 全部已修改的模板：模板标识 → 配置（上传了 JRXML 的带 jrxml: true） */
    public Map<String, Object> all() {
        Set<String> custom = new HashSet<>(templateMapper.selectJrxmlKeys());
        Map<String, Object> result = new LinkedHashMap<>();
        for (SysPrintTemplate t : templateMapper.selectList(Wrappers.emptyWrapper())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(t.getConfig(), LinkedHashMap.class);
            if (custom.contains(t.getTemplateKey())) {
                config.put(JRXML_FLAG, true);
            }
            result.put(t.getTemplateKey(), config);
        }
        return result;
    }

    public void save(String key, Map<String, Object> config) {
        checkKey(key);
        Map<String, Object> clean = new LinkedHashMap<>(config);
        clean.remove(JRXML_FLAG);
        String json = objectMapper.writeValueAsString(clean);
        if (json.length() > MAX_CONFIG_LENGTH) {
            throw new BizException("模板配置过大");
        }
        SysPrintTemplate t = find(key);
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

    /** 恢复默认版式：同时删除上传的 JRXML */
    public void reset(String key) {
        checkKey(key);
        templateMapper.delete(Wrappers.<SysPrintTemplate>lambdaQuery().eq(SysPrintTemplate::getTemplateKey, key));
    }

    /** 上传的 JRXML，没有时为 null */
    public String jrxml(String key) {
        if (key == null || !validKey(key)) {
            return null;
        }
        SysPrintTemplate t = templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery()
                .select(SysPrintTemplate::getId, SysPrintTemplate::getJrxml)
                .eq(SysPrintTemplate::getTemplateKey, key));
        return t == null ? null : t.getJrxml();
    }

    /** 保存 JRXML（调用方已校验能编译） */
    public void saveJrxml(String key, String jrxml) {
        checkKey(key);
        SysPrintTemplate t = find(key);
        if (t == null) {
            t = new SysPrintTemplate();
            t.setTemplateKey(key);
            t.setConfig(EMPTY_CONFIG);
            t.setJrxml(jrxml);
            templateMapper.insert(t);
        } else {
            t.setJrxml(jrxml);
            templateMapper.updateById(t);
        }
    }

    /** 删除 JRXML，改回按配置打印；没有修改过配置时整条删除 */
    public void removeJrxml(String key) {
        checkKey(key);
        SysPrintTemplate t = find(key);
        if (t == null) {
            return;
        }
        if (EMPTY_CONFIG.equals(t.getConfig())) {
            templateMapper.deleteById(t.getId());
        } else {
            templateMapper.update(null, Wrappers.<SysPrintTemplate>lambdaUpdate()
                    .set(SysPrintTemplate::getJrxml, null)
                    .eq(SysPrintTemplate::getId, t.getId()));
        }
    }

    private SysPrintTemplate find(String key) {
        return templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery().eq(SysPrintTemplate::getTemplateKey, key));
    }

    private static boolean validKey(String key) {
        return key.matches("^[A-Za-z_][A-Za-z0-9_.]{0,63}$");
    }

    private static void checkKey(String key) {
        if (key == null || !validKey(key)) {
            throw new BizException("模板标识不正确");
        }
    }
}
