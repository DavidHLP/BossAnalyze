package com.david.hlp.Spring.crawler.boss.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.david.hlp.Spring.crawler.boss.model.JobList;

@Mapper
public interface JobListMapper {
    void insert(JobList jobList);
}
