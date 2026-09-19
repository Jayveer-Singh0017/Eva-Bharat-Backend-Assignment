# ============================================================
# Stage 1: Build React/Vite Frontend
# ============================================================
FROM node:22-alpine AS frontend-build

WORKDIR /frontend

# Copy frontend package files
COPY frontend/package*.json ./

# Install dependencies
RUN npm install

# Copy frontend source
COPY frontend/ ./

# Build production frontend
# API calls will use the same origin as the backend
RUN VITE_API_BASE="" npm run build


# ============================================================
# Stage 2: Build Spring Boot Backend
# ============================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS backend-build

WORKDIR /app

# Copy Maven configuration
COPY backend/pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy backend source
COPY backend/src ./src

# Copy React production build into Spring Boot static folder
COPY --from=frontend-build /frontend/dist ./src/main/resources/static

# Build Spring Boot application
RUN mvn clean package -DskipTests -B


# ============================================================
# Stage 3: Production Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S app && adduser -S -G app app

# Copy built Spring Boot JAR
COPY --from=backend-build /app/target/*.jar app.jar

USER app

# Render uses the PORT environment variable
EXPOSE 8080

# Use Render's PORT, fallback to 8080 for local execution
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]