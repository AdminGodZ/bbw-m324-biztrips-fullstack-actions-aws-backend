FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 10001 app && adduser -D -u 10001 -G app app
WORKDIR /app
RUN mkdir uploads && chown app:app uploads
COPY --from=build /build/target/*.jar app.jar
USER app
ENV APP_STORAGE_LOCATION=/app/uploads
VOLUME ["/app/uploads"]
EXPOSE 8080
HEALTHCHECK --interval=15s --start-period=90s --timeout=5s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
