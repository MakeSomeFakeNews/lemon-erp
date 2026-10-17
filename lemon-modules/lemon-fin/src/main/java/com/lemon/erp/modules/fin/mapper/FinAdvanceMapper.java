package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.model.SettleKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.entity.FinAdvanceApply;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FinAdvanceMapper extends BaseMapper<FinAdvance> {

    Page<FinAdvance> selectPageList(Page<FinAdvance> page, @Param("kind") SettleKind kind, @Param("partnerId") Long partnerId,
                                    @Param("keyword") String keyword, @Param("onlyOpen") boolean onlyOpen);

    FinAdvance selectDetail(@Param("id") Long id);

    List<FinAdvanceApply> selectApplies(@Param("advanceId") Long advanceId);

    /** 冲抵：已冲抵累计不超过金额；返回 0 表示超出 */
    int addUsed(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 往来单位未冲抵的预收 / 预付余额 */
    BigDecimal sumUnused(@Param("kind") SettleKind kind, @Param("partnerId") Long partnerId);
}
