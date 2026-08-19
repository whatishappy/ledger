#!/bin/bash
# 个人云端记账本 - 部署脚本
# 目标 VM: 192.168.10.101（已有 Docker + MySQL 8.0 + Redis）

set -e

VM_HOST="192.168.10.101"
VM_USER="${VM_USER:-root}"
PROJECT_DIR="/opt/ledger"

echo "========== 个人云端记账本部署 =========="
echo "目标主机: ${VM_HOST}"
echo "部署目录: ${PROJECT_DIR}"

# 1. 在 VM 上创建部署目录
echo "[1/5] 创建部署目录..."
ssh ${VM_USER}@${VM_HOST} "mkdir -p ${PROJECT_DIR}/exports ${PROJECT_DIR}/logs ${PROJECT_DIR}/monitoring/grafana/provisioning/datasources ${PROJECT_DIR}/monitoring/grafana/provisioning/dashboards"

# 2. 拷贝部署文件到 VM
echo "[2/5] 拷贝部署文件..."
scp Dockerfile ${VM_USER}@${VM_HOST}:${PROJECT_DIR}/
scp docker-compose.yml ${VM_USER}@${VM_HOST}:${PROJECT_DIR}/
scp -r monitoring/* ${VM_USER}@${VM_HOST}:${PROJECT_DIR}/monitoring/

# 3. 在 VM 上构建并启动
echo "[3/5] 构建并启动容器..."
ssh ${VM_USER}@${VM_HOST} "cd ${PROJECT_DIR} && docker-compose build && docker-compose up -d"

# 4. 等待应用启动
echo "[4/5] 等待应用启动..."
sleep 30

# 5. 健康检查
echo "[5/5] 健康检查..."
ssh ${VM_USER}@${VM_HOST} "curl -s http://localhost:8080/actuator/health || echo '应用未就绪，请检查日志'"

echo ""
echo "========== 部署完成 =========="
echo "应用地址: http://${VM_HOST}:8080/doc.html"
echo "Grafana:  http://${VM_HOST}:3000"
echo "Prometheus: http://${VM_HOST}:9090"
echo ""
echo "查看日志: ssh ${VM_USER}@${VM_HOST} 'cd ${PROJECT_DIR} && docker-compose logs -f app'"
