# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the backend directory contents to /app
# We use the root context to access the backend folder
COPY backend/ .

# Grant execution permissions to gradlew
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Render/Back4app dynamic port support
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
