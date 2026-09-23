# Render (Docker runtime) — Spring Boot 21, built with the project's Maven wrapper.
# Stage 1: compile + package the jar.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests package

# Stage 2: slim runtime, runs the repackaged Boot jar.
# server.port=${PORT:8080} in application.properties already honors Render's $PORT.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["sh", "-c", "java -jar app.jar"]
