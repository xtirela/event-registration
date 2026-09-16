#!/usr/bin/env bash
# Standalone kube-prometheus-stack (Prometheus + Grafana + Alertmanager).
# NOT part of the umbrella chart: it ships ~5MB of CRDs+dashboards that blow
# the 1MB Helm release-secret limit. Run BEFORE the umbrella (its
# ServiceMonitors need the monitoring.coreos.com CRDs this creates).
set -euo pipefail

CHART_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

helm upgrade --install kube-prometheus-stack \
  oci://ghcr.io/prometheus-community/charts/kube-prometheus-stack \
  --version 90.1.1 \
  --namespace eventreg --create-namespace \
  --values "$CHART_DIR/values.yaml" \
  "$@"