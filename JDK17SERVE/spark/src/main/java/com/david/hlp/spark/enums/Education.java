package com.david.hlp.spark.enums;

/**
 * 学历枚举类
 * 
 * @author david
 */
public enum Education {
    /**
     * 不限
     */
    UNLIMITED("不限"),
    
    /**
     * 初中及以下
     */
    JUNIOR_HIGH_AND_BELOW("初中及以下"),
    
    /**
     * 中专/中技
     */
    TECHNICAL_SECONDARY("中专/中技"),
    
    /**
     * 高中
     */
    HIGH_SCHOOL("高中"),
    
    /**
     * 大专
     */
    JUNIOR_COLLEGE("大专"),
    
    /**
     * 本科
     */
    BACHELOR("本科"),
    
    /**
     * 硕士
     */
    MASTER("硕士"),
    
    /**
     * 博士
     */
    DOCTOR("博士");
    
    private final String description;
    
    Education(String description) {
        this.description = description;
    }
    
    /**
     * 获取学历描述
     *
     * @return 学历描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据描述获取枚举值
     *
     * @param description 学历描述
     * @return 枚举值
     */
    public static Education getByDescription(String description) {
        for (Education education : values()) {
            if (education.getDescription().equals(description)) {
                return education;
            }
        }
        return UNLIMITED;
    }
} 