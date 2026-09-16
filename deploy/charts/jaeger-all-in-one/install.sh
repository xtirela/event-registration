#!/usr/bin/env bash
# Standalone Jaeger all-in-one (UI + OTLP collector + in-memory storage).
# NOT part of the umbrella chart; deployed separately like kube-prometheus-stack.
# event-service / event-registration-service export traces to
# http://jaeger:4318/v1/traces via their chart's otlpEndpoint value.
set -euo pipefail

CHART_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

helm repo add jaeger-all-in-one https://raw.githubusercontent.com/hansehe/jaeger-all-in-one/master/helm/charts >/dev/null 2>&1 || true
helm repo update jaeger-all-in-one >/dev/null

helm upgrade --install jaeger jaeger-all-in-one/jaeger-all-in-one \
  --version 0.1.12 \
  --namespace eventreg --create-namespace \
  --values "$CHART_DIR/values.yaml" \
  "$@"