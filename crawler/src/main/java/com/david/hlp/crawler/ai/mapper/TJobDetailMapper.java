package com.david.hlp.crawler.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;

import io.lettuce.core.dynamic.annotation.Param;

@Mapper
public interface TJobDetailMapper {
    @Select("SELECT id , detail_data from t_job_detail WHERE employee_benefits is NULL OR employee_benefits = '' limit 10")
    List<MiniJobDetail> selectEmployeeBenefitsJobDetailIsNull();

    @Update("UPDATE t_job_detail SET employee_benefits = #{employeeBenefits} WHERE id = #{id}")
    void updateEmployeeBenefits(MiniJobDetail miniJobDetail);

    @Select("SELECT id , detail_data from t_job_detail WHERE job_requirements is NULL OR job_requirements = '' limit 10")
    List<MiniJobDetail> selectJobRequirementsJobDetailIsNull();

    @Update("UPDATE t_job_detail SET job_requirements = #{jobRequirements} WHERE id = #{id}")
    void updateJobRequirements(MiniJobDetail miniJobDetail);

    List<MiniJobDetail> selectJobRequirements(@Param("cityName") String cityName,
            @Param("positionName") String positionName,
            @Param("startIndex") Long startIndex, @Param("limit") Integer limit);
}
