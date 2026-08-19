 # 多阶段构建
# Stage 1: 编译打包
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 下载依赖（利用缓存层）
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: 运行
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区设置
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建导出文件目录
RUN mkdir -p /app/exports /app/logs

# 拷贝构建产物
COPY --from=builder /build/target/personal-ledger.jar /app/app.jar

# 拷贝数据库脚本
COPY --from=builder /build/target/classes/db/schema.sql /app/db/schema.sql

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "/app/app.jar"]
