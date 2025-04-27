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
    .select("id", "position_name","company_name")  # 只选择需要的列

df.createOrReplaceTempView("JobCityRecommender")

# 使用Spark SQL查询数据
result = spark.sql(f"""
                   SELECT company_name , count(company_name) as count 
                   FROM JobCityRecommender 
                   where position_name like '%{inputdata}%' 
                   group by company_name 
                   order by count(company_name) desc 
                   limit {limitdata}
                   """)

result.show()

spark.stop()