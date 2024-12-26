# 使用基于 Debian Bullseye 的 OpenJDK 17 镜像
FROM openjdk:17-jdk-bullseye

# 安装 Maven + Docker CLI (docker.io)
# docker.io为了让后端springboot项目能够在里面启动docker容器
RUN apt-get update && \
    apt-get install -y maven docker.io && \
    rm -rf /var/lib/apt/lists/*

# 容器内工作目录改到 /app
WORKDIR /app

# 复制 pom.xml 并离线下载依赖
COPY pom.xml /app
RUN mvn dependency:go-offline

# 复制源码
COPY src /app/src

# 打包 Spring Boot 应用（跳过测试）
RUN mvn clean package -DskipTests

# 暴露端口 8080
EXPOSE 8080

# 启动 Spring Boot
CMD ["java", "-jar", "target/Backend-0.0.1-SNAPSHOT.jar"]
