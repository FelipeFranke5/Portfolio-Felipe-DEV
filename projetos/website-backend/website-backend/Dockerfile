# ============================================================
#  website-backend/Dockerfile
#  Build multi-stage: compila com Maven e roda apenas o JRE.
#  Usado pelo docker-compose.yml (dev) para builds locais.
# ============================================================

# ---------- Stage 1: build ----------
FROM maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e AS build
WORKDIR /build

# Copia primeiro só o pom.xml e baixa as dependências.
# Isso aproveita o cache de camadas do Docker: enquanto o pom.xml não mudar,
# essa camada não é refeita, mesmo que o código-fonte mude (build fica bem mais rápido).
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Agora copia o código-fonte e builda o artefato final.
COPY src ./src
RUN mvn -B clean package -DskipTests


# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c AS runtime
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8085

# healthcheck usando o endpoint do Actuator (já exposto no application.yml)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8085/actuator/health | grep -q '"status":"UP"' || exit 1


ENTRYPOINT ["java", "-jar", "app.jar"]
