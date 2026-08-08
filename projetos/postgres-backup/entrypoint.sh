#!/bin/sh
# ============================================================
#  postgres-backup/entrypoint.sh
#  Loop simples (sem cron daemon) que roda o backup.sh a cada
#  BACKUP_INTERVAL_SECONDS, começando por um backup imediato.
# ============================================================
set -eu

INTERVAL="${BACKUP_INTERVAL_SECONDS:-86400}"

while true; do
  /usr/local/bin/backup.sh || echo "[$(date -Iseconds)] Backup falhou, tentando de novo no próximo ciclo."
  echo "[$(date -Iseconds)] Próximo backup em ${INTERVAL}s."
  sleep "$INTERVAL"
done
