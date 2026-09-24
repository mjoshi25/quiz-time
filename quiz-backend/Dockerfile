# ==========================================
# Build stage
# ==========================================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven project
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy application source
COPY src ./src

# Build Spring Boot application
RUN mvn clean package -DskipTests


# ==========================================
# Runtime stage
# ==========================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy generated Spring Boot JAR
COPY --from=build /app/target/*.jar app.jar

# Render supplies PORT automatically
ENV PORT=8080

EXPOSE 8080

# Start application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]