#!/usr/bin/env bash
# 构建柠檬ERP 的后端、前端镜像，可选推送到镜像仓库
#
#   ./docker/build.sh                                  # 构建 lemon-erp-backend:latest、lemon-erp-web:latest
#   ./docker/build.sh -t 1.0.0                         # 指定标签
#   ./docker/build.sh -p registry.example.com/lemon/lemon-erp -t 1.0.0 --push
#   ./docker/build.sh --npm-registry https://registry.npmmirror.com
#   ./docker/build.sh --platform linux/amd64           # 在 Apple 芯片的电脑上构建给 x86 服务器使用的镜像
set -euo pipefail

cd "$(dirname "$0")/.."

PREFIX=lemon-erp
TAG=latest
PUSH=false
NPM_REGISTRY=
PLATFORM=

usage() {
  sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [ $# -gt 0 ]; do
  case "$1" in
    -p|--prefix) PREFIX="$2"; shift 2 ;;
    -t|--tag) TAG="$2"; shift 2 ;;
    --push) PUSH=true; shift ;;
    --npm-registry) NPM_REGISTRY="$2"; shift 2 ;;
    --platform) PLATFORM="$2"; shift 2 ;;
    -h|--help) usage 0 ;;
    *) echo "未知参数：$1"; usage 1 ;;
  esac
done

BUILD=(docker build)
if [ -n "$PLATFORM" ]; then
  BUILD=(docker buildx build --platform "$PLATFORM" --load)
fi

echo "==> 构建后端镜像 ${PREFIX}-backend:${TAG}"
"${BUILD[@]}" -f docker/backend.Dockerfile -t "${PREFIX}-backend:${TAG}" .

echo "==> 构建前端镜像 ${PREFIX}-web:${TAG}"
"${BUILD[@]}" -f docker/web.Dockerfile --build-arg NPM_REGISTRY="$NPM_REGISTRY" -t "${PREFIX}-web:${TAG}" .

if [ "$PUSH" = true ]; then
  echo "==> 推送镜像"
  docker push "${PREFIX}-backend:${TAG}"
  docker push "${PREFIX}-web:${TAG}"
fi

echo "完成：${PREFIX}-backend:${TAG}、${PREFIX}-web:${TAG}"
