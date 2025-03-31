package com.david.hlp.Spring.common.interfaceI;

import com.david.hlp.Spring.common.result.PageInfo;
import java.util.List;
/**
 * 基础Service接口
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface BaseService<T, ID> {
    /**
     * 根据ID获取实体
     * @param id 主键ID
     * @return 实体对象
     */
    T getById(ID id);

    /**
     * 创建实体
     * @param entity 实体对象
     * @return 创建后的实体对象
     */
    T create(T entity);

    /**
     * 更新实体
     * @param entity 实体对象
     * @return 更新后的实体对象
     */
    T update(T entity);

    /**
     * 删除实体
     * @param id 主键ID
     */
    void deleteById(ID id);

    /**
     * 分页查询
     * @param page 页码
     * @param limit 每页大小
     * @return 分页结果
     */
    PageInfo<T> getPage(Integer page, Integer limit , T entity);

    /**
     * 获取所有实体列表
     * @return 实体列表
     */
    List<T> listAll();
}