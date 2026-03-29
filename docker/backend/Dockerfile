# ─── 阶段1：Maven 构建 ───────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先只复制 pom.xml，利用 Docker 缓存层加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码并打包（跳过测试加快速度）
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── 阶段2：运行时镜像 ───────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 时区
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制 jar
COPY --from=builder /app/target/*.jar app.jar

# 创建非 root 用户运行
RUN addgroup -S quantnest && adduser -S quantnest -G quantnest
USER quantnest

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]