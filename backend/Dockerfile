# 使用基于 Debian Bullseye 的 OpenJDK 17 镜像
FROM openjdk:17-jdk-bullseye

# 更新包列表并安装 Python3, pip 和 Maven
RUN apt-get update && \
    apt-get install -y python3 python3-pip maven && \
    rm -rf /var/lib/apt/lists/*

# 在容器内创建应用目录
WORKDIR /app

# 复制 pom.xml 并下载依赖
COPY pom.xml /app
RUN mvn dependency:go-offline

# 复制源码
COPY src /app/src

# 打包应用（跳过测试）
RUN mvn clean package -DskipTests

# 暴露端口 8080
EXPOSE 8080

# 运行 Spring Boot 应用
CMD ["java", "-jar", "target/Backend-0.0.1-SNAPSHOT.jar"]
