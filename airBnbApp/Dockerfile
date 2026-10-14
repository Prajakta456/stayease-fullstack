# ---- build: compile and package the Spring Boot jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Download dependencies first so they are cached until pom.xml changes
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar app.jar

# ---- runtime: slim JRE, non-root user ----
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app stayease
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER stayease

EXPOSE 8080
# Readiness only turns UP after startup work (Flyway migrations, demo seeding) has finished
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fs http://localhost:8080/api/v1/actuator/health/readiness || exit 1

# Size the heap from the container's memory limit
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
