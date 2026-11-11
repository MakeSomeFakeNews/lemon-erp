package com.lemon.erp.framework.data;

/**
 * 有检索列（{@code search_key}）的主数据实体：物料、客户、供应商等。
 * <p>
 * 检索列由 {@link SearchKeys#build} 根据 {@link #searchTexts()} 计算：插入时由 MyBatis-Plus 拦截器自动填写，
 * 修改后由 Service 调用 {@link SearchKeyTable#refresh}；为空（升级前的数据、直接写库）时由 {@link SearchKeyBackfill} 在启动时回填。
 * 实体中的 {@code searchKey} 字段加 {@code @JsonIgnore}，不返回前端也不接收前端传入的值。
 */
public interface Searchable {

    /** 参与检索的文字，如编码、名称、规格、联系人、电话；为空的忽略 */
    String[] searchTexts();

    String getSearchKey();

    void setSearchKey(String searchKey);
}
