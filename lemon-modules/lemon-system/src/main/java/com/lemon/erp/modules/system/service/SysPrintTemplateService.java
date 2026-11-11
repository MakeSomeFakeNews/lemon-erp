package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.system.dto.PrintFormatRequest;
import com.lemon.erp.modules.system.dto.PrintFormatVO;
import com.lemon.erp.modules.system.entity.SysPrintTemplate;
import com.lemon.erp.modules.system.mapper.SysPrintTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 打印模板：只保存租户修改过的配置，配置内容由前端解释（纸张、显示哪些字段、顺序、名称、列宽、签字栏等）；
 * 也可以上传 JRXML 完全自定义版式（校验和使用见 print/PrintService）。
 * <p>
 * 打印格式：同一模板标识（单据）可以有多份格式，每份有自己的配置、JRXML 和套打底图，其中有且只有一份默认格式。
 * 没有任何记录时按程序内置的默认版式（相当于一份「标准格式」）；新增第一份格式时先把标准格式保存为默认格式。
 * 按模板标识读写（不传格式 ID）的接口操作默认格式，标签、通用设置只有这一份
 */
@Service
@RequiredArgsConstructor
public class SysPrintTemplateService {

    /** 单个模板配置的最大长度，防止异常数据 */
    private static final int MAX_CONFIG_LENGTH = 20_000;

    /** 套打底图 data URL 的最大长度（约 2MB 图片 base64 后） */
    private static final int MAX_BACKGROUND_LENGTH = 3 * 1024 * 1024;

    /** 每个单据最多的打印格式数 */
    static final int MAX_FORMATS = 20;

    /** 只上传了 JRXML、没有修改过配置的模板 */
    private static final String EMPTY_CONFIG = "{}";

    /** all() 中标记上传了 JRXML 的模板 */
    public static final String JRXML_FLAG = "jrxml";

    /** 通用设置（公司名称等），只有一份 */
    public static final String COMMON_KEY = "_common";

    /** 标签模板（前端生成 HTML 打印），只有一份 */
    private static final String LABEL_PREFIX = "label.";

    private final SysPrintTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    /** 全部已修改的模板（各单据的默认格式）：模板标识 → 配置（上传了 JRXML 的带 jrxml: true） */
    public Map<String, Object> all() {
        Set<Long> custom = new HashSet<>(templateMapper.selectJrxmlIds());
        Map<String, Object> result = new LinkedHashMap<>();
        for (SysPrintTemplate t : templateMapper.selectList(Wrappers.<SysPrintTemplate>lambdaQuery()
                .eq(SysPrintTemplate::getIsDefault, true))) {
            Map<String, Object> config = parse(t.getConfig());
            if (custom.contains(t.getId())) {
                config.put(JRXML_FLAG, true);
            }
            result.put(t.getTemplateKey(), config);
        }
        return result;
    }

    /** 全部单据的打印格式（不含标签、通用设置），按模板标识、排序 */
    public List<PrintFormatVO> formats() {
        Set<Long> custom = new HashSet<>(templateMapper.selectJrxmlIds());
        return templateMapper.selectList(Wrappers.<SysPrintTemplate>lambdaQuery()
                        .ne(SysPrintTemplate::getTemplateKey, COMMON_KEY)
                        .notLikeRight(SysPrintTemplate::getTemplateKey, LABEL_PREFIX)
                        .orderByAsc(SysPrintTemplate::getTemplateKey)
                        .orderByAsc(SysPrintTemplate::getSort)
                        .orderByAsc(SysPrintTemplate::getId))
                .stream()
                .map(t -> new PrintFormatVO(t.getId(), t.getTemplateKey(), t.getName(), t.getIsDefault(), t.getSort(),
                        t.getStatus(), custom.contains(t.getId()), parse(t.getConfig())))
                .toList();
    }

    /** 保存配置：formatId 为空时保存默认格式（没有时新建标准格式） */
    public void save(String key, Long formatId, Map<String, Object> config) {
        checkKey(key);
        Map<String, Object> clean = new LinkedHashMap<>(config);
        clean.remove(JRXML_FLAG);
        String json = objectMapper.writeValueAsString(clean);
        if (json.length() > MAX_CONFIG_LENGTH) {
            throw new BizException("system.error.templateConfigurationTooLarge");
        }
        SysPrintTemplate t = target(key, formatId);
        if (t == null) {
            templateMapper.insert(standard(key, json));
        } else {
            updateColumns(t.getId(), json, false, null, false, null);
        }
    }

