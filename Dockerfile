# Stage 1: Build stage
# Sử dụng image có sẵn Maven và JDK để build (khắc phục lỗi 'mvn' not found)
FROM maven:3-openjdk-17 AS build 

WORKDIR /app

# Copy toàn bộ dự án vào thư mục làm việc
COPY . .

# Chạy lệnh build Maven để tạo ra file JAR thực thi
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------------

# Stage 2: Runtime stage
# SỬ DỤNG IMAGE TEMURIN DỰA TRÊN UBUNTU ĐỂ ĐẢM BẢO VIỆC CÀI ĐẶT CHROMIUM/SELENIUM THÀNH CÔNG
FROM eclipse-temurin:17-jre-focal

# CÀI ĐẶT CHROMIUM VÀ DEPENDENCIES CHO SELENIUM
# Lệnh cài đặt chromium-browser/dependencies trên Ubuntu/Debian
RUN apt-get update && apt-get install -y \
    chromium-browser \
    wget \
    curl \
    # Thêm libgconf-2-4 và các dependencies khác thường gặp cho môi trường headless
    libgconf-2-4 \
    libnss3 \
    libfontconfig1 \
    libxcomposite1 \
    libxrandr2 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy file JAR từ stage build
# Tên file JAR được rút gọn thành fc-crawler.jar
COPY --from=build /app/target/fc-crawler-0.0.1-SNAPSHOT.jar fc-crawler.jar

# Expose port mặc định của ứng dụng Spring Boot
EXPOSE 8080

# (Tùy chọn) Health Check để Render hoặc Docker biết ứng dụng đã sẵn sàng
# Giả định bạn có endpoint Health Check hoặc endpoint crawl để kiểm tra.
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/crawl/codes || exit 1

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "fc-crawler.jar"]
