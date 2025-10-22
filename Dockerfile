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
# Sử dụng JRE/JDK cơ bản làm môi trường chạy
FROM openjdk:17-jdk-slim

# CÀI ĐẶT CHROMIUM VÀ DEPENDENCIES CHO SELENIUM
# openjdk:17-jdk-slim dựa trên Debian/Ubuntu nên ta dùng apt-get
RUN apt-get update && apt-get install -y \
    chromium-browser \
    wget \
    curl \
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
```
eof

---

### Lưu ý quan trọng cho việc triển khai trên Render

Vì bạn đang dùng Selenium và Chromium trong môi trường container không có giao diện (headless) trên Render, bạn **phải** cấu hình code Spring Boot (Java) của mình để thêm các `ChromeOptions` sau:

```java
// Ví dụ cấu hình trong code Java của bạn
ChromeOptions options = new ChromeOptions();

// Bắt buộc phải thêm các đối số sau để chạy trong container
options.addArguments("--headless");
options.addArguments("--no-sandbox"); // Cần thiết trong môi trường container
options.addArguments("--disable-dev-shm-usage"); // Cần thiết để tránh lỗi bộ nhớ trong container

// ... Khởi tạo ChromeDriver với các options này
// WebDriver driver = new ChromeDriver(options);
