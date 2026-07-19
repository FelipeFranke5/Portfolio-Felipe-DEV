# ============================================================
#  website-frontend/Dockerfile
#  Imagem de desenvolvimento: instala as dependências e roda
#  `ng serve` com hot reload. Usado pelo docker-compose.yml (dev).
# ============================================================

FROM node:22-alpine
WORKDIR /app

# Copia primeiro só os manifestos e instala as dependências.
# Aproveita o cache de camadas do Docker: enquanto package.json e
# package-lock.json não mudarem, essa camada não é refeita.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 4200

ENTRYPOINT ["npm", "start", "--", "--host", "0.0.0.0"]
