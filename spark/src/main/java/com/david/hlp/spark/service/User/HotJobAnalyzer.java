package com.david.hlp.spark.service.User;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import com.david.hlp.spark.model.User.HotJob;

import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotJobAnalyzer {
    private final SparkSession sparkSession;
    
    /**
     * 获取热门职位前N名
     * 
     * @param limit 限制返回数量
     * @return 热门职位列表
     */
    public List<HotJob> listHotJobs(Long limit) {
        // 从MySQL数据库中读取数据
        Dataset<Row> df = sparkSession.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://localhost:3306/boss_data")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("user", "root")
                .option("password", "Alone117") // 注意：生产环境中不建议硬编码密码
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                .select("id", "position_name");
        
        // 创建临时视图
        df.createOrReplaceTempView("HotJobAnalyzer");
        
        // 使用Spark SQL查询数据
        Dataset<Row> result = sparkSession.sql(
                "SELECT position_name AS positionName, count(*) as positionCount " +
                "FROM HotJobAnalyzer " +
                "GROUP BY positionName " +
                "ORDER BY positionCount DESC " +
                "LIMIT " + limit);
        
        // 使用Dataset API将结果转换为Bean对象
        return result.as(org.apache.spark.sql.Encoders.bean(HotJob.class))
                .collectAsList();
    }
}
