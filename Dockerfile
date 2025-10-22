# Multi-stage Dockerfile for Spring Boot app with headless Chromium
FROM maven:3.9.4-eclipse-temurin-17 as build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
# Install necessary packages for Chromium
RUN apk add --no-cache chromium nss freetype ttf-freefont

COPY --from=build /build/target/fc-crawler-0.0.1-SNAPSHOT.jar /app/fc-crawler.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/fc-crawler.jar"]
