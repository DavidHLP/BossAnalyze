package com.david.hlp.Spring.crawler.boss.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.david.hlp.Spring.crawler.boss.entity.JobDetailEntity;

import java.util.List;

@Mapper
public interface JobDetailEntityMapper {
    
    /**
     * 插入一条职位详情记录
     *
     * @param jobDetailEntity 职位详情实体
     * @return 影响行数
     */
    int insert(JobDetailEntity jobDetailEntity);
    
    /**
     * 通过ID更新职位详情
     *
     * @param jobDetailEntity 职位详情实体
     * @return 影响行数
     */
    int updateById(JobDetailEntity jobDetailEntity);
    
    /**
     * 逻辑删除职位详情
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据ID获取职位详情
     *
     * @param id 主键ID
     * @return 职位详情实体
     */
    JobDetailEntity getById(@Param("id") Long id);
    
    /**
     * 根据职位ID获取职位详情
     *
     * @param positionId 职位ID
     * @return 职位详情实体
     */
    JobDetailEntity getByPositionId(@Param("positionId") String positionId);
    
    /**
     * 根据公司ID查询职位列表
     *
     * @param companyId 公司ID
     * @return 职位详情列表
     */
    List<JobDetailEntity> listByCompanyId(@Param("companyId") String companyId);
    
    /**
     * 根据职位名称模糊查询职位列表
     *
     * @param positionName 职位名称
     * @return 职位详情列表
     */
    List<JobDetailEntity> listByPositionName(@Param("positionName") String positionName);
    
    /**
     * 分页查询职位列表
     *
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 职位详情列表
     */
    List<JobDetailEntity> listPage(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 统计职位总数
     *
     * @return 职位总数
     */
    int count();
}
