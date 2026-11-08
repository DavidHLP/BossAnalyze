package com.david.hlp.web.common.entity;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页信息包装类，与Spring Data分页组件兼容
 *
 * @author david
 * @param <T> 数据对象泛型
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PageInfo<T> implements Serializable {
    /**
     * 分页数据列表
     */
    private List<T> content;

    /**
     * 查询条件对象
     */
    private T query;

    /**
     * 当前页码，从0开始
     */
    private Integer number;

    /**
     * 每页显示条数
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Long totalElements;

    /**
     * 总页数
     */
    private Integer totalPages;

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
    private Boolean first;

    /**
     * 是否为最后一页
     */
    private Boolean last;

    /**
     * 将Spring Data的Page对象转换为PageInfo对象
     *
     * @param page Spring Data的Page对象
     * @param <T>  数据对象泛型
     * @return PageInfo对象
     */
    public static <T> PageInfo<T> of(Page<T> page) {
        return PageInfo.<T>builder()
                .content(page.getContent())
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 将Spring Data的Page对象转换为PageInfo对象，并转换内容类型
     *
     * @param page      Spring Data的Page对象
     * @param converter 内容转换函数
     * @param <T>       源数据类型
     * @param <R>       目标数据类型
     * @return PageInfo对象
     */
    public static <T, R> PageInfo<R> of(Page<T> page, Function<T, R> converter) {
        List<R> convertedContent = page.getContent().stream()
                .map(converter)
                .collect(Collectors.toList());

        return PageInfo.<R>builder()
                .content(convertedContent)
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * 获取分页数据
     * 
     * @return 分页数据列表
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 当前页码（从0开始计数）
     * 
     * @return 当前页码
     */
    public int getNumber() {
        return number;
    }

    /**
     * 获取每页大小
     * 
     * @return 每页大小
     */
    public int getSize() {
        return size;
    }

    /**
     * 获取总元素数
     * 
     * @return 总元素数
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * 获取总页数
     * 
     * @return 总页数
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 是否有下一页
     * 
     * @return 是否有下一页
     */
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * 是否有上一页
     * 
     * @return 是否有上一页
     */
    public boolean hasPrevious() {
        return hasPrevious;
    }

    /**
     * 是否为第一页
     * 
     * @return 是否为第一页
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * 是否为最后一页
     * 
     * @return 是否为最后一页
     */
    public boolean isLast() {
        return last;
    }

    /**
     * 创建Pageable对象
     *
     * @return Spring Data的Pageable对象
     */
    public Pageable toPageable() {
        return PageRequest.of(this.number, this.size);
    }

    /**
     * 创建带排序的Pageable对象
     *
     * @param sort 排序信息
     * @return Spring Data的Pageable对象
     */
    public Pageable toPageable(Sort sort) {
        return PageRequest.of(this.number, this.size, sort);
    }
}
