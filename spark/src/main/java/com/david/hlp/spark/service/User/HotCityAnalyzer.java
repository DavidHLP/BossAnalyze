package com.david.hlp.spark.service.User;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import com.david.hlp.spark.model.User.HotCity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotCityAnalyzer {

    private final SparkSession sparkSession; // 注入 SparkSession

    public List<HotCity> analyzeHotCities(Long limit) {
        // 使用注入的 sparkSession
        Dataset<Row> df = sparkSession.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://localhost:3306/boss_data")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("user", "root")
                .option("password", "Alone117") // 注意：生产环境中不建议硬编码密码
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                // 仅选择 city_name 列，因为 HotCity 模型只需要城市名称和计数
                .select("city_name");

        df.createOrReplaceTempView("HotCityAnalyzer");

        // 使用 Spark SQL 查询数据，并重命名列以匹配 HotCity 字段
        Dataset<HotCity> hotCityDs = sparkSession.sql(
                "SELECT city_name AS cityName, count(*) AS jobCount " + // 重命名列
                "FROM HotCityAnalyzer " +
                "GROUP BY city_name " +
                "ORDER BY jobCount DESC " +
                "LIMIT "+ limit) // 取前 10 个城市
                .as(Encoders.bean(HotCity.class)); // 直接转换为 Dataset<HotCity>

        // 收集结果为 List
        List<HotCity> hotCities = hotCityDs.collectAsList();

        // 返回结果列表
        return hotCities;
    }
}
