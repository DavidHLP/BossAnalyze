FROM ubuntu:22.04

# 环境变量设置
ENV HADOOP_HOME=/opt/hadoop
ENV HBASE_HOME=/opt/hbase
ENV SPARK_HOME=/opt/spark
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# 以 root 用户执行
USER root

# 更新并安装依赖包
RUN apt-get update && \
    apt-get install -y sudo openjdk-8-jdk openssh-server openssh-client \
                       wget curl vim net-tools telnet && \
    ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa && \
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys && \
    chmod 0600 ~/.ssh/authorized_keys && \
    mkdir -p /data/hdfs && \
    mkdir -p /data/hdfs/journal/node/local/data

# 下载并安装Miniconda
RUN wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O /tmp/miniconda.sh && \
    chmod +x /tmp/miniconda.sh && \
    /tmp/miniconda.sh -b -p /opt/miniconda3 && \
    rm /tmp/miniconda.sh

# 设置Miniconda环境变量
ENV PATH="/opt/miniconda3/bin:$PATH"

# 创建并配置pyspark conda环境
RUN conda create -n pyspark python=3.8 -y && \
    echo "conda activate pyspark" >> ~/.bashrc

# 设置默认conda环境
ENV CONDA_DEFAULT_ENV=pyspark

# 配置Spark环境变量到/etc/profile
RUN echo 'export SPARK_HOME=/opt/spark' >> /etc/profile && \
    echo 'export PYSPARK_PYTHON=/opt/miniconda3/envs/pyspark/bin/python' >> /etc/profile && \
    echo 'export PYSPARK_DRIVER_PYTHON=/opt/miniconda3/envs/pyspark/bin/python' >> /etc/profile && \
    echo 'export PATH=$SPARK_HOME/bin:$SPARK_HOME/sbin:$PATH' >> /etc/profile

# 启动 SSH 服务
RUN service ssh start

# 暴露端口
EXPOSE 9870 9868 9864 9866 8088 8020 16000 16010 16020 7077 8080 8081 22

# 容器启动时启动 SSH
CMD ["/usr/sbin/sshd", "-D"]