    /** 删除该模板标识的全部格式，恢复程序内置的默认版式（同时删除上传的 JRXML） */
    public void reset(String key) {
        checkKey(key);
        templateMapper.delete(Wrappers.<SysPrintTemplate>lambdaQuery().eq(SysPrintTemplate::getTemplateKey, key));
    }

    /** 某个格式恢复默认版式：清空配置、上传的 JRXML 和套打底图，名称、默认、排序、状态不变 */
    public void reset(String key, Long formatId) {
        checkKey(key);
        if (formatId == null) {
            reset(key);
            return;
        }
        SysPrintTemplate t = requireFormat(key, formatId);
        updateColumns(t.getId(), EMPTY_CONFIG, true, null, true, null);
    }

    /** 上传的 JRXML，没有时为 null；formatId 为空时取默认格式，不属于该模板标识（或其他租户）的格式报错 */
    public String jrxml(String key, Long formatId) {
        if (key == null || !validKey(key)) {
            if (formatId != null) {
                throw new BizException("system.error.printFormatNotFound");
            }
            return null;
        }
        SysPrintTemplate t = templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery()
                .select(SysPrintTemplate::getId, SysPrintTemplate::getTemplateKey, SysPrintTemplate::getJrxml)
                .eq(formatId != null, SysPrintTemplate::getId, formatId)
                .eq(formatId == null, SysPrintTemplate::getTemplateKey, key)
                .eq(formatId == null, SysPrintTemplate::getIsDefault, true));
        if (formatId != null && (t == null || !key.equals(t.getTemplateKey()))) {
            throw new BizException("system.error.printFormatNotFound");
        }
        return t == null ? null : t.getJrxml();
    }

    /** 保存 JRXML（调用方已校验能编译）；formatId 为空时保存到默认格式（没有时新建标准格式） */
    public void saveJrxml(String key, Long formatId, String jrxml) {
        checkKey(key);
        SysPrintTemplate t = target(key, formatId);
        if (t == null) {
            t = standard(key, EMPTY_CONFIG);
            t.setJrxml(jrxml);
            templateMapper.insert(t);
        } else {
            updateColumns(t.getId(), null, true, jrxml, false, null);
        }
    }

    /** 删除 JRXML，改回按配置打印；该单据只有这一份格式且没有修改过配置时整条删除（恢复为未修改） */
    public void removeJrxml(String key, Long formatId) {
        checkKey(key);
        SysPrintTemplate t = target(key, formatId);
        if (t == null) {
            return;
        }
        if (EMPTY_CONFIG.equals(t.getConfig()) && count(key) == 1) {
            templateMapper.deleteById(t.getId());
        } else {
            updateColumns(t.getId(), null, true, null, false, null);
        }
    }

    /** 套打底图（data URL），没有时为 null；formatId 为空时取默认格式 */
    public String background(String key, Long formatId) {
        checkKey(key);
        SysPrintTemplate t = templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery()
                .select(SysPrintTemplate::getId, SysPrintTemplate::getTemplateKey, SysPrintTemplate::getBackground)
                .eq(formatId != null, SysPrintTemplate::getId, formatId)
                .eq(formatId == null, SysPrintTemplate::getTemplateKey, key)
                .eq(formatId == null, SysPrintTemplate::getIsDefault, true));
        if (formatId != null && (t == null || !key.equals(t.getTemplateKey()))) {
            throw new BizException("system.error.printFormatNotFound");
        }
        return t == null ? null : t.getBackground();
    }

    /** 保存套打底图：PNG / JPEG 的 data URL，不超过 3MB；为空时删除。formatId 为空时保存到默认格式 */
    public void saveBackground(String key, Long formatId, String dataUrl) {
        checkKey(key);
        if (dataUrl != null && !dataUrl.isBlank()) {
            if (!dataUrl.startsWith("data:image/png;base64,") && !dataUrl.startsWith("data:image/jpeg;base64,")) {
                throw new BizException("system.error.onlyPngJpgImagesSupportedBackground");
            }
            if (dataUrl.length() > MAX_BACKGROUND_LENGTH) {
                throw new BizException("system.error.backgroundImageCannotExceedMb");
            }
        }
        String value = dataUrl == null || dataUrl.isBlank() ? null : dataUrl;
        SysPrintTemplate t = target(key, formatId);
        if (t == null) {
            if (value == null) {
                return;
            }
            t = standard(key, EMPTY_CONFIG);
            t.setBackground(value);
            templateMapper.insert(t);
            return;
        }
        updateColumns(t.getId(), null, false, null, true, value);
    }

    // ---------------------------------------------------------------- 打印格式

    /**
     * 新增打印格式：可从同一单据的已有格式复制（配置、JRXML、套打底图）。该单据还没有保存过格式时，
     * 先把程序内置的默认版式保存为默认的「标准格式」，新格式设为默认时原默认格式取消
     */
    @Transactional
    public Long createFormat(PrintFormatRequest req) {
        String key = req.templateKey();
        checkKey(key);
        if (COMMON_KEY.equals(key) || key.startsWith(LABEL_PREFIX)) {
            throw new BizException("system.error.printFormatNotSupported");
        }
        List<Long> ids = templateMapper.lockByKey(key);
        if (ids.size() >= MAX_FORMATS) {
            throw new BizException("system.error.printFormatTooMany", MAX_FORMATS);
        }
        SysPrintTemplate source = req.sourceId() == null ? null : templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery()
                .select(SysPrintTemplate::getId, SysPrintTemplate::getTemplateKey, SysPrintTemplate::getConfig,
                        SysPrintTemplate::getJrxml, SysPrintTemplate::getBackground)
                .eq(SysPrintTemplate::getId, req.sourceId()));
        if (req.sourceId() != null && (source == null || !key.equals(source.getTemplateKey()))) {
            throw new BizException("system.error.printFormatNotFound");
        }
        if (ids.isEmpty()) {
            templateMapper.insert(standard(key, EMPTY_CONFIG));
        }
        String name = req.name().trim();
        checkNameUnique(key, name, null);
        EnableStatus status = req.status() == null ? EnableStatus.ENABLED : req.status();
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault());
        if (makeDefault && status.isDisabled()) {
            throw new BizException("system.error.printFormatDefaultCannotDisable");
        }
        SysPrintTemplate t = new SysPrintTemplate();
        t.setTemplateKey(key);
        t.setName(name);
        t.setIsDefault(false);
        t.setSort(req.sort() != null ? req.sort() : ids.isEmpty() ? 1 : ids.size());
        t.setStatus(status);
        t.setConfig(source == null ? EMPTY_CONFIG : source.getConfig());
        if (source != null) {
            t.setJrxml(source.getJrxml());
            t.setBackground(source.getBackground());
        }
        templateMapper.insert(t);
        if (makeDefault) {
            makeDefault(key, t.getId());
        }
        return t.getId();
    }

    /** 修改打印格式的名称、排序、状态，isDefault 为 true 时设为默认格式（默认格式不能停用） */
    @Transactional
    public void updateFormat(Long id, PrintFormatRequest req) {
        SysPrintTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("system.error.printFormatNotFound");
        }
        templateMapper.lockByKey(t.getTemplateKey());
        String name = req.name().trim();
        checkNameUnique(t.getTemplateKey(), name, id);
        EnableStatus status = req.status() == null ? t.getStatus() : req.status();
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault()) && !Boolean.TRUE.equals(t.getIsDefault());
        if ((Boolean.TRUE.equals(t.getIsDefault()) || makeDefault) && status.isDisabled()) {
            throw new BizException("system.error.printFormatDefaultCannotDisable");
        }
        templateMapper.update(null, Wrappers.<SysPrintTemplate>lambdaUpdate()
                .set(SysPrintTemplate::getName, name)
                .set(req.sort() != null, SysPrintTemplate::getSort, req.sort())
                .set(SysPrintTemplate::getStatus, status)
                .eq(SysPrintTemplate::getId, id));
        if (makeDefault) {
            makeDefault(t.getTemplateKey(), id);
        }
    }

    /** 删除打印格式：还有其他格式时默认格式不能删除（先把其他格式设为默认）；删除唯一的格式即恢复默认版式 */
    @Transactional
    public void deleteFormat(Long id) {
        SysPrintTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("system.error.printFormatNotFound");
        }
        List<Long> ids = templateMapper.lockByKey(t.getTemplateKey());
        if (Boolean.TRUE.equals(t.getIsDefault()) && ids.size() > 1) {
            throw new BizException("system.error.printFormatDefaultCannotDelete");
        }
        templateMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 内部

    /** 设为默认：同一模板标识的其他格式取消默认（调用方已锁定该模板标识） */
    private void makeDefault(String key, Long id) {
        templateMapper.update(null, Wrappers.<SysPrintTemplate>lambdaUpdate()
                .set(SysPrintTemplate::getIsDefault, false)
                .eq(SysPrintTemplate::getTemplateKey, key)
                .ne(SysPrintTemplate::getId, id));
        templateMapper.update(null, Wrappers.<SysPrintTemplate>lambdaUpdate()
                .set(SysPrintTemplate::getIsDefault, true)
                .eq(SysPrintTemplate::getId, id));
    }

    private void checkNameUnique(String key, String name, Long excludeId) {
        boolean exists = templateMapper.exists(Wrappers.<SysPrintTemplate>lambdaQuery()
                .eq(SysPrintTemplate::getTemplateKey, key)
                .eq(SysPrintTemplate::getName, name)
                .ne(excludeId != null, SysPrintTemplate::getId, excludeId));
        if (exists) {
            throw new BizException("system.error.printFormatNameExists", name);
        }
    }

    /** 默认的标准格式（名称按默认语言存库） */
    private static SysPrintTemplate standard(String key, String config) {
        SysPrintTemplate t = new SysPrintTemplate();
        t.setTemplateKey(key);
        t.setName(Messages.get(Messages.DEFAULT_LOCALE, "system.text.printFormatStandard"));
        t.setIsDefault(true);
        t.setSort(0);
        t.setStatus(EnableStatus.ENABLED);
        t.setConfig(config);
        return t;
    }

    /** 要操作的格式：formatId 为空时为默认格式（可能还没有），否则必须属于该模板标识 */
    private SysPrintTemplate target(String key, Long formatId) {
        return formatId == null ? templateMapper.selectOne(Wrappers.<SysPrintTemplate>lambdaQuery()
                .eq(SysPrintTemplate::getTemplateKey, key)
                .eq(SysPrintTemplate::getIsDefault, true)) : requireFormat(key, formatId);
    }

    private SysPrintTemplate requireFormat(String key, Long formatId) {
        SysPrintTemplate t = templateMapper.selectById(formatId);
        if (t == null || !Objects.equals(key, t.getTemplateKey())) {
            throw new BizException("system.error.printFormatNotFound");
        }
        return t;
    }

    private long count(String key) {
        return templateMapper.selectCount(Wrappers.<SysPrintTemplate>lambdaQuery().eq(SysPrintTemplate::getTemplateKey, key));
    }

    /** 按需更新配置、JRXML、套打底图（JRXML、底图允许设为空） */
    private void updateColumns(Long id, String config, boolean setJrxml, String jrxml, boolean setBackground, String background) {
        templateMapper.update(null, Wrappers.<SysPrintTemplate>lambdaUpdate()
                .set(config != null, SysPrintTemplate::getConfig, config)
                .set(setJrxml, SysPrintTemplate::getJrxml, jrxml)
                .set(setBackground, SysPrintTemplate::getBackground, background)
                .eq(SysPrintTemplate::getId, id));
    }

    private Map<String, Object> parse(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = objectMapper.readValue(json, LinkedHashMap.class);
        return config;
    }

    private static boolean validKey(String key) {
        return key.matches("^[A-Za-z_][A-Za-z0-9_.]{0,63}$");
    }

    private static void checkKey(String key) {
        if (key == null || !validKey(key)) {
            throw new BizException("system.error.invalidTemplateKey");
        }
    }
}
