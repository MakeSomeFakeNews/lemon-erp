package com.lemon.erp.modules.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.workflow.entity.WfCopy;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface WfCopyMapper extends BaseMapper<WfCopy> {

    /**
     * 同一节点抄送给多个用户，单条 INSERT ... VALUES (...), (...)
     *
     * @param copy 公共字段（流程、节点、发起人、抄送时间），userId 取自 userIds
     */
    int insertBatch(@Param("copy") WfCopy copy, @Param("userIds") Collection<Long> userIds);
}
