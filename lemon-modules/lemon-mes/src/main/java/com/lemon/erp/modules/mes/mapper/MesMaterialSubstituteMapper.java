package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesMaterialSubstitute;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface MesMaterialSubstituteMapper extends BaseMapper<MesMaterialSubstitute> {

    /** 主料的替代料（含替代料名称和全部仓库库存），按优先级 */
    List<MesMaterialSubstitute> selectByMaterialIds(@Param("materialIds") Collection<Long> materialIds);

    void insertBatch(@Param("list") List<MesMaterialSubstitute> list);
}
