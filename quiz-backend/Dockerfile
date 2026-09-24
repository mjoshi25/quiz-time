# ==============================
# Build stage
# ==============================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies first for better Docker caching
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests


# ==============================
# Runtime stage
# ==============================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy generated JAR
COPY --from=build /app/target/*.jar app.jar

# Render provides the PORT environment variable.
# Spring Boot will use 8080 locally unless PORT is provided.
ENV PORT=8080

EXPOSE 8080

# Start Spring Boot
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]