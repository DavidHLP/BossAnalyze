package com.david.hlp.crawler.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;

import org.apache.ibatis.annotations.Param;

@Mapper
public interface TJobDetailMapper {
    List<MiniJobDetail> selectJobRequirements(@Param("cityName") String cityName,
            @Param("positionName") String positionName,
            @Param("startIndex") Long startIndex, @Param("limit") Integer limit);

    List<MiniJobDetail> selectMiniJobDetail(@Param("cityName") String cityName,
            @Param("positionName") String positionName,
            @Param("startIndex") Long startIndex, @Param("limit") Integer limit);
}
