package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmPriceControl;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ScmPriceControlMapper extends BaseMapper<ScmPriceControl> {

    IPage<ScmPriceControl> selectPageList(IPage<ScmPriceControl> page, @Param("keyword") String keyword);

    /**
     * 往来单位某物料最近的成交价（已审核的订单，不含赠品），按订单日期倒序
     *
     * @return [{orderId, orderNo, orderDate, price（基本单位、含税、订单币种）, taxRate, qty, currency, exchangeRate}]
     */
    List<Map<String, Object>> selectHistory(@Param("kind") String kind, @Param("partnerId") Long partnerId,
                                            @Param("materialId") Long materialId, @Param("limit") int limit);
}
