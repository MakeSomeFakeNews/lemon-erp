package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.ScmPriceViews;
import com.lemon.erp.modules.scm.entity.ScmPromotion;
import com.lemon.erp.modules.scm.model.PromoType;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScmPromotionMapper extends BaseMapper<ScmPromotion> {

    /** @param activeOn 只看该日期有效（启用且在期间内）的 */
    IPage<ScmPromotion> selectPageList(IPage<ScmPromotion> page, @Param("keyword") String keyword, @Param("promoType") PromoType promoType,
                                       @Param("activeOn") LocalDate activeOn);

    ScmPromotion selectDetail(@Param("id") Long id);

    /** 订单日期有效、适用客户等级的启用促销 */
    List<ScmPromotion> selectApplicable(@Param("date") LocalDate date, @Param("customerLevel") String customerLevel);

    /** 应用记录：orderId、orderNo、customerName、orderDate、status、discountAmount、giftQty */
    List<ScmPriceViews.PromotionUsage> selectUsages(@Param("promotionId") Long promotionId);

    int deleteUsages(@Param("orderId") Long orderId);

    int insertUsages(@Param("orderId") Long orderId, @Param("list") List<Map<String, Object>> usages);
}
