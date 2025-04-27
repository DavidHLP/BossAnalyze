package com.david.hlp.Spring.analyzer.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.david.hlp.Spring.analyzer.entity.AnalyzerEntity;
@Mapper
public interface SalaryAnalyzerMapper {
    /**
     * 插入分析结果
     * @param analyzerEntity 分析结果实体
     */
    void insertAnalyzerResult(AnalyzerEntity analyzerEntity);

}
