package com.david.hlp.Spring.crawler.boss.mapper;

import org.apache.ibatis.annotations.Param;

import com.david.hlp.Spring.crawler.boss.entity.CityData;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CityDataMapper {
    /**
     * 根据ID获取城市数据
     *
     * @param id 城市ID
     * @return 城市数据
     */
    CityData getById(@Param("id") Integer id);

    /**
     * 根据城市代码获取城市数据
     *
     * @param code 城市代码
     * @return 城市数据
     */
    CityData getByCode(@Param("code") Integer code);

    /**
     * 获取所有城市数据
     *
     * @return 城市数据列表
     */
    List<CityData> listAll();

    /**
     * 插入城市数据
     *
     * @param cityData 城市数据
     * @return 影响行数
     */
    int insert(CityData cityData);

    /**
     * 更新城市数据
     *
     * @param cityData 城市数据
     * @return 影响行数
     */
    int update(CityData cityData);

    /**
     * 根据ID删除城市数据
     *
     * @param id 城市ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据城市代码删除城市数据
     *
     * @param code 城市代码
     * @return 影响行数
     */
    int deleteByCode(@Param("code") Integer code);
}
