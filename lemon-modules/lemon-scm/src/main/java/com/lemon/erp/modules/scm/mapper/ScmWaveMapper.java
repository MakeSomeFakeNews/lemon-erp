package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmWave;
import com.lemon.erp.modules.scm.model.WaveStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ScmWaveMapper extends BaseMapper<ScmWave> {

    IPage<ScmWave> selectPageList(IPage<ScmWave> page, @Param("status") WaveStatus status, @Param("warehouseId") Long warehouseId,
                                  @Param("keyword") String keyword);

    ScmWave selectDetail(@Param("id") Long id);

    /**
     * 波次中待拣货的发货通知的拣货建议，按库位路径（区域 → 排序 → 编码，未分库位的在最后）、物料、批次排序：
     * deliveryId、deliveryNo、customerName、deliveryItemId、materialId、materialCode、materialName、spec、unit、serialEnabled、
     * locationId、locationCode、zone、batchNo、expiryDate、qty
     */
    List<Map<String, Object>> selectPickLines(@Param("waveId") Long waveId);

    /** 拣货建议不足通知数量的明细（库存不足）：deliveryNo、materialCode、materialName、unit、qty、suggested */
    List<Map<String, Object>> selectShortages(@Param("waveId") Long waveId);
}
