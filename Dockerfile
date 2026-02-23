# -------------------------
# Stage 1: Build
# -------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests


# -------------------------
# Stage 2: Runtime
# -------------------------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy built jar (wildcard avoids hardcoding name)
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Render provides PORT env variable
ENTRYPOINT ["sh","-c","java -jar app.jar --server.port=${PORT:-8080}"]
