# syntax=docker/dockerfile:1

# ---------- Build stage ----------
# Uses the project's Maven wrapper so the build is reproducible and pinned.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the wrapper + POM first so dependency resolution is cached as its own layer.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Now copy sources and build the (executable, layered) jar. Tests run in CI
# before the image is built (they need Docker for Testcontainers), so skip here.
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests \
    && cp target/spring-mcp-server-*.jar /app/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl is used by the container HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /app/app.jar /app/app.jar
USER app

EXPOSE 8080

# Surface the Spring Boot actuator health endpoint to the Docker runtime.
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
