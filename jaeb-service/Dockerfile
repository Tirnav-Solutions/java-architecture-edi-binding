# ==============================
# JAEB EDI Middleware Service
# ==============================
# Multi-stage build for minimal image size

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app

# Copy parent pom and all modules
COPY pom.xml .
COPY jaeb-core/pom.xml jaeb-core/pom.xml
COPY jaeb-model-x12/pom.xml jaeb-model-x12/pom.xml
COPY jaeb-service/pom.xml jaeb-service/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl jaeb-service -am -B

# Copy source code
COPY jaeb-core/src jaeb-core/src
COPY jaeb-model-x12/src jaeb-model-x12/src
COPY jaeb-service/src jaeb-service/src

# Build the service JAR
RUN mvn clean package -pl jaeb-service -am -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

COPY --from=build /app/jaeb-service/target/jaeb-service-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/edi/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
