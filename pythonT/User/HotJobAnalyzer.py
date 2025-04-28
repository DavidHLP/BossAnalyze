from pyspark.sql import SparkSession

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
    .select("id", "position_name","city_name")  # 只选择需要的列

df.createOrReplaceTempView("HotCityAnalyzer")

# 使用Spark SQL查询数据
result = spark.sql("""
    SELECT position_name, count(*) as position_count
    FROM HotCityAnalyzer
    GROUP BY position_name
    ORDER BY position_count DESC
    LIMIT 10
    """)

result.show()