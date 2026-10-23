#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PACKAGE_DIR="${SCRIPT_DIR}/app-score"
FRONT_SOURCE="${PROJECT_ROOT}/la-scoreboard-vue"
FRONT_OUTPUT="${PACKAGE_DIR}/frond/dist"
BACK_OUTPUT="${PACKAGE_DIR}/back/lacrosse-scoreboard.jar"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[错误] 未找到命令：$1" >&2
    exit 1
  fi
}

require_command npm
require_command mvn

echo "[1/4] 运行 Vue 测试并构建 la-scoreboard-vue"
(
  cd "${FRONT_SOURCE}"
  npm test
  npm run build
)

echo "[2/4] 运行 Java 测试"
(
  cd "${PROJECT_ROOT}"
  mvn -pl edus-core test
)

echo "[3/4] 打包 Java 后端"
(
  cd "${PROJECT_ROOT}"
  mvn -pl edus-core clean package -DskipTests
)

JAR_SOURCE="${PROJECT_ROOT}/edus-core/target/lacrosse-scoreboard.jar"
if [[ ! -f "${JAR_SOURCE}" ]]; then
  echo "[错误] 未找到后端 JAR：${JAR_SOURCE}" >&2
  exit 1
fi

case "${FRONT_OUTPUT}" in
  "${PACKAGE_DIR}"/*) ;;
  *) echo "[错误] 前端输出目录不在部署目录内" >&2; exit 1 ;;
esac

echo "[4/4] 更新可上传目录"
rm -rf -- "${FRONT_OUTPUT}"
mkdir -p "${FRONT_OUTPUT}" "$(dirname "${BACK_OUTPUT}")"
cp -R "${FRONT_SOURCE}/dist/." "${FRONT_OUTPUT}/"
cp "${JAR_SOURCE}" "${BACK_OUTPUT}"

echo
echo "打包完成：${PACKAGE_DIR}"
echo "请将 app-score 目录整体上传到服务器，在该目录执行：chmod +x deploy.sh && ./deploy.sh"
