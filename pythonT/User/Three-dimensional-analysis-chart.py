from pyspark.sql import SparkSession
from pyspark.sql.functions import col, from_json, udf
from pyspark.sql.types import StructType, StructField, StringType, IntegerType
import re

# 创建Spark会话
spark = SparkSession.builder.appName("SalaryDegreeAnalyze")\
    .config("spark.jars.packages", "mysql:mysql-connector-java:8.0.28") \
    .config("spark.sql.adaptive.enabled", "true") \
    .config("spark.sql.shuffle.partitions", "10") \
    .getOrCreate()

# 定义更完整的JSON结构以提取更多字段
json_schema = StructType([
    StructField("basicInfo", StructType([
        StructField("salary", StringType(), True),
        StructField("degree", StringType(), True),
        StructField("experience", StringType(), True),
        StructField("address", StringType(), True),
    ]), True),
    StructField("companyInfo", StructType([
        StructField("companyName", StringType(), True),
        StructField("companySize", StringType(), True),
        StructField("industry", StringType(), True),
        StructField("financingStage", StringType(), True)
    ]), True),
    StructField("companyUrl", StringType(), True)
])

# 读取数据并进行解析和过滤
job_df = spark.read.format("jdbc")\
    .option("url", "jdbc:mysql://localhost:3306/boss_data")\
    .option("driver", "com.mysql.cj.jdbc.Driver")\
    .option("user", "root")\
    .option("password", "Alone117")\
    .option("dbtable", "t_job_detail")\
    .option("fetchsize", "10000") \
    .load()\
    .select("position_id", "position_name", "detail_data", "city_name")\
    .withColumn("jsonData", from_json(col("detail_data"), json_schema))\
    .select(
        col("position_id"),
        col("position_name"),
        col("city_name"),
        col("jsonData.basicInfo.salary").alias("salary"),
        col("jsonData.basicInfo.degree").alias("degree"),
        col("jsonData.basicInfo.experience").alias("experience"),
        col("jsonData.basicInfo.address").alias("address"),
        col("jsonData.companyInfo.companyName").alias("companyName"),
        col("jsonData.companyInfo.companySize").alias("companySize"),
        col("jsonData.companyInfo.industry").alias("industry"),
        col("jsonData.companyInfo.financingStage").alias("financingStage"),
        col("jsonData.companyUrl").alias("companyUrl")
    )\
    .filter(col("salary").isNotNull() & col("degree").isNotNull())

# 提取薪资数值 - 只处理"4-5K"和"4K"两种格式，其他格式忽略
def extract_salary(salary_str):
    if not salary_str:
        return None
    
    try:
        # 只处理格式为"数字K"或"数字-数字K"的情况
        if re.match(r'^\d+K$', salary_str):
            # 单一数值，如"4K"
            return int(salary_str.replace('K', ''))
        elif re.match(r'^\d+-\d+K$', salary_str):
            # 范围值，如"4-5K"
            parts = salary_str.replace('K', '').split('-')
            lower = int(parts[0])
            upper = int(parts[1])
            return (lower + upper) // 2
        else:
            # 其他格式忽略
            return None
    except Exception as e:
        print(f"Error processing salary: {salary_str}, Error: {e}")
        return None

extract_salary_udf = udf(extract_salary, IntegerType())
job_df = job_df.withColumn("salary_value", extract_salary_udf(col("salary")))

# 经验映射
def map_experience(exp_str):
    if not exp_str:
        return 0
    if '应届' in exp_str:
        return 0
    if '无需' in exp_str or '不限' in exp_str:
        return 0
    if '1年以内' in exp_str:
        return 1
    if '1-3' in exp_str:
        return 2
    if '3-5' in exp_str:
        return 4
    if '5-10' in exp_str:
        return 7
    if '10年以上' in exp_str:
        return 10
    return 0

map_experience_udf = udf(map_experience, IntegerType())
job_df = job_df.withColumn("experience_value", map_experience_udf(col("experience")))

# 学历映射
def map_degree(degree_str):
    if not degree_str:
        return 0
    if '不限' in degree_str:
        return 0
    if '初中' in degree_str:
        return 1
    if '高中' in degree_str:
        return 2
    if '中专' in degree_str:
        return 2
    if '大专' in degree_str:
        return 3
    if '本科' in degree_str:
        return 4
    if '硕士' in degree_str:
        return 5
    if '博士' in degree_str:
        return 6
    return 0

map_degree_udf = udf(map_degree, IntegerType())
job_df = job_df.withColumn("degree_value", map_degree_udf(col("degree")))

# 为ECharts准备数据，包括所有需要的维度
echarts_df = job_df.select(
    "position_id", 
    "position_name", 
    "city_name", 
    "salary", 
    "salary_value", 
    "degree", 
    "degree_value", 
    "experience", 
    "experience_value", 
    "companyName", 
    "companySize", 
    "industry", 
    "financingStage", 
    "companyUrl"
)

echarts_df.show()