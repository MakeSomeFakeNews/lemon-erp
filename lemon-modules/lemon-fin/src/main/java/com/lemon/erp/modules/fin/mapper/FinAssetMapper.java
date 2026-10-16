package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.model.AssetStatus;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinAssetMapper extends BaseMapper<FinAsset> {

    IPage<FinAsset> selectPageList(IPage<FinAsset> page, @Param("keyword") String keyword, @Param("category") String category,
                                   @Param("status") AssetStatus status, @Param("deptId") Long deptId);

    /** 需要计提折旧的资产：在用 / 闲置、开始使用日期早于期间首日、未提足 */
    List<FinAsset> selectDepreciable(@Param("periodStart") LocalDate periodStart);

    /**
     * 累加折旧（撤销时为负数），已计提月数随之 +1 / -1
     *
     * @param expectedMonths 计提时的已计提月数（乐观锁），撤销时为空
     */
    int addDepreciation(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("expectedMonths") Integer expectedMonths);
}
