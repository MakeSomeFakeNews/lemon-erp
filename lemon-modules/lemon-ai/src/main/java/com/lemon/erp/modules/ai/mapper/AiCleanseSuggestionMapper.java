package com.lemon.erp.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ai.dto.AiCleanseStat;
import com.lemon.erp.modules.ai.dto.AiCleanseSuggestionQuery;
import com.lemon.erp.modules.ai.entity.AiCleanseSuggestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiCleanseSuggestionMapper extends BaseMapper<AiCleanseSuggestion> {

    /** 批量插入（单条多值 INSERT） */
    int insertBatch(@Param("list") List<AiCleanseSuggestion> list);

    /** 按条件分页，带处理人姓名；疑似重复按组排列 */
    IPage<AiCleanseSuggestion> selectSuggestionPage(Page<AiCleanseSuggestion> page, @Param("q") AiCleanseSuggestionQuery q);

    /** 任务的建议按种类、状态计数 */
    List<AiCleanseStat> countByKindStatus(@Param("taskId") Long taskId);
}
