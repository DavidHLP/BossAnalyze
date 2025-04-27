package com.david.hlp.spark.service;

import com.david.hlp.spark.model.JobKeywordResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.spark.sql.functions.*;

/**
 * 职位需求分析服务
 */
public class JobRequirementAnalysis {
    
    /**
     * 分析职位关键词
     * 
     * @param positionName 职位名称
     * @return 职位关键词分析结果
     */
    public List<JobKeywordResult> analyzeJobRequirements(String positionName) {
        if (positionName == null || positionName.trim().isEmpty()) {
            positionName = "信用卡销售";
        }
        
        final String finalPositionName = positionName;
        List<JobKeywordResult> resultList = new ArrayList<>();
        
        // 创建SparkSession并优化配置
        SparkSession spark = SparkSession.builder()
                .appName("UserAnalyze")
                .master("local[*]")
                .config("spark.jars.packages", "mysql:mysql-connector-java:8.0.28")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.shuffle.partitions", "10")
                .getOrCreate();
        
        try {
            // 从MySQL读取数据，只选择需要的列
            Dataset<Row> df = spark.read().format("jdbc")
                    .option("url", "jdbc:mysql://localhost:3306/boss_data")
                    .option("driver", "com.mysql.cj.jdbc.Driver")
                    .option("user", "root")
                    .option("password", "Alone117")
                    .option("dbtable", "t_job_detail")
                    .option("fetchsize", "10000")
                    .load()
                    .select("id", "position_name", "detail_data");
            
            // 定义JSON结构
            StructType jsonSchema = new StructType(new StructField[]{
                    DataTypes.createStructField("jobDescription", 
                            new StructType(new StructField[]{
                                    DataTypes.createStructField("keywords", 
                                            DataTypes.createArrayType(DataTypes.StringType), true)
                            }), true)
            });
            
            // 解析JSON并提取keywords
            Dataset<Row> keywordsDF = df
                    .withColumn("jsonData", from_json(col("detail_data"), jsonSchema))
                    .select(
                            col("id"),
                            col("position_name"),
                            col("jsonData.jobDescription.keywords").alias("keywords")
                    )
                    .cache();
            
            // 根据职位名称过滤数据
            Dataset<Row> filteredKeywordsDF;
            if (!"ALL".equals(finalPositionName)) {
                filteredKeywordsDF = keywordsDF.filter(
                        col("position_name").equalTo(finalPositionName)
                                .and(col("keywords").isNotNull())
                );
            } else {
                filteredKeywordsDF = keywordsDF.filter(col("keywords").isNotNull());
            }
            
            // 显示过滤后的数据
            System.out.println("职位 '" + finalPositionName + "' 的关键词 (已排除空值):");
            filteredKeywordsDF.show(false);
            
            // 创建临时视图
            filteredKeywordsDF.createOrReplaceTempView("filtered_keywords_view");
            
            // 执行SQL查询
            String finalSql = "WITH exploded_data AS (" +
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
                    ")," +
                    "structured_data AS (" +
                    "  SELECT" +
                    "    position_name," +
                    "    collect_list(struct(keyword_count as count, keyword)) as kw_structs" +
                    "  FROM" +
                    "    counted_data" +
                    "  GROUP BY" +
                    "    position_name" +
                    ")" +
                    "SELECT" +
                    "  position_name," +
                    "  to_json(" +
                    "    transform(" +
                    "      sort_array(kw_structs, false)," +
                    "      x -> map(x.keyword, x.count)" +
                    "    )" +
                    "  ) as keyword_counts_json" +
                    " FROM" +
                    "  structured_data";
            
            // 执行查询并显示结果
            Dataset<Row> resultDF = spark.sql(finalSql);
            System.out.println("职位 '" + finalPositionName + "' 的关键词统计 (已排序):");
            resultDF.show(false);
            
            // 将结果转换为JobKeywordResult对象列表
            List<Row> rows = resultDF.collectAsList();
            for (Row row : rows) {
                String position = row.getString(0);
                String keywordJson = row.getString(1);
                
                // 使用GSON解析JSON
                Map<String, Integer> keywordMap = new HashMap<>();
                JsonArray jsonArray = JsonParser.parseString(keywordJson).getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                        String keyword = entry.getKey();
                        int count = entry.getValue().getAsInt();
                        keywordMap.put(keyword, count);
                    }
                }
                
                // 使用Builder模式创建结果对象
                JobKeywordResult result = JobKeywordResult.builder()
                        .positionName(position)
                        .keywordCount(keywordMap)
                        .build();
                
                resultList.add(result);
            }
            
            // 释放缓存
            keywordsDF.unpersist();
        } finally {
            // 关闭SparkSession
            spark.stop();
        }
        
        return resultList;
    }
    
    /**
     * 获取单个职位的关键词分析结果
     * 
     * @param positionName 职位名称
     * @return 职位关键词分析结果
     */
    public JobKeywordResult getJobKeywordResult(String positionName) {
        List<JobKeywordResult> resultList = analyzeJobRequirements(positionName);
        return resultList.isEmpty() ? JobKeywordResult.builder().positionName(positionName).build() : resultList.get(0);
    }
}
