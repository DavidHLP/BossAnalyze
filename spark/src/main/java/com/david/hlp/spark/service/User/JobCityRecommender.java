package com.david.hlp.spark.service.User;

import lombok.RequiredArgsConstructor;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import com.david.hlp.spark.model.User.CityJobRecommendation;

import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class JobCityRecommender {
    private final SparkSession sparkSession;

    /**
     * 根据职位名称占比推荐城市。
     *
     * @param positionName 职位名称 (例如 "数据分析师").
     * @param limit        返回的最大城市数量。
     * @return 一个 CityJobRecommendation 列表，包含城市名称和职位占比。
     */
    public List<CityJobRecommendation> recommendCities(String positionName, int limit) {
        // JDBC 连接属性
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", "root");
        // 考虑将凭证外部化
        connectionProperties.put("password", "Alone117");
        connectionProperties.put("driver", "com.mysql.cj.jdbc.Driver");
        // 优化 JDBC 获取大小
        connectionProperties.put("fetchsize", "10000");

        // JDBC URL
        String jdbcUrl = "jdbc:mysql://localhost:3306/boss_data";

        // 从 MySQL 读取数据，仅选择必要的列
        Dataset<Row> df = sparkSession.read()
                .jdbc(jdbcUrl, "t_job_detail", connectionProperties)
                // 只需要这两个
                .select("position_name", "city_name");

        // 创建临时视图
        // 使用不同的名称以避免与类名冲突
        df.createOrReplaceTempView("JobCityRecommenderView");

        List<CityJobRecommendation> recommendations; // 声明变量
        try {
            // 为 LIKE 子句转义 positionName 中潜在的通配符
            // 在 Java 字符串中，'\' 本身也需要转义，所以 '\%' 变成 "\\%"
            String escapedPositionName = positionName.replace("%", "\\%").replace("_", "\\_");
            String likePattern = "%" + escapedPositionName + "%";


            // 构建 Spark SQL 查询
            // 使用 CAST 确保浮点除法
            // 添加对 total_count > 0 的检查以防止除以零
            String sqlQuery = String.format(
                    "WITH FilteredJobs AS (" +
                            "  SELECT city_name " +
                            "  FROM JobCityRecommenderView " +
                            // 使用转义后的模式
                            "  WHERE position_name LIKE '%s'" +
                            "), CompanyCounts AS (" +
                            "  SELECT city_name, COUNT(city_name) AS city_count " +
                            "  FROM FilteredJobs " +
                            "  GROUP BY city_name" +
                            "), TotalCount AS (" +
                            "  SELECT COUNT(*) AS total_count " +
                            "  FROM FilteredJobs" +
                            ") " +
                            "SELECT " +
                            "  cc.city_name, " +
                            "  CAST(cc.city_count AS DOUBLE) / tc.total_count AS proportion " +
                            "FROM CompanyCounts cc " +
                            "CROSS JOIN TotalCount tc " +
                            // 避免除以零
                            "WHERE tc.total_count > 0 " +
                            "ORDER BY proportion DESC " +
                            "LIMIT %d",
                    // 将参数传递给 format
                    likePattern, limit);

            // 执行查询
            Dataset<Row> result = sparkSession.sql(sqlQuery);

            // （可选）显示结果（用于调试或如果需要）
            result.show(); // 仍然显示 Row 格式的结果

            // 将 Dataset<Row> 映射到 Dataset<CityJobRecommendation>
            Dataset<CityJobRecommendation> recommendationDs = result.map(
                    (MapFunction<Row, CityJobRecommendation>) row -> {
                        CityJobRecommendation rec = new CityJobRecommendation();
                        // 假设 CityJobRecommendation 有 setCityName 和 setProportion 方法
                        // 从 Row 中按名称获取数据，更健壮
                        rec.setCityName(row.getAs("city_name"));
                        rec.setProportion(row.getAs("proportion"));
                        return rec;
                    },
                    Encoders.bean(CityJobRecommendation.class) // 为 JavaBean 提供编码器
            );

            // 收集结果为 List<CityJobRecommendation>
            recommendations = recommendationDs.collectAsList();

        } finally {
            // 清理临时视图（无论是否发生异常都应执行）
            sparkSession.catalog().dropTempView("JobCityRecommenderView");
        }


        return recommendations; // 返回 List<CityJobRecommendation>
    }
}