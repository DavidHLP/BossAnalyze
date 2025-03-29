package com.david.hlp.Spring.common.result;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 分页信息包装类
 *
 * @author david
 * @param <T> 数据对象泛型
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PageInfo<T> {
    /**
     * 分页数据列表
     */
    private List<T> items;

    /**
     * 查询条件对象
     */
    private T query;

    /**
     * 当前页码，从1开始
     */
    private Integer pageNum;

    /**
     * 每页显示条数
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否为第一页
     */
    private Boolean isFirst;

    /**
     * 是否为最后一页
     */
    private Boolean isLast;
}
