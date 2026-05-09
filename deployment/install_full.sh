#!/bin/bash
set -e

COMPOSE="docker compose"
if ! $COMPOSE version >/dev/null 2>&1; then
  COMPOSE="docker-compose"
fi
# 然后用 $COMPOSE 替代所有 docker compose

echo "?? Datalogger Full Offline Installer (database + database-ui + App)"
echo "-------------------------------------------------------------------"

# === 1. 创建目录结构 ===
INSTALL_DIR="$HOME/.datalogger/docker"
mkdir -p "$INSTALL_DIR/backend_data" #防止由docker以root用户自动创建，导致后面clean脚本无法删除
mkdir -p "$INSTALL_DIR/tsdb_data"    #防止由docker以root用户自动创建，导致后面clean脚本无法删除

# === 2. 加载所有 Docker 镜像（假设都在当前目录）===
#echo "?? Loading Docker images..."
# timescaledb
# docker load -i timescaledb.tar >/dev/null
# docker load -i timescaledb-ui.tar >/dev/null


# === 3. 拷贝 compose 文件和前端/后端构建上下文（关键！）===
cp -f docker-compose.yml "$INSTALL_DIR/" 2>/dev/null || {
  echo "? Missing docker-compose.yml in current directory!"
  exit 1
}

# 如果你有预构建的 backend.jar 和 frontend/dist，也拷贝进去
# （否则需在目标机器上构建，或提前 build 并打包 .tar 镜像）
if [ -d ./backend ]; then
  cp -r ./backend "$INSTALL_DIR/"
fi
if [ -d ./frontend ]; then
  cp -r ./frontend "$INSTALL_DIR/"
fi

# === 4. 进入配置目录 ===
cd "$INSTALL_DIR"

# === 5. 启动 timescaledb ===
echo "?? Starting timescaledb..."
$COMPOSE up -d timescaledb

# 等待就绪
echo "? Waiting for timescaledb to be ready..."
for i in {1..20}; do
  if docker logs datalogger-opcua-database 2>&1 | grep -q "server started"; then
    break
  fi
  sleep 1
done


# === 8. 启动其余所有服务（Backend + Frontend + ui）===
echo "?? Starting all remaining services..."
$COMPOSE up -d

# === 9. 等待后端就绪（可选，增强健壮性）===
echo "? Waiting for backend to start..."
for i in {1..30}; do
  if docker logs datalogger-opcua-backend 2>&1 | grep -q "(Server startup|Started .*Application)"; then
    break
  fi
  sleep 2
done


# === 10. 输出最终信息 ===
echo
echo "?? Datalogger is fully ready!"
echo
echo "?? Access Points:"
echo "   ?? Application Frontend: http://localhost:1234"
echo "   ?? TimeScaleDb Explorer:   http://localhost:8082"
echo
echo "?? Files saved in: $INSTALL_DIR"
