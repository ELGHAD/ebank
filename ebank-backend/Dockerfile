FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace/app

# Dependencies first so they are cached between source changes
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system ebank && useradd --system --gid ebank --no-create-home ebank
WORKDIR /app
COPY --from=build /workspace/app/target/ebank-backend-*.jar app.jar
USER ebank

EXPOSE 8080
# Generous start period: a slow or memory-constrained host can take over 2 minutes to start (observed: 140 s).
# A success during the start period marks the container healthy at once, so normal starts are not delayed.
HEALTHCHECK --interval=10s --timeout=3s --start-period=180s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
# JVM flags can be passed with the standard JAVA_TOOL_OPTIONS environment variable
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
