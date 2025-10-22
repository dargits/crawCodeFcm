# Stage 1: Build stage
# SỬ DỤNG IMAGE CÓ CẢ MAVEN VÀ JDK để lệnh 'mvn' được tìm thấy.
FROM maven:3.8.2-jdk-17 AS builder 

WORKDIR /app

# Copy pom.xml và tải dependencies để tối ưu hóa cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy code source và build ứng dụng
COPY src ./src
# Lệnh build bằng Maven
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------------

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

# Install Chromium và dependencies cho Selenium
RUN apt-get update && apt-get install -y \
    chromium-browser \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR từ build stage (từ stage 'builder')
COPY --from=builder /app/target/*.jar app.jar

# Expose port (mặc định của Spring Boot)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/crawl/codes || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]