# Stage 1: Build stage
# Sử dụng image có sẵn Maven và JDK để build (đã fix lỗi 'mvn' not found)
FROM maven:3-openjdk-17 AS build 

WORKDIR /app

# Copy toàn bộ dự án vào thư mục làm việc
COPY . .

# FIX: Thêm -Dfile.encoding=UTF-8 để khắc phục lỗi biên dịch ký tự đặc biệt
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# -----------------------------------------------------------------------------------

# Stage 2: Runtime stage
# SỬ DỤNG IMAGE TEMURIN DỰA TRÊN UBUNTU ĐỂ CÀI ĐẶT CHROMIUM/SELENIUM
FROM eclipse-temurin:17-jre-focal

# CÀI ĐẶT CHROMIUM VÀ DEPENDENCIES CHO SELENIUM
RUN apt-get update && apt-get install -y \
    chromium-browser \
    wget \
    curl \
    # Thêm các thư viện cần thiết cho môi trường headless
    libgconf-2-4 \
    libnss3 \
    libfontconfig1 \
    libxcomposite1 \
    libxrandr2 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy file WAR từ stage build và đổi tên thành app.war
# Tên file chính xác từ thư mục target: fc-crawler-0.0.1-SNAPSHOT.war
COPY --from=build /app/target/fc-crawler-0.0.1-SNAPSHOT.war app.war

# Expose port mặc định của ứng dụng Spring Boot
EXPOSE 8080

# (Tùy chọn) Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/crawl/codes || exit 1

# Lệnh chạy ứng dụng
# LƯU Ý: Lệnh 'java -jar' vẫn hoạt động nếu WAR của bạn là executable (có Tomcat nhúng).
ENTRYPOINT ["java", "-jar", "app.war"]
