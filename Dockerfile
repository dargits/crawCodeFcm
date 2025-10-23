# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build project
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Install Chrome for Selenium (required for web scraping)
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    unzip \
    ca-certificates \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /tmp/*

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/fc-crawler-0.0.1-SNAPSHOT.jar /app/app.jar

# Environment variables
ENV PORT=10000 \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    CHROMEDRIVER_SKIP_DOWNLOAD=true \
    WDM_CHROMEDRIVER_DOWNLOAD_FALLBACK=true

EXPOSE $PORT

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:$PORT/api/crawl || exit 1

# Run Spring Boot application
CMD exec java $JAVA_OPTS -jar /app/app.jar --server.port=$PORT