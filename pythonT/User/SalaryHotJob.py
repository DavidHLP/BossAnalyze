from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col, regexp_extract, when
from pyspark.sql.types import StructType, StructField, StringType, IntegerType

# 创建Spark会话
spark = SparkSession.builder.appName("SalaryAnalyze")\
    .config("spark.jars.packages", "mysql:mysql-connector-java:8.0.28") \
    .config("spark.sql.adaptive.enabled", "true") \
    .config("spark.sql.shuffle.partitions", "10") \
    .getOrCreate()

# JSON结构定义
json_schema = StructType([
    StructField("basicInfo", StructType([
        StructField("salary", StringType(), True)
    ]), True)
])

try:
    # 一步完成数据读取、解析和过滤
    salary_df = spark.read.format("jdbc")\
        .option("url", "jdbc:mysql://localhost:3306/boss_data")\
        .option("driver", "com.mysql.cj.jdbc.Driver")\
        .option("user", "root")\
        .option("password", "Alone117")\
        .option("dbtable", "t_job_detail")\
        .option("fetchsize", "10000") \
        .load()\
        .select("position_name", "detail_data", "city_name")\
        .withColumn("jsonData", from_json(col("detail_data"), json_schema))\
        .select(
            col("position_name"),
            col("city_name"),
            col("jsonData.basicInfo.salary").alias("salary")
        )\
        .filter(
            (col("salary").rlike("^\\d+-\\d+K$") | col("salary").rlike("^\\d+K$")) &
            col("salary").isNotNull()
        )\
        .withColumn(
            "min_salary",
            when(
                col("salary").rlike("^\\d+-\\d+K$"),
                regexp_extract(col("salary"), "^(\\d+)-\\d+K$", 1).cast(IntegerType())
            ).otherwise(
                regexp_extract(col("salary"), "^(\\d+)K$", 1).cast(IntegerType())
            )
        )\
        .withColumn(
            "max_salary",
            when(
                col("salary").rlike("^\\d+-\\d+K$"),
                regexp_extract(col("salary"), "^\\d+-(\\d+)K$", 1).cast(IntegerType())
            ).otherwise(
                regexp_extract(col("salary"), "^(\\d+)K$", 1).cast(IntegerType())
            )
        )\
        .cache()

    # 注册视图
    salary_df.createOrReplaceTempView("salary_view")

    # 执行合并查询 - 高薪职位排名及推荐城市
    combined_df = spark.sql("""
    WITH position_avg_salary AS (
        SELECT
            position_name,
            COUNT(*) as job_count,
            MIN(min_salary) as min_salary,
            MAX(max_salary) as max_salary,
            ROUND((AVG(min_salary) + AVG(max_salary))/2, 1) as avg_salary
        FROM
            salary_view
        GROUP BY
            position_name
        HAVING
            COUNT(*) >= 10
    ),
    city_avg_salary AS (
        SELECT
            position_name,
            city_name,
            COUNT(*) as city_job_count,
            ROUND((AVG(min_salary) + AVG(max_salary))/2, 1) as city_avg_salary,
            ROW_NUMBER() OVER (PARTITION BY position_name ORDER BY (AVG(min_salary) + AVG(max_salary))/2 DESC) as rank
        FROM
            salary_view
        GROUP BY
            position_name, city_name
        HAVING
            COUNT(*) >= 5
    )
    SELECT
        p.position_name,
        p.job_count,
        p.min_salary,
        p.max_salary,
        p.avg_salary,
        c.city_name as recommended_city,
        c.city_avg_salary as recommended_city_salary,
        c.city_job_count as recommended_city_job_count
    FROM
        position_avg_salary p
    LEFT JOIN
        city_avg_salary c ON p.position_name = c.position_name AND c.rank = 1
    ORDER BY
        p.avg_salary DESC
    LIMIT 50
    """)

    print("薪水最高的50个职位及其推荐城市:")
    combined_df.show(50, truncate=False)

finally:
    # 清理资源
    if 'salary_df' in locals():
        salary_df.unpersist()
    spark.stop()
