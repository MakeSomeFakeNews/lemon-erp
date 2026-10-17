# 前端镜像（Nginx + 前端静态文件，/api 转发到后端）：构建上下文为项目根目录
#   docker build -f docker/web.Dockerfile -t lemon-erp-web .

# ---------- 构建 ----------
FROM node:22-alpine AS build
# 国内网络可以指定 npm 镜像：--build-arg NPM_REGISTRY=https://registry.npmmirror.com
ARG NPM_REGISTRY=
WORKDIR /web
COPY lemon-web/package.json lemon-web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    if [ -n "$NPM_REGISTRY" ]; then npm config set registry "$NPM_REGISTRY"; fi \
    && npm ci
COPY lemon-web/ ./
RUN npm run build

# ---------- 运行 ----------
FROM nginx:1.27-alpine
ENV TZ=Asia/Shanghai
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /web/dist /usr/share/nginx/html
EXPOSE 80
