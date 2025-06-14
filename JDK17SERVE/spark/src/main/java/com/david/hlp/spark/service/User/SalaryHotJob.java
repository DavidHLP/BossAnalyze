package com.david.hlp.spark.service.User;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Service;

import static org.apache.spark.sql.functions.*;
import org.apache.spark.sql.Encoders;
import java.util.List;
import java.util.Properties;

import lombok.RequiredArgsConstructor;
import com.david.hlp.spark.model.User.SalaryJob;

@Service
@RequiredArgsConstructor
public class SalaryHotJob {
    private final SparkSession sparkSession;

    public List<SalaryJob> analyzeSalaryHotJob(Long limit) {
        // 定义JSON结构
        StructType jsonSchema = new StructType()
                .add("basicInfo", new StructType()
                        .add("salary", DataTypes.StringType));

        // 设置数据库连接属性
        Properties connectionProperties = new Properties();
        connectionProperties.put("driver", "com.mysql.cj.jdbc.Driver");
        connectionProperties.put("user", "root");
        connectionProperties.put("password", "Alone117");
        connectionProperties.put("fetchsize", "10000");

        // 从MySQL数据库加载数据
        Dataset<Row> salaryDf = sparkSession.read()
                .jdbc("jdbc:mysql://localhost:3306/boss_data", "t_job_detail", connectionProperties)
                .select("position_name", "detail_data", "city_name");

        // 解析JSON数据并提取薪资信息
        salaryDf = salaryDf
                .withColumn("jsonData", functions.from_json(col("detail_data"), jsonSchema))
                .select(
                        col("position_name"),
                        col("city_name"),
                        col("jsonData.basicInfo.salary").as("salary"))
                .filter(
                        col("salary").rlike("^\\d+-\\d+K$").or(col("salary").rlike("^\\d+K$"))
                                .and(col("salary").isNotNull()));

        // 提取最低和最高薪资
        salaryDf = salaryDf
                .withColumn(
                        "min_salary",
                        when(
                                col("salary").rlike("^\\d+-\\d+K$"),
                                regexp_extract(col("salary"), "^(\\d+)-\\d+K$", 1).cast(DataTypes.IntegerType))
                                .otherwise(
                                        regexp_extract(col("salary"), "^(\\d+)K$", 1).cast(DataTypes.IntegerType)))
                .withColumn(
                        "max_salary",
                        when(
                                col("salary").rlike("^\\d+-\\d+K$"),
                                regexp_extract(col("salary"), "^\\d+-(\\d+)K$", 1).cast(DataTypes.IntegerType))
                                .otherwise(
                                        regexp_extract(col("salary"), "^(\\d+)K$", 1).cast(DataTypes.IntegerType)));

        // 缓存数据
        salaryDf.cache();

        // 创建临时视图
        salaryDf.createOrReplaceTempView("salary_view");

        // 执行查询 - 高薪职位排名
        Dataset<SalaryJob> rankingDf = sparkSession.sql(
                "WITH position_avg_salary AS (" +
                        "  SELECT " +
                        "    position_name as position_name, " +
                        "    COUNT(*) as job_count, " +
                        "    MIN(min_salary) as min_salary, " +
                        "    MAX(max_salary) as max_salary, " +
                        "    ROUND((AVG(min_salary) + AVG(max_salary))/2, 1) as avg_salary " +
                        "  FROM " +
                        "    salary_view " +
                        "  GROUP BY " +
                        "    position_name " +
                        "  HAVING " +
                        "    COUNT(*) >= 10 " +
                        "), " +
                        "city_avg_salary AS (" +
                        "  SELECT " +
                        "    position_name, " +
                        "    city_name, " +
                        "    COUNT(*) as city_job_count, " +
                        "    ROUND((AVG(min_salary) + AVG(max_salary))/2, 1) as city_avg_salary, " +
                        "    ROW_NUMBER() OVER (PARTITION BY position_name ORDER BY (AVG(min_salary) + AVG(max_salary))/2 DESC) as rank "
                        +
                        "  FROM " +
                        "    salary_view " +
                        "  GROUP BY " +
                        "    position_name, city_name " +
                        "  HAVING " +
                        "    COUNT(*) >= 5 " +
                        ") " +
                        "SELECT " +
                        "  p.position_name as positionName, " +
                        "  p.job_count as jobCount, " +
                        "  p.min_salary as minSalary, " +
                        "  p.max_salary as maxSalary, " +
                        "  p.avg_salary as avgSalary, " +
                        "  c.city_name as recommendedCity, " +
                        "  c.city_avg_salary as recommendedCitySalary, " +
                        "  c.city_job_count as recommendedCityJobCount " +
                        "FROM " +
                        "  position_avg_salary p " +
                        "LEFT JOIN " +
                        "  city_avg_salary c ON p.position_name = c.position_name AND c.rank = 1 " +
                        "ORDER BY " +
                        "  p.avg_salary DESC, " +
                        "  p.job_count DESC " +
                        "LIMIT " + limit)
                .as(Encoders.bean(SalaryJob.class));

        // 将DataFrame转换为List<SalaryJob>
        List<SalaryJob> result = rankingDf.collectAsList();
        // 释放缓存
        salaryDf.unpersist();
        return result;
    }
}
