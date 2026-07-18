#!/bin/sh

set -eu

REPOSITORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPOSITORY_ROOT"

normalize_lines() {
  sed '/^$/d' | LC_ALL=C sort | paste -sd ' ' -
}

compose_config() {
  output_option=$1
  shift

  GOOGLE_APPLICATION_CREDENTIALS=validation-credentials.json \
    docker compose --env-file /dev/null "$@" config --no-env-resolution "$output_option"
}

assert_service_set() {
  label=$1
  expected_services=$2
  shift 2

  actual=$(compose_config --services "$@" | normalize_lines)
  expected=$(printf '%s\n' $expected_services | normalize_lines)

  if [ "$actual" != "$expected" ]; then
    printf '%s services mismatch\nexpected: %s\nactual:   %s\n' \
      "$label" "$expected" "$actual" >&2
    exit 1
  fi
}

assert_no_profiles() {
  label=$1
  shift

  profiles=$(compose_config --profiles "$@" | normalize_lines)
  if [ -n "$profiles" ]; then
    printf '%s must not declare profiles: %s\n' "$label" "$profiles" >&2
    exit 1
  fi
}

validate_core() {
  env_file_reset=$(mktemp)
  trap 'rm -f "$env_file_reset"' EXIT HUP INT TERM
  printf 'services:\n  app:\n    env_file: !reset []\n' > "$env_file_reset"

  dev_files="-f docker-compose.dev.yml -f docker-compose.infra.yml -f $env_file_reset"
  prod_files="-f docker-compose.prod.yml -f docker-compose.infra.yml -f $env_file_reset"

  # Word splitting is intentional so each Compose flag is passed separately.
  # shellcheck disable=SC2086
  compose_config --quiet $dev_files
  # shellcheck disable=SC2086
  assert_service_set "dev + infra" "app db minio minio-provision redis" $dev_files
  # shellcheck disable=SC2086
  assert_no_profiles "dev + infra" $dev_files

  # shellcheck disable=SC2086
  compose_config --quiet $prod_files
  # shellcheck disable=SC2086
  assert_service_set "prod + infra" "app minio minio-provision prod-db redis" $prod_files
  # shellcheck disable=SC2086
  assert_no_profiles "prod + infra" $prod_files

  rm -f "$env_file_reset"
  trap - EXIT HUP INT TERM
}

assert_rendered_target() {
  target=$1
  rendered_config=$(mktemp)
  trap 'rm -f "$rendered_config"' EXIT HUP INT TERM

  MONITORING_TARGET="$target" \
    PROMETHEUS_CONFIG_TEMPLATE=monitoring/prometheus/prometheus.yml \
    PROMETHEUS_CONFIG_OUTPUT="$rendered_config" \
    PROMETHEUS_BIN=/usr/bin/true \
    /bin/sh monitoring/prometheus/render-config.sh

  if ! grep -F -- "$target" "$rendered_config" >/dev/null; then
    printf 'rendered Prometheus config does not contain target: %s\n' "$target" >&2
    exit 1
  fi

  if grep -F -- "__MONITORING_TARGET__" "$rendered_config" >/dev/null; then
    printf 'rendered Prometheus config still contains the target placeholder\n' >&2
    exit 1
  fi

  rm -f "$rendered_config"
  trap - EXIT HUP INT TERM
}

validate_monitor() {
  monitor_files="-f docker-compose.monitor.yml"

  # shellcheck disable=SC2086
  compose_config --quiet $monitor_files
  # shellcheck disable=SC2086
  assert_service_set "monitor" "grafana prometheus" $monitor_files
  # shellcheck disable=SC2086
  assert_no_profiles "monitor" $monitor_files

  # shellcheck disable=SC2086
  networks=$(compose_config --networks $monitor_files | normalize_lines)
  if [ "$networks" != "default" ]; then
    printf 'monitor must use only its default network: %s\n' "$networks" >&2
    exit 1
  fi

  assert_rendered_target "host.docker.internal:8080"
  assert_rendered_target "192.168.0.10:18080"
}

case "${1:-all}" in
  core)
    validate_core
    ;;
  monitor)
    validate_monitor
    ;;
  all)
    validate_core
    validate_monitor
    ;;
  *)
    printf 'usage: %s [core|monitor|all]\n' "$0" >&2
    exit 2
    ;;
esac

printf 'Docker Compose validation passed (%s)\n' "${1:-all}"
