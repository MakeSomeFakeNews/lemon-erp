package com.lemon.erp.modules.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.workflow.entity.WfModel;
import org.apache.ibatis.annotations.Param;

public interface WfModelMapper extends BaseMapper<WfModel> {

    /**
     * 分页查询流程模型，不返回设计 JSON，只返回是否已设计
     *
     * @param name     名称或标识，模糊匹配，可为空
     * @param category 分类，可为空
     */
    IPage<WfModel> selectPageList(IPage<WfModel> page, @Param("name") String name, @Param("category") String category);
}
