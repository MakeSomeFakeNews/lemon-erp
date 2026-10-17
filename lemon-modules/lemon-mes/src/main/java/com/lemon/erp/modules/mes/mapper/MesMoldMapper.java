package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesMold;
import com.lemon.erp.modules.mes.entity.MesMoldLog;
import com.lemon.erp.modules.mes.model.MoldStatus;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesMoldMapper extends BaseMapper<MesMold> {

    /**
     * @param keyword   编码、名称
     * @param productId 适用该产品
     * @param warn      只看达到寿命预警的
     */
    IPage<MesMold> selectPageList(IPage<MesMold> page, @Param("keyword") String keyword, @Param("moldType") String moldType,
                                  @Param("status") MoldStatus status, @Param("productId") Long productId, @Param("warn") Boolean warn);

    MesMold selectDetail(@Param("id") Long id);

    /** 领用到工单的模具（报工选择） */
    List<MesMold> selectByWo(@Param("woId") Long woId);

    /** 模具适用产品：moldId、materialId、code、name */
    List<Map<String, Object>> selectProducts(@Param("moldIds") Collection<Long> moldIds);

    int deleteProducts(@Param("moldId") Long moldId);

    int insertProducts(@Param("moldId") Long moldId, @Param("productIds") List<Long> productIds);

    /** 累计模次（并发安全） */
    int addUsed(@Param("id") Long id, @Param("shots") int shots);

    List<MesMoldLog> selectLogs(@Param("moldId") Long moldId);
}
