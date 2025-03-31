package com.david.hlp.kafka.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.david.hlp.kafka.entity.Degree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DegreeMapper extends BaseMapper<Degree> {

    /**
     * 根据uniqueId查询学历信息
     *
     * @param uniqueId 唯一标识
     * @return 学历信息
     */
    Degree getByUniqueId(@Param("uniqueId") String uniqueId);

    /**
     * 插入学历信息
     *
     * @param degree 学历信息
     * @return 影响行数
     */
    int insert(Degree degree);
}
