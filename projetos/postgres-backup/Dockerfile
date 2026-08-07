# ============================================================
#  postgres-backup/Dockerfile
#  Sidecar que roda pg_dump periodicamente contra o serviço
#  "postgres" e mantém apenas os N dumps mais recentes (retenção
#  configurável). Endereça o ponto pendente #9 do README
#  ("Sem backup/restore automatizado para o PostgreSQL em produção").
# ============================================================

FROM postgres:16-alpine

COPY backup.sh /usr/local/bin/backup.sh
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
