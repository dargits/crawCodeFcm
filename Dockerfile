# Stage 1: Build stage
# Đặt tên cho stage là 'builder' để có thể tham chiếu từ stage sau
FROM openjdk:17-jdk-alpine AS builder

WORKDIR /app

# Copy pom.xml và tải dependencies để tối ưu hóa cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy code source và build ứng dụng
COPY src ./src
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------------

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre

# Install Chromium và dependencies cho Selenium
# Chromium thường được dùng thay cho Google Chrome trên các base image nhỏ gọn (Alpine/Slim)
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

# Health check (tùy chọn, Render có thể dùng cho dịch vụ Web Service)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/crawl/codes || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]