package com.david.hlp.crawler.boss.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.david.hlp.crawler.boss.model.JobList;

@Mapper
public interface JobListMapper {
    void insert(JobList jobList);
    JobList selectByUrl(String url);
}
