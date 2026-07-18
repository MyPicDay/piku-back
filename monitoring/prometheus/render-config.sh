#!/bin/sh

set -eu

template=${PROMETHEUS_CONFIG_TEMPLATE:-/etc/prometheus/prometheus.yml.template}
output=${PROMETHEUS_CONFIG_OUTPUT:-/tmp/prometheus.yml}
prometheus_bin=${PROMETHEUS_BIN:-/bin/prometheus}
target=${MONITORING_TARGET:-host.docker.internal:8080}

escaped_target=$(printf '%s' "$target" | sed 's/[&|]/\\&/g')
sed "s|__MONITORING_TARGET__|$escaped_target|g" "$template" > "$output"

exec "$prometheus_bin" "$@"
