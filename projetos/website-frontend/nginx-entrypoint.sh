#!/bin/sh
# ============================================================
#  website-frontend/nginx-entrypoint.sh
#  NGINX não recarrega certificados TLS sozinho quando o arquivo
#  em disco muda. O serviço certbot roda "certbot renew" a cada
#  12h (só substitui o cert quando está de fato perto de expirar);
#  este loop recarrega o NGINX a cada 6h para pegar qualquer
#  renovação sem downtime (SIGHUP = graceful reload).
#
#  Evita a alternativa de dar ao container do certbot acesso ao
#  socket do Docker do host (docker.sock) só para mandar um sinal
#  de reload — isso equivaleria a dar controle root do host ao
#  certbot, um risco desproporcional ao problema.
# ============================================================
set -eu

( while true; do
    sleep 6h
    nginx -s reload
  done ) &

exec nginx -g "daemon off;"
