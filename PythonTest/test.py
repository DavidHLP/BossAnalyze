from pyspark.sql import SparkSession
from pyspark.sql.functions import *
import requests
from pyspark.sql.functions import udf
import ipaddress
from typing import List, Union, Optional

def is_ip_in_networks(ip_str: str, networks: List[str]) -> bool:
    """
    检查IP地址是否在指定的网络列表中
    
    Args:
        ip_str: 要检查的IP地址字符串
        networks: 网络地址列表，可以是单个IP或CIDR表示法
        
    Returns:
        bool: 如果IP在任何指定的网络中则返回True，否则返回False
    """
    try:
        ip = ipaddress.ip_address(ip_str)
        for network_str in networks:
            try:
                network = ipaddress.ip_network(network_str, strict=False)
                if ip in network:
                    return True
            except ValueError:
                # 如果网络格式无效，跳过
                continue
    except ValueError:
        # 如果IP地址格式无效，返回False
        pass
    return False

def get_ip_type(ip_str: str) -> Optional[str]:
    """
    获取IP地址类型描述
    
    Args:
        ip_str: IP地址字符串
        
    Returns:
        str: IP地址类型描述，如果无法识别则返回None
    """
    try:
        ip = ipaddress.ip_address(ip_str)
        
        if ip.is_loopback:
            return "本地回环地址"
        elif ip.is_private:
            return "私有地址"
        elif ip.is_link_local:
            return "链路本地地址"
        elif ip.is_multicast:
            return "组播地址"
        elif ip.is_unspecified:
            return "未指定地址"
        else:
            return "公网地址"
    except ValueError:
        return None

API_KEY = "266a9419b23942ebb641cb20985db574"
# 需要过滤的IP地址列表，包括本地回环地址和私有地址段
FILTER_IP = [
    # IPv4 回环地址
    "127.0.0.1",      # 标准本地回环
    "127.0.0.0/8",    # 整个127.0.0.0/8都是回环地址
    
    # IPv6 回环地址
    "::1",
    "0:0:0:0:0:0:0:1",
    
    # 私有地址段
    "10.0.0.0/8",      # 10.0.0.0 - 10.255.255.255
    "172.16.0.0/12",   # 172.16.0.0 - 172.31.255.255
    "192.168.0.0/16",  # 192.168.0.0 - 192.168.255.255
    "169.254.0.0/16",  # 链路本地地址
    
    # 组播地址
    "224.0.0.0/4",
    "ff00::/8"
]

spark = SparkSession.builder.appName("test").getOrCreate()
df = spark.read.text("hdfs://hadoop-single:9000/logs")

filtered_df = df.filter(df.value.contains("ACCESS|ts=")).select(
    regexp_extract("value", r"ACCESS\|ts=(\d+)", 1).alias("timestamp"),
    regexp_extract("value", r"ip=([^|]+)", 1).alias("ip"),
    regexp_extract("value", r"path=([^|]+)", 1).alias("path"),
    regexp_extract("value", r"method=([^|]+)", 1).alias("method"),
    regexp_extract("value", r"ua=([^|]+)", 1).alias("user_agent"),
)
based_df = filtered_df.select(
    from_unixtime(col("timestamp") / 1000, "yyyy-MM-dd HH:mm:ss").alias("date"),
    "ip",
    "path",
    "method",
    "user_agent",
).filter(col("method") != "OPTIONS")

# 访问热力图分析 细粒度为分钟
heat_map_df = (
    based_df.withColumn("date", date_format(col("date"), "yyyy-MM-dd HH:mm"))
    .groupBy("date")
    .count()
    .sort("count", ascending=False)
)
heat_map_df.show(5)

# 访问热力图分析 细粒度为小时
heat_map_df = (
    based_df.withColumn("date", date_format(col("date"), "yyyy-MM-dd HH"))
    .groupBy("date")
    .count()
    .sort("count", ascending=False)
)
heat_map_df.show(5)

# IP地理分布分析
unique_ips = based_df.select("ip").distinct()
ip_list = [row["ip"] for row in unique_ips.collect()]

geo_data = []
for ip in ip_list:
    if is_ip_in_networks(ip, FILTER_IP):
        ip_type = get_ip_type(ip) or "未知类型"
        geo_data.append(
            {
                "ip": ip,
                "country_name": "本地网络",
                "city": f"{ip_type} ({ip})",
            }
        )
        continue
    else:
        url = f"https://api.ipgeolocation.io/ipgeo?ip={ip}&apiKey={API_KEY}"
        try:
            response = requests.get(url, timeout=1)
            data = response.json()
            geo_data.append(
                {
                    "ip": ip,
                    "country_name": data.get("country_name", ""),
                    "city": data.get("city", ""),
                }
            )
        except Exception as e:
            geo_data.append({"ip": ip, "country_name": "", "city": ""})

geo_df = spark.createDataFrame(geo_data)

ip_count_df = based_df.groupBy("ip").agg(count("*").alias("count"))

final_df = ip_count_df.join(geo_df, on="ip", how="left")
final_df.select("ip", "count", "country_name", "city").show(5)

# HTTP方法分布
http_method_df = based_df.groupBy("method").agg(count("*").alias("count"))
http_method_df.orderBy("count", ascending=False).show(5)

# 工作日/周末访问分布
weekday_df = based_df.withColumn(
    "weekday", dayofweek(col("date")).alias("weekday")
).groupBy("weekday").agg(count("*").alias("count"))
weekday_df.orderBy("weekday").show(5)

# 用户代理分析
browser_df = based_df.withColumn(
    "browser",
    when(
        col("user_agent").contains("Chrome"), "Chrome"
    ).when(
        col("user_agent").contains("Firefox"), "Firefox"
    ).when(
        col("user_agent").contains("Safari"), "Safari"
    ).when(
        col("user_agent").contains("Opera"), "Opera"
    ).when(
        col("user_agent").contains("Edge"), "Edge"
    ).when(
        col("user_agent").contains("IE"), "IE"
    ).otherwise("其他")
)
browser_df.groupBy("browser").agg(count("*").alias("count")).orderBy("count", ascending=False).show(5)