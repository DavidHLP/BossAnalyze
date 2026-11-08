package com.david.hlp.crawler.boss.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

import com.david.hlp.crawler.boss.entity.PositionData;

@Mapper
public interface PositionDataMapper {
    /**
     * 新增职位数据
     *
     * @param positionData 职位数据
     * @return 影响行数
     */
    int insert(PositionData positionData);

    /**
     * 批量新增职位数据
     *
     * @param positionDataList 职位数据列表
     * @return 影响行数
     */
    int batchInsert(List<PositionData> positionDataList);

    /**
     * 根据ID更新职位数据
     *
     * @param positionData 职位数据
     * @return 影响行数
     */
    int updateById(PositionData positionData);

    /**
     * 根据ID删除职位数据
     *
     * @param id 职位ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 批量删除职位数据
     *
     * @param ids ID列表
     * @return 影响行数
     */
    int batchDeleteByIds(List<Integer> ids);

    /**
     * 根据ID获取职位数据
     *
     * @param id 职位ID
     * @return 职位数据
     */
    PositionData getById(Integer id);

    /**
     * 根据code获取职位数据
     *
     * @param code 职位编码
     * @return 职位数据
     */
    PositionData getByCode(String code);

    /**
     * 获取所有职位数据列表
     *
     * @return 职位数据列表
     */
    List<PositionData> listAll();

    /**
     * 获取某一类型的职位数据列表
     *
     * @param type 职位类型
     * @return 职位数据列表
     */
    List<PositionData> listByType(String type);

    /**
     * 获取子职位列表
     *
     * @param parentId 父职位编码
     * @return 子职位列表
     */
    List<PositionData> listByParentId(String parentId);
}
