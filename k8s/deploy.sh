#!/usr/bin/env bash
# Implanta o sistema no cluster do kubectl atual (kind local ou o do CI).
# Uso: k8s/deploy.sh [tag-das-imagens]   (padrao: latest)
set -euo pipefail
cd "$(dirname "$0")/.."

TAG="${1:-latest}"
NS=biblioteca

kubectl apply -f k8s/00-namespace.yaml

# configuracao do Grafana vem da mesma pasta usada pelo docker compose
kubectl -n "$NS" create configmap grafana-datasources \
  --from-file=observability/grafana/provisioning/datasources --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$NS" create configmap grafana-dashboard-provider \
  --from-file=observability/grafana/provisioning/dashboards --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$NS" create configmap grafana-dashboards \
  --from-file=observability/grafana/dashboards --dry-run=client -o yaml | kubectl apply -f -

for manifest in k8s/[0-9]*.yaml; do
  sed "s#\(ghcr.io/gustacassel/[a-z-]*\):latest#\1:${TAG}#" "$manifest" | kubectl apply -f -
done

echo "Aguardando os rollouts..."
for sts in library-db students-db rabbitmq; do
  kubectl -n "$NS" rollout status statefulset/"$sts" --timeout=300s
done
for deploy in zipkin loki grafana library-api students-api frontend api-gateway; do
  kubectl -n "$NS" rollout status deployment/"$deploy" --timeout=600s
done

kubectl -n "$NS" get pods -o wide
