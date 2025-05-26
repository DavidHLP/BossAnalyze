package com.david.hlp.crawler.boss.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDescriptionInfo implements Serializable {
    private static final long serialVersionUID = 1L;
        
        /**
         * 任职要求
         */
        private String requirements;
        
        /**
         * 岗位职责
         */
        private String responsibilities;
        
        /**
         * 完整职位描述
         */
        private String fullDescription;
        
        /**
         * 关键词标签
         */
        private List<String> keywords;
}
