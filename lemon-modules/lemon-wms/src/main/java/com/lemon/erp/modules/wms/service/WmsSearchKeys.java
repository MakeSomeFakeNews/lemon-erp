package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.data.SearchKeyTable;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仓库模块有检索列（search_key）的表：物料，修改后刷新、启动时回填，见 {@link com.lemon.erp.framework.data.SearchKeys}
 */
@Configuration
public class WmsSearchKeys {

    @Bean
    public SearchKeyTable<WmsMaterial> materialSearchKeys(WmsMaterialMapper mapper) {
        return new SearchKeyTable<>("wms_material", mapper, WmsMaterial::getId, WmsMaterial::getSearchKey);
    }
}
