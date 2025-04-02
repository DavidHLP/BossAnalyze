package com.david.hlp.Spring.crawler.boss.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobList {
    /**
     * 主键ID
     */
    private Integer id;

    /**
     * HTML页面URL
     */
    private String htmlUrl;

    /**
     * 存储JSON格式的数据
     */
    private String jsonData;

    /**
     * 创建时间
     */
    private java.util.Date createdAt;

    /**
     * 更新时间
     */
    private java.util.Date updatedAt;

}
