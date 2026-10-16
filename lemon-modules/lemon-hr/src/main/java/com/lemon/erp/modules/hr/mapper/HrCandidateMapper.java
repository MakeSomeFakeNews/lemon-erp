package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrCandidate;
import org.apache.ibatis.annotations.Param;

public interface HrCandidateMapper extends BaseMapper<HrCandidate> {

    /** @param keyword 姓名、手机号或院校 */
    IPage<HrCandidate> selectPageList(IPage<HrCandidate> page, @Param("recruitId") Long recruitId,
                                      @Param("stage") String stage, @Param("keyword") String keyword);
}
