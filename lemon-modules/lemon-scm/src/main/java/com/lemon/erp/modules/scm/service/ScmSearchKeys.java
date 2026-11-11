package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.data.SearchKeyTable;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 供应链模块有检索列（search_key）的表：客户、供应商，修改后刷新、启动时回填，见 {@link com.lemon.erp.framework.data.SearchKeys}
 */
@Configuration
public class ScmSearchKeys {

    @Bean
    public SearchKeyTable<ScmCustomer> customerSearchKeys(ScmCustomerMapper mapper) {
        return new SearchKeyTable<>("scm_customer", mapper, ScmCustomer::getId, ScmCustomer::getSearchKey);
    }

    @Bean
    public SearchKeyTable<ScmSupplier> supplierSearchKeys(ScmSupplierMapper mapper) {
        return new SearchKeyTable<>("scm_supplier", mapper, ScmSupplier::getId, ScmSupplier::getSearchKey);
    }
}
