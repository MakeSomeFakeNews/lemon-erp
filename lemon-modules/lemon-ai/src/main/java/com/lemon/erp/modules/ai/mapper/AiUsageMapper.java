package com.lemon.erp.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ai.dto.AiUsageStat;
import com.lemon.erp.modules.ai.entity.AiUsage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageMapper extends BaseMapper<AiUsage> {

    /** 当前租户自某时刻起的 token 合计（输入 + 输出） */
    long sumTokensSince(@Param("since") LocalDateTime since);

    /** 按功能汇总 */
    List<AiUsageStat> sumByFeature(@Param("since") LocalDateTime since);

    /** 按调用人汇总（用量从多到少，最多 limit 人） */
    List<AiUsageStat> sumByUser(@Param("since") LocalDateTime since, @Param("limit") int limit);
}
