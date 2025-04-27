package com.david.hlp.spark.service.User;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import com.david.hlp.spark.model.User.CityHotJob;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobHeatCalculator {
    private final SparkSession sparkSession;

    public List<CityHotJob> calculateJobHeat(String cityName) {
        // 读取数据，只选择需要的列
        Dataset<Row> df = sparkSession.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://localhost:3306/boss_data")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("user", "root")
                .option("password", "Alone117") // 注意：密码硬编码不安全，建议使用配置管理
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                .select("id", "position_name", "city_name"); // 只选择需要的列

        df.createOrReplaceTempView("JobHeatCalculator");

        // 使用 Spark SQL 查询数据，使用别名并转换类型以匹配 CityHotJob
        String query = String.format(
                "SELECT city_name AS cityName, position_name AS positionName, CAST(count(*) AS INT) AS jobCount " +
                        "FROM JobHeatCalculator " +
                        "WHERE city_name = '%s' " +
                        "GROUP BY city_name, position_name " +
                        "ORDER BY jobCount DESC " +
                        "LIMIT 10",
                cityName
        );

        Dataset<Row> result = sparkSession.sql(query);

        // 使用 Encoders 将 Dataset<Row> 直接转换为 List<CityHotJob>
        return result.as(Encoders.bean(CityHotJob.class)).collectAsList();
    }
}
