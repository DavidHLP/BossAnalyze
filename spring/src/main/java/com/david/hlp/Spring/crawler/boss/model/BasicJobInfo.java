package com.david.hlp.Spring.crawler.boss.model;

import java.io.Serializable;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BasicJobInfo implements Serializable {
        
        /**
         * 职位名称
         */
        private String positionName;
        
        /**
         * 薪资范围
         */
        private String salary;
        
        /**
         * 工作城市
         */
        private String city;
        
        /**
         * 工作经验要求
         */
        private String experience;
        
        /**
         * 学历要求
         */
        private String degree;
        
        /**
         * 工作地址
         */
        private String address;
}
