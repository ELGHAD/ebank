FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace/app

COPY .mvn/ .mvn/
COPY pom.xml ./
COPY src ./src

RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/app/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
