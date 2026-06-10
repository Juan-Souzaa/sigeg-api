#!/bin/sh
set -eu

UPLOAD_DIR="${APP_UPLOAD_DIR:-/app/uploads}"

mkdir -p "${UPLOAD_DIR}/menus"
chown -R appuser:appuser "${UPLOAD_DIR}"

exec runuser -u appuser -- java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar
