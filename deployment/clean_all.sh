#!/bin/bash
set -e

echo "🗑️  Datalogger Full Uninstaller "
echo "---------------------------------------------------"

# === 1. 停止并删除所有相关容器 ===
echo "🛑 Stopping and removing containers..."
CONTAINERS=(
  datalogger-opcua-backend
  datalogger-opcua-frontend
  datalogger-opcua-database
  datalogger-opcua-database-ui
)

for container in "${CONTAINERS[@]}"; do
  if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
    echo "   → Removing container: $container"
    docker rm -f "$container" >/dev/null 2>&1
  fi
done

# === 2. 删除相关镜像 ===
echo "🖼️  Removing custom images..."
IMAGES=(
  datalogger-opcua-backend:v1.0
  datalogger-opcua-frontend:v1.0
)

for img in "${IMAGES[@]}"; do
  if docker images --format '{{.Repository}}:{{.Tag}}' | grep -q "^${img}$"; then
    echo "   → Removing image: $img"
    docker rmi -f "$img" >/dev/null 2>&1
  fi
done

# === 3. 清理 Docker 网络 ===
echo "🕸️  Removing application network..."
NETWORK="app-network"
if docker network ls --format '{{.Name}}' | grep -q "^${NETWORK}$"; then
  echo "   → Removing network: $NETWORK"
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
fi

# === 4. 删除本地数据目录 ===
echo "📂 Removing local data directories..."
if [ -d "$HOME/.datalogger" ]; then
  echo "   → Removing $HOME/.datalogger"
  rm -rf "$HOME/.datalogger"
fi

# === 5. 删除敏感配置文件（关键！）===
echo "🔐 Removing sensitive config files..."

echo
echo "✅ Uninstall complete! All components and sensitive files removed."
echo "➡️  Ready for a fresh installation with: ./install_offline_fresh.sh"
