package com.david.hlp.spark.service.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Service;
import com.david.hlp.spark.model.User.JobRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static org.apache.spark.sql.functions.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobRequirementAnalysis {
    private final SparkSession sparkSession;
    
    /**
     * 分析职位需求并统计关键词
     * 
     * @param positionName 职位名称，如果为"ALL"则分析所有职位
     * @return 职位需求分析结果
     */
    public JobRequirement analyzeJobRequirements(String positionName) {
        // 定义JSON结构
        StructType jsonSchema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("jobDescription", 
                DataTypes.createStructType(new StructField[]{
                    DataTypes.createStructField("keywords", 
                        DataTypes.createArrayType(DataTypes.StringType), true)
                }), true)
        });
        
        // 读取数据库中的职位数据
        Dataset<Row> df = sparkSession.read().format("jdbc")
            .option("url", "jdbc:mysql://localhost:3306/boss_data")
            .option("driver", "com.mysql.cj.jdbc.Driver")
            .option("user", "root")
            .option("password", "Alone117")
            .option("dbtable", "t_job_detail")
            .option("fetchsize", "10000")
            .load()
            .select("id", "position_name", "detail_data");
        
        // 解析JSON数据
        Dataset<Row> keywordsDF = df
            .withColumn("jsonData", from_json(col("detail_data"), jsonSchema))
            .select(
                col("id"),
                col("position_name"),
                col("jsonData.jobDescription.keywords").alias("keywords")
            )
            .cache();
        
        // 过滤数据
        Dataset<Row> filteredKeywordsDF;
        if (!"ALL".equals(positionName)) {
            filteredKeywordsDF = keywordsDF.filter(
                col("position_name").equalTo(positionName).and(col("keywords").isNotNull())
            );
        } else {
            filteredKeywordsDF = keywordsDF.filter(col("keywords").isNotNull());
        }
        
        // 注册临时视图
        filteredKeywordsDF.createOrReplaceTempView("filtered_keywords_view");
        
        // 执行SQL分析
        String sql = "WITH exploded_data AS (" +
            "  SELECT" +
            "    position_name," +
            "    keyword" +
            "  FROM" +
            "    filtered_keywords_view" +
            "  LATERAL VIEW explode(keywords) as keyword" +
            "  WHERE" +
            "    keyword IS NOT NULL" +
            ")," +
            "counted_data AS (" +
            "  SELECT" +
            "    position_name," +
            "    keyword," +
            "    COUNT(*) as keyword_count" +
            "  FROM" +
            "    exploded_data" +
            "  GROUP BY" +
            "    position_name, keyword" +
            ")" +
            "SELECT" +
            "  position_name," +
            "  keyword," + 
            "  keyword_count" +
            " FROM" +
            "  counted_data" +
            " ORDER BY" +
            "  position_name, keyword_count DESC";
        
        Dataset<Row> resultDF = sparkSession.sql(sql);
        
        // 将结果转换为JobRequirement对象
        List<Row> rows = resultDF.collectAsList();
        List<Map<String, Long>> keywordCounts = new ArrayList<>();
        
        for (Row row : rows) {
            Map<String, Long> keywordMap = new HashMap<>();
            keywordMap.put(row.getString(1), row.getLong(2));
            keywordCounts.add(keywordMap);
        }
        
        // 清理缓存
        keywordsDF.unpersist();
        
        // 构建并返回结果
        return JobRequirement.builder()
            .positionName(positionName)
            .keywordCounts(keywordCounts)
            .build();
    }
}
