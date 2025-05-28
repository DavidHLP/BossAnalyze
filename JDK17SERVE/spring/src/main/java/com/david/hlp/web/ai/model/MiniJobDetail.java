package com.david.hlp.web.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 职位详情精简实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MiniJobDetail implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 职位名称
     */
    private String positionName;

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * 职位详情数据
     */
    private String detailData;

    /**
     * 员工福利
     */
    private String employeeBenefits;

    /**
     * 职位需求
     */
    private String jobRequirements;
}
