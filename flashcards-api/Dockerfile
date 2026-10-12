# Dockerfile multi-stage da API de Flashcards.
#
# Build:  docker build -t flashcards-api .
# Uso:    docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod ... flashcards-api

# ---- Estagio 1: Maven (para copiar o binario do maven) ----
FROM maven:3.9-eclipse-temurin-25 AS maven

# ---- Estagio 2: build com JDK 25 ----
FROM eclipse-temurin:25-jdk AS build

COPY --from=maven /usr/share/maven /usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"

WORKDIR /app

# Cache de dependencias
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Estagio 3: runtime (JRE 25) ----
FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
