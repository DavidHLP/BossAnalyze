from pyspark.sql import SparkSession

inputdata = '数据分析师'
limitdata = 10

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

df.createOrReplaceTempView("JobCityRecommender")

# 使用Spark SQL查询数据，计算占比
result = spark.sql(f"""
                   WITH CompanyCounts AS (
                       SELECT 
                           city_name, 
                           count(city_name) as city_count
                       FROM JobCityRecommender 
                       WHERE position_name like '%{inputdata}%' 
                       GROUP BY city_name
                   ), TotalCount AS (
                       SELECT count(*) as total_count
                       FROM JobCityRecommender
                       WHERE position_name like '%{inputdata}%'
                   )
                   SELECT 
                       cc.city_name,
                       cc.city_count / tc.total_count as proportion
                   FROM CompanyCounts cc
                   CROSS JOIN TotalCount tc
                   ORDER BY proportion DESC
                   LIMIT {limitdata}
                   """)

result.show()

spark.stop()