#!/bin/sh
# ============================================================
#  postgres-backup/backup.sh
#  Roda um pg_dump do banco de produção, comprime e remove dumps
#  mais antigos que BACKUP_RETENTION_DAYS.
#
#  Restore manual (a partir de um dump em /backups):
#    gunzip -c /backups/website_db_AAAAMMDD_HHMMSS.sql.gz | \
#      psql -h postgres -U "$POSTGRES_USER" -d "$POSTGRES_DB"
# ============================================================
set -eu

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/backups
BACKUP_FILE="${BACKUP_DIR}/${POSTGRES_DB}_${TIMESTAMP}.sql.gz"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

mkdir -p "$BACKUP_DIR"

echo "[$(date -Iseconds)] Iniciando backup de ${POSTGRES_DB}..."

TMP_SQL=$(mktemp "${BACKUP_DIR}/.${POSTGRES_DB}_${TIMESTAMP}.XXXXXX.sql")
TMP_BACKUP="${BACKUP_FILE}.tmp"
trap 'rm -f "$TMP_SQL" "$TMP_BACKUP"' EXIT HUP INT TERM

PGPASSWORD="$POSTGRES_PASSWORD" pg_dump \
  -h "$POSTGRES_HOST" \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" \
  --no-owner --no-privileges > "$TMP_SQL"

gzip -c "$TMP_SQL" > "$TMP_BACKUP"
mv "$TMP_BACKUP" "$BACKUP_FILE"
rm -f "$TMP_SQL"
trap - EXIT HUP INT TERM

echo "[$(date -Iseconds)] Backup salvo em ${BACKUP_FILE} ($(du -h "$BACKUP_FILE" | cut -f1))"

echo "[$(date -Iseconds)] Removendo backups com mais de ${RETENTION_DAYS} dias..."
find "$BACKUP_DIR" -name "${POSTGRES_DB}_*.sql.gz" -mtime "+${RETENTION_DAYS}" -print -delete

echo "[$(date -Iseconds)] Backups atuais:"
ls -lh "$BACKUP_DIR"
