#!/usr/bin/env bash

#================================================================================
# 1. Java 和 Hadoop 相关环境变量
#================================================================================
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export HADOOP_HOME=/opt/hadoop
export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop
export YARN_CONF_DIR=/opt/hadoop/etc/hadoop

#================================================================================
# 2. PySpark 相关配置 (如果使用)
#================================================================================
export PYSPARK_PYTHON=/opt/miniconda3/envs/pyspark/bin/python
export PYSPARK_DRIVER_PYTHON=/opt/miniconda3/envs/pyspark/bin/python

#================================================================================
# 4. Spark Worker 节点配置
#    这些配置只在 Worker 节点上生效
#================================================================================
export SPARK_WORKER_CORES=1
export SPARK_WORKER_MEMORY=1g
export SPARK_WORKER_PORT=7078
export SPARK_WORKER_WEBUI_PORT=8081

#================================================================================
# 5. 【重要】为 Worker 指定所有 Master 的地址
#    Worker 启动时会读取此变量来找到 Master
#================================================================================
export SPARK_MASTER_URL="spark://hadoop-master1:7077,hadoop-master2:7077,hadoop-master3:7077"