package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.mes.entity.MesBom;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesBomMapper extends BaseMapper<MesBom> {

    /** @param keyword 产品编码或名称 */
    IPage<MesBom> selectPageList(IPage<MesBom> page, @Param("keyword") String keyword, @Param("status") EnableStatus status);

    MesBom selectDetail(@Param("id") Long id);

    /** 产品的默认 BOM（启用的） */
    List<MesBom> selectDefaults(@Param("materialIds") Collection<Long> materialIds);

    /**
     * 全部 BOM 的父子关系，校验循环引用
     *
     * @return [{parent, child}]
     */
    List<Map<String, Object>> selectEdges();
}
