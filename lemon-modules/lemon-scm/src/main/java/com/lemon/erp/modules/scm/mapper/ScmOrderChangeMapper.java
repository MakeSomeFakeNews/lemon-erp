package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmOrderChange;
import com.lemon.erp.modules.scm.entity.ScmOrderChangeItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmOrderChangeMapper extends BaseMapper<ScmOrderChange> {

    List<ScmOrderChange> selectByOrder(@Param("orderId") Long orderId);

    List<ScmOrderChangeItem> selectItems(@Param("changeId") Long changeId);
}
