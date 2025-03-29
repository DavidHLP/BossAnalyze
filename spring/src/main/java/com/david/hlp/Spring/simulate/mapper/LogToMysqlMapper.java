package com.david.hlp.Spring.simulate.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.david.hlp.Spring.simulate.entity.NginxAccessLog;

import java.util.List;

@Mapper
public interface LogToMysqlMapper {
    /**
     * 插入日志
     * @param log 日志实体
     */
    @Insert("INSERT INTO nginx_access_log (ip, access_time, method, path, protocol, status, bytes, referrer, user_agent, user_id) VALUES (#{ip}, #{accessTime}, #{method}, #{path}, #{protocol}, #{status}, #{bytes}, #{referrer}, #{userAgent}, #{userId})")
    void insertLog(NginxAccessLog log);

    /**
     * 批量插入所有日志，一次性插入多条数据
     * @param logs 日志列表
     */
    @Insert("<script>" +
            "INSERT INTO nginx_access_log (ip, access_time, method, path, protocol, status, bytes, referrer, user_agent, user_id) VALUES " +
            "<foreach collection='list' item='log' separator=','>" +
            "(#{log.ip}, #{log.accessTime}, #{log.method}, #{log.path}, #{log.protocol}, #{log.status}, #{log.bytes}, #{log.referrer}, #{log.userAgent}, #{log.userId})" +
            "</foreach>" +
            "</script>")
    void insertAllLogs(@Param("list") List<NginxAccessLog> logs);
}
