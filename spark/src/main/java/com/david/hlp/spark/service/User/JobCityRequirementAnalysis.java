package com.david.hlp.spark.service.User;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.from_json;

import java.util.List;

import com.david.hlp.spark.model.User.CityJobAnalysis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobCityRequirementAnalysis {
    private final SparkSession sparkSession;

    public List<CityJobAnalysis> jobCityRequirementAnalysis(String positionName, String cityName) {

        // 只读取需要的列，减少数据传输
        Dataset<Row> df = sparkSession.read().format("jdbc")
                .option("url", "jdbc:mysql://localhost:3306/boss_data")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("user", "root")
                .option("password", "Alone117") // 建议使用更安全的方式管理密码
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                .select(col("id"), col("position_name"), col("detail_data"), col("city_name")); // 只选择需要的列

        // 定义 detail_data 中 JSON 的部分结构 (只需要到 keywords)
        StructType jsonSchema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("jobDescription", DataTypes.createStructType(new StructField[]{
                        DataTypes.createStructField("keywords", DataTypes.createArrayType(DataTypes.StringType), true) // keywords 是一个字符串数组
                }), true)
        });

        // 解析 JSON 并提取 keywords
        Dataset<Row> keywordsDf = df.withColumn("jsonData", from_json(col("detail_data"), jsonSchema))
                .select(
                        col("id"),
                        col("position_name"),
                        col("city_name"),
                        col("jsonData.jobDescription.keywords").alias("keywords")
                )
                .cache(); // 缓存频繁使用的中间结果

        // 缩小范围：首先进行过滤，减少后续处理的数据量
        Dataset<Row> filteredKeywordsDf;
        if (!"ALL".equals(positionName) && !"ALL".equals(cityName)) {
            filteredKeywordsDf = keywordsDf.filter(
                    col("position_name").equalTo(positionName)
                            .and(col("city_name").equalTo(cityName))
                            .and(col("keywords").isNotNull()) // 同时过滤掉 keywords 为 NULL 的记录
            );
        } else if (!"ALL".equals(positionName)) {
            filteredKeywordsDf = keywordsDf.filter(
                    col("position_name").equalTo(positionName)
                            .and(col("keywords").isNotNull()) // 同时过滤掉 keywords 为 NULL 的记录
            );
        } else if (!"ALL".equals(cityName)) {
            filteredKeywordsDf = keywordsDf.filter(
                    col("city_name").equalTo(cityName)
                            .and(col("keywords").isNotNull()) // 同时过滤掉 keywords 为 NULL 的记录
            );
        } else {
            filteredKeywordsDf = keywordsDf.filter(
                    col("keywords").isNotNull() // 只过滤掉 keywords 为 NULL 的记录
            );
        }

        // 显示过滤后的原始数据（已排除 NULL）
        System.out.println(String.format("Keywords for position '%s' at city '%s' (NULL values excluded):", positionName, cityName));
        filteredKeywordsDf.show(false); // truncate=False 对应 Java 的 false

        // 注册 DataFrame 为临时视图
        filteredKeywordsDf.createOrReplaceTempView("filtered_keywords_view");

        // 使用优化后的 SQL：直接使用一个查询完成所有工作
        String finalSql = "WITH exploded_data AS (\n" +
                "  SELECT\n" +
                "    position_name,\n" +
                "    city_name,\n" +
                "    keyword\n" +
                "  FROM\n" +
                "    filtered_keywords_view\n" +
                "  LATERAL VIEW explode(keywords) exploded_table AS keyword\n" +
                "  WHERE\n" +
                "    keyword IS NOT NULL\n" +
                "),\n" +
                "counted_data AS (\n" +
                "  SELECT\n" +
                "    position_name,\n" +
                "    city_name,\n" +
                "    keyword,\n" +
                "    COUNT(*) as keyword_count\n" +
                "  FROM\n" +
                "    exploded_data\n" +
                "  GROUP BY\n" +
                "    position_name, city_name, keyword\n" +
                "),\n" +
                "structured_data AS (\n" +
                "  SELECT\n" +
                "    position_name,\n" +
                "    city_name,\n" +
                "    collect_list(struct(keyword_count as count, keyword)) as kw_structs\n" +
                "  FROM\n" +
                "    counted_data\n" +
                "  GROUP BY\n" +
                "    position_name, city_name\n" +
                ")\n" +
                "SELECT\n" +
                "  city_name,\n" +
                "  position_name,\n" +
                "    transform(\n" +
                "      sort_array(kw_structs, false),\n" +
                "      x -> map(x.keyword, x.count)\n" +
                "    ) \n" +
                " as keywordCounts \n" +
                "FROM\n" +
                "  structured_data";


        // 执行优化后的查询
        Dataset<Row> resultDf = sparkSession.sql(finalSql);

        // 将 DataFrame 映射到 Dataset<CityJobAnalysis>
        // 确保 CityJobAnalysis 类有对应的字段 (cityName, positionName, keywordCountsJson)
        // 并且有 getter/setter 或使用 Lombok @Data
        Dataset<CityJobAnalysis> cityJobAnalysisDs = resultDf
                .select(
                    col("city_name").alias("cityName"),
                    col("position_name").alias("positionName"),
                    col("keywordCounts").alias("keywordCounts")
                )
               .as(org.apache.spark.sql.Encoders.bean(CityJobAnalysis.class));

        // 将结果收集为 List
        List<CityJobAnalysis> results = cityJobAnalysisDs.collectAsList();

        // 清理缓存，释放内存
        keywordsDf.unpersist();

        return results; // 返回结果列表
    }
}
