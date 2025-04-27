from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col, explode, to_json
from pyspark.sql.types import StructType, StructField, StringType, ArrayType

# 可以从命令行接收参数，如果没提供，使用默认值
final_position_name = '数据分析师'

# 增加 Spark 配置，优化性能
spark = SparkSession.builder.appName("UserAnalyze")\
    .config("spark.jars.packages", "mysql:mysql-connector-java:8.0.28") \
    .config("spark.sql.adaptive.enabled", "true") \
    .config("spark.sql.shuffle.partitions", "10") \
    .getOrCreate()

# 只读取需要的列，减少数据传输
df = spark.read.format("jdbc")\
    .option("url", "jdbc:mysql://localhost:3306/boss_data")\
    .option("driver", "com.mysql.cj.jdbc.Driver")\
    .option("user", "root")\
    .option("password", "Alone117")\
    .option("dbtable", "t_job_detail")\
    .option("fetchsize", "10000") \
    .load()\
    .select("id", "position_name", "detail_data")  # 只选择需要的列

# 定义 detail_data 中 JSON 的部分结构 (只需要到 keywords)
json_schema = StructType([
    StructField("jobDescription", StructType([
        StructField("keywords", ArrayType(StringType()), True)  # keywords 是一个字符串数组
    ]), True)
])

# 解析 JSON 并提取 keywords
keywords_df = df.withColumn("jsonData", from_json(col("detail_data"), json_schema)) \
                .select(
                    col("id"), 
                    col("position_name"), 
                    col("jsonData.jobDescription.keywords").alias("keywords")
                )\
                .cache()  # 缓存频繁使用的中间结果

# 缩小范围：首先进行过滤，减少后续处理的数据量
if final_position_name != 'ALL':
    filtered_keywords_df = keywords_df.filter(
        (col("position_name") == final_position_name) & 
        col("keywords").isNotNull()  # 同时过滤掉 keywords 为 NULL 的记录
    )
else:
    filtered_keywords_df = keywords_df.filter(
        col("keywords").isNotNull()  # 只过滤掉 keywords 为 NULL 的记录
    )

# 显示过滤后的原始数据（已排除 NULL）
print(f"Keywords for position '{final_position_name}' (NULL values excluded):")
filtered_keywords_df.show(truncate=False)

# 注册 DataFrame 为临时视图
filtered_keywords_df.createOrReplaceTempView("filtered_keywords_view")

# 使用优化后的 SQL：直接使用一个查询完成所有工作
final_sql = f"""
WITH exploded_data AS (
  SELECT
    position_name,
    keyword
  FROM
    filtered_keywords_view
  LATERAL VIEW explode(keywords) as keyword
  WHERE
    keyword IS NOT NULL
),
counted_data AS (
  SELECT
    position_name,
    keyword,
    COUNT(*) as keyword_count
  FROM
    exploded_data
  GROUP BY
    position_name, keyword
),
structured_data AS (
  SELECT
    position_name,
    collect_list(struct(keyword_count as count, keyword)) as kw_structs
  FROM
    counted_data
  GROUP BY
    position_name
)
SELECT
  position_name,
  to_json(
    transform(
      sort_array(kw_structs, false),
      x -> map(x.keyword, x.count)
    )
  ) as keyword_counts_json
FROM
  structured_data
"""

# 执行优化后的查询
result_df = spark.sql(final_sql)

# 显示结果
print(f"Aggregated and sorted keyword counts for position '{final_position_name}':")
result_df.show()

# 清理缓存，释放内存
keywords_df.unpersist()

# 关闭 Spark 会话
spark.stop()
