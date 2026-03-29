# ─── 阶段1：Node.js 构建 ────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制 package.json，利用缓存层
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# 复制源码并构建
COPY . .
RUN npm run build

# ─── 阶段2：Nginx 托管 ──────────────────────────────
FROM nginx:1.25-alpine

# 时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 复制构建产物（Vite 默认输出 dist，CRA 是 build，按实际修改）
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx 配置由 docker-compose volumes 挂载，这里不重复复制
EXPOSE 80