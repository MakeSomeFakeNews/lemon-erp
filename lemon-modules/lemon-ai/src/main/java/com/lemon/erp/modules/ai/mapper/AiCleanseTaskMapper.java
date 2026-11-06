package com.lemon.erp.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ai.entity.AiCleanseTask;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface AiCleanseTaskMapper extends BaseMapper<AiCleanseTask> {

    /** 任务列表（新的在前），带发起人姓名 */
    IPage<AiCleanseTask> selectTaskPage(Page<AiCleanseTask> page, @Param("sourceKeys") Collection<String> sourceKeys);
}
