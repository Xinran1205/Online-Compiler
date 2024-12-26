# 给沙箱用的镜像，基于 Debian Bullseye + OpenJDK17
FROM openjdk:17-jdk-bullseye

# 安装 Python3
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /workspace

# 不复制任何 Spring 项目的代码，只是一个“运行环境”
CMD ["bash"]
         