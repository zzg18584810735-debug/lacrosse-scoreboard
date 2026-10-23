#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env.test"
IMAGE_REPOSITORY="hub.lo/jx/lascore"
SERVICE_NAME="msdev_lascore_dev"
CONTAINER_NAME="lacrosse-scoreboard"
APP_HOST_PORT="${APP_HOST_PORT:-5173}"
DATA_DIR="${SCRIPT_DIR}/data"
BACK_JAR="${SCRIPT_DIR}/back/lacrosse-scoreboard.jar"
FRONT_INDEX="${SCRIPT_DIR}/frond/dist/index.html"

if ! command -v docker >/dev/null 2>&1; then
  echo "[错误] 服务器尚未安装 Docker" >&2
  exit 1
fi

for required in "${SCRIPT_DIR}/Dockerfile" "${BACK_JAR}" "${FRONT_INDEX}"; do
  if [[ ! -f "${required}" ]]; then
    echo "[错误] 缺少镜像构建文件：${required}" >&2
    exit 1
  fi
done

if ! read -r -p "请输入镜像版本号（例如 0.1）：" IMAGE_VERSION; then
  echo "[错误] 无法读取镜像版本号" >&2
  exit 1
fi
if [[ -z "${IMAGE_VERSION}" ]]; then
  echo "[错误] 镜像版本号不能为空" >&2
  exit 1
fi
if [[ ! "${IMAGE_VERSION}" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]*$ ]] || (( ${#IMAGE_VERSION} > 128 )); then
  echo "[错误] 镜像版本号格式不正确：${IMAGE_VERSION}" >&2
  exit 1
fi
IMAGE_NAME="${IMAGE_REPOSITORY}:${IMAGE_VERSION}"

cd "${SCRIPT_DIR}"

echo "[构建] 使用 back 和 frond 构建前后端镜像 ${IMAGE_NAME}"
docker build -t "${IMAGE_NAME}" "${SCRIPT_DIR}"

UPDATE_SERVICE="${UPDATE_SERVICE:-}"
if [[ -z "${UPDATE_SERVICE}" ]]; then
  read -r -p "是否更新 Docker Service ${SERVICE_NAME}？[y/N] " UPDATE_SERVICE || UPDATE_SERVICE="n"
fi

case "${UPDATE_SERVICE}" in
  y|Y|yes|YES|Yes)
    if ! docker service inspect "${SERVICE_NAME}" >/dev/null 2>&1; then
      echo "[错误] Docker Service 不存在：${SERVICE_NAME}" >&2
      exit 1
    fi
    echo "[Service] 更新 ${SERVICE_NAME} 使用镜像 ${IMAGE_NAME}"
    docker service update --image "${IMAGE_NAME}" --force "${SERVICE_NAME}"
    echo "[日志] ${SERVICE_NAME}"
    docker service logs "${SERVICE_NAME}"
    exit 0
    ;;
esac

START_CONTAINER="${START_CONTAINER:-}"
if [[ -z "${START_CONTAINER}" ]]; then
  read -r -p "是否立即启动普通 Docker 容器？[y/N] " START_CONTAINER || START_CONTAINER="n"
fi

case "${START_CONTAINER}" in
  y|Y|yes|YES|Yes)
    ;;
  *)
    echo "镜像已构建，本次未启动任何容器：${IMAGE_NAME}"
    exit 0
    ;;
esac

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[错误] 启动普通容器需要环境文件：${ENV_FILE}" >&2
  exit 1
fi

echo "[普通容器] 更新前后端容器"
if docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
  docker rm -f "${CONTAINER_NAME}" >/dev/null
fi
mkdir -p "${DATA_DIR}/logs"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --add-host host.docker.internal:host-gateway \
  --env-file "${ENV_FILE}" \
  -p "${APP_HOST_PORT}:80" \
  -v "${DATA_DIR}/logs:/app/logs" \
  "${IMAGE_NAME}" >/dev/null

echo
echo "部署完成"
echo "访问地址：http://服务器IP:${APP_HOST_PORT}"
echo "查看日志：docker logs -f ${CONTAINER_NAME}"
