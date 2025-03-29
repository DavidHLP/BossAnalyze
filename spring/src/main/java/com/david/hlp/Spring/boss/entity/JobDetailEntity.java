package com.david.hlp.Spring.boss.entity;

import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDetailEntity implements Serializable{
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 职位唯一标识
     */
    private String positionId;
    /**
     * 职位名称
     */
    private String positionName;
    /**
     * 公司唯一标识
     */
    private String companyId;
    /**
     * 公司名称
     */
    private String companyName;
    /**
     * JobDetailData完整数据(JSON格式)
     */
    private String detailData;
    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;
    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;
    /**
     * 是否删除
     */
    private Boolean isDeleted;
}
