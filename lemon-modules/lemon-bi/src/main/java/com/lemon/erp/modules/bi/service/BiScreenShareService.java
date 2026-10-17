package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.dto.BiScreen;
import com.lemon.erp.modules.bi.dto.ScreenShareRequest;
import com.lemon.erp.modules.bi.entity.BiScreenShare;
import com.lemon.erp.modules.bi.mapper.BiScreenMapper;
import com.lemon.erp.modules.bi.mapper.BiScreenShareMapper;
import com.lemon.erp.modules.bi.model.ScreenKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 大屏投屏链接：令牌为 128 位随机数，链接免登录、只读，只能查看链接中指定的大屏；
 * 停用、过期或重置令牌后原链接立即失效
 */
@Service
@RequiredArgsConstructor
public class BiScreenShareService {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 最近访问时间的更新间隔，避免每次刷新都写库 */
    private static final long ACCESS_UPDATE_MINUTES = 5;

    private final BiScreenShareMapper shareMapper;
    private final BiScreenMapper screenMapper;
    private final BiScreenService screenService;

    public List<BiScreenShare> list() {
        return shareMapper.selectList(Wrappers.<BiScreenShare>lambdaQuery().orderByDesc(BiScreenShare::getId));
    }

    public Long create(ScreenShareRequest req) {
        BiScreenShare share = new BiScreenShare();
        apply(share, req);
        share.setToken(newToken());
        shareMapper.insert(share);
        return share.getId();
    }

    public void update(Long id, ScreenShareRequest req) {
        BiScreenShare share = get(id);
        apply(share, req);
        shareMapper.updateById(share);
    }

    /** 重新生成令牌，原链接失效 */
    public String resetToken(Long id) {
        BiScreenShare share = get(id);
        share.setToken(newToken());
        shareMapper.updateById(share);
        return share.getToken();
    }

    public void delete(Long id) {
        shareMapper.deleteById(get(id).getId());
    }

    // ---------------------------------------------------------------- 免登录访问

    /** 链接信息：{name, company, screens: [{key, label}], intervalSeconds, workCenterId, workCenterName} */
    public BiInsight.ShareInfo publicInfo(String token) {
        BiScreenShare share = resolve(token);
        return TenantContext.callAs(tenantOf(share), () -> new BiInsight.ShareInfo(share.getName(),
                screenMapper.tenantName(tenantOf(share)),
                keys(share).stream().map(k -> new BiInsight.ScreenOption(k.name(), Messages.get(k.getLabel()))).toList(),
                share.getIntervalSeconds(), share.getWorkCenterId(),
                share.getWorkCenterId() == null ? null : screenMapper.workCenterName(share.getWorkCenterId())));
    }

    public BiScreen.ScreenData publicData(String token, ScreenKey key) {
        BiScreenShare share = resolve(token);
        if (!keys(share).contains(key)) {
            throw new BizException("bi.error.screenNotIncludedLink");
        }
        return TenantContext.callAs(tenantOf(share), () -> screenService.data(key, key.isByWorkCenter() ? share.getWorkCenterId() : null));
    }

    /** 按令牌查找（跨租户），校验启用和有效期，按间隔记录访问时间 */
    private BiScreenShare resolve(String token) {
        BiScreenShare share = token == null || token.length() != 32 ? null
                : TenantContext.callIgnore(() -> shareMapper.selectOne(Wrappers.<BiScreenShare>lambdaQuery().eq(BiScreenShare::getToken, token)));
        LocalDateTime now = LocalDateTime.now();
        if (share == null || share.getStatus() != EnableStatus.ENABLED || share.getExpireTime() != null && share.getExpireTime().isBefore(now)) {
            throw new BizException("bi.error.shareLinkInvalidDisabledExpired");
        }
        if (share.getLastAccessTime() == null || share.getLastAccessTime().plusMinutes(ACCESS_UPDATE_MINUTES).isBefore(now)) {
            // 只更新访问时间（实体更新会把可清空的字段一并写成空）
            TenantContext.runAs(tenantOf(share), () -> shareMapper.update(null, Wrappers.<BiScreenShare>lambdaUpdate()
                    .set(BiScreenShare::getLastAccessTime, now).eq(BiScreenShare::getId, share.getId())));
        }
        return share;
    }

    // ---------------------------------------------------------------- 工具

    private BiScreenShare get(Long id) {
        BiScreenShare share = shareMapper.selectById(id);
        if (share == null) {
            throw new BizException("bi.error.shareLinkNotFound");
        }
        return share;
    }

    private static void apply(BiScreenShare share, ScreenShareRequest req) {
        share.setName(req.name().trim());
        share.setScreens(req.screens().stream().distinct().map(ScreenKey::name).collect(Collectors.joining(",")));
        share.setIntervalSeconds(req.intervalSeconds());
        share.setWorkCenterId(req.workCenterId());
        share.setExpireTime(req.expireTime());
        share.setStatus(req.status());
    }

    private static List<ScreenKey> keys(BiScreenShare share) {
        return Arrays.stream(share.getScreens().split(",")).map(ScreenKey::valueOf).toList();
    }

    private static Long tenantOf(BiScreenShare share) {
        return share.getTenantId() == null ? TenantContext.PLATFORM_TENANT_ID : share.getTenantId();
    }

    private static String newToken() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
