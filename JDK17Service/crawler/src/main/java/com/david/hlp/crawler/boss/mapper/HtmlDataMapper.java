package com.david.hlp.crawler.boss.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.david.hlp.crawler.boss.entity.HtmlData;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface HtmlDataMapper extends BaseMapper<HtmlData>{
    /**
     * 插入HTML数据
     *
     * @param htmlData HTML数据对象
     * @return 影响行数
     */
    int insert(HtmlData htmlData);

    /**
     * 批量插入HTML数据
     *
     * @param htmlDataList HTML数据对象列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<HtmlData> htmlDataList);

    /**
     * 根据ID获取HTML数据
     *
     * @param id 主键ID
     * @return HTML数据对象
     */
    HtmlData getById(@Param("id") Integer id);

    /**
     * 根据URL获取HTML数据
     *
     * @param url 网页URL
     * @return HTML数据对象
     */
    HtmlData getByUrl(@Param("url") String url);

    /**
     * 条件查询HTML数据列表
     *
     * @param htmlData 查询条件
     * @return HTML数据对象列表
     */
    List<HtmlData> listByCondition(HtmlData htmlData);

    /**
     * 更新HTML数据
     *
     * @param htmlData HTML数据对象
     * @return 影响行数
     */
    int update(HtmlData htmlData);

    /**
     * 更新HTML数据状态
     *
     * @param id 主键ID
     * @param status 状态值
     * @return 影响行数
     */
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 根据ID删除HTML数据
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据状态获取HTML数据列表
     *
     * @param status 状态值
     * @param limit 限制数量
     * @return HTML数据对象列表
     */
    List<HtmlData> listByStatus(@Param("status") Integer status, @Param("limit") Integer limit);
}