package com.lemon.erp.modules.monitor.dto;

import java.util.List;
import java.util.Map;

/**
 * 缓存服务信息
 *
 * @param type         redis | memory
 * @param info         Redis INFO 全部字段（memory 模式为空）
 * @param dbSize       key 总数
 * @param commandStats 命令统计，按调用次数倒序
 */
public record CacheInfo(String type, Map<String, String> info, long dbSize, List<CommandStat> commandStats) {

    /**
     * @param usec 累计耗时（微秒）
     */
    public record CommandStat(String name, long calls, long usec, double usecPerCall) {
    }
}
