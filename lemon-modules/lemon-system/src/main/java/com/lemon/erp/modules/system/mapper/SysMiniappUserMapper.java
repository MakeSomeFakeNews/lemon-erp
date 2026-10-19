package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysMiniappUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysMiniappUserMapper extends BaseMapper<SysMiniappUser> {

    /** 微信在各租户中的绑定（登录时还没有租户，不加租户条件） */
    @InterceptorIgnore(tenantLine = "true")
    List<SysMiniappUser> selectByOpenid(@Param("openid") String openid);
}
