# syntax=docker/dockerfile:1.7
# Multi-stage build: Maven (JDK) -> slim JRE runtime image.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Dependencies in their own layer: re-used from the GitHub Actions layer cache while pom.xml is unchanged.
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=dev
ARG GIT_SHA=local
LABEL org.opencontainers.image.title="biztrips-backend" \
      org.opencontainers.image.description="M324 RefCard05 Business Trips backend (Spring Boot, RDS MariaDB, S3)" \
      org.opencontainers.image.authors="Tim Held" \
      org.opencontainers.image.source="https://github.com/AdminGodZ/bbw-m324-biztrips-fullstack-actions-aws-backend" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_SHA}"
RUN addgroup -g 10001 app && adduser -D -u 10001 -G app app
WORKDIR /app
# Only used by the local storage type (Docker Compose without S3); on ECS photos live in S3.
RUN mkdir uploads && chown app:app uploads
COPY --from=build /build/target/*.jar app.jar
COPY deploy/rds-ca-bundle.crt /app/rds-ca-bundle.crt
USER app
ENV APP_VERSION=${APP_VERSION} \
    GIT_SHA=${GIT_SHA} \
    APP_STORAGE_LOCATION=/app/uploads
EXPOSE 8080
HEALTHCHECK --interval=15s --start-period=90s --timeout=5s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
