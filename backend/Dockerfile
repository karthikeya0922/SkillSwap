# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 skillswap && mkdir -p /app/uploads /app/logs && chown skillswap /app/uploads /app/logs
COPY --from=build /app/target/skillswap-backend-*.jar app.jar
USER skillswap
EXPOSE 8080
ENV UPLOADS_DIR=/app/uploads LOG_PATH=/app/logs
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
