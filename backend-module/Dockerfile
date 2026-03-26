############################
# Desafio Fullstack – Dockerfile do Backend Spring Boot
# Multi-stage: build com Maven → runtime com JRE slim
############################

# ---- Estágio 1: Build com Maven ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copia o POM raiz e os POMs dos módulos para cache de dependências
COPY pom.xml                        ./pom.xml
COPY ejb-module/pom.xml             ./ejb-module/pom.xml
COPY backend-module/pom.xml         ./backend-module/pom.xml

# Baixa dependências antes de copiar o código (cache eficiente)
RUN mvn dependency:go-offline -B

# Copia o código-fonte
COPY ejb-module/src     ./ejb-module/src
COPY backend-module/src ./backend-module/src

# Build completo (ejb-module → backend-module)
RUN mvn clean package -B -DskipTests

# ---- Estágio 2: Runtime com JRE slim ----
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# curl é usado no healthcheck do container
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuário não-root por segurança
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

# Copia o fat JAR gerado pelo Spring Boot
COPY --from=builder /app/backend-module/target/*.jar app.jar

# Porta da aplicação
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
