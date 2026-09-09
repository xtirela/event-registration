FROM eclipse-temurin:25 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew gradlew
COPY src src
RUN chmod +x gradlew && ./gradlew build installDist -x test

FROM eclipse-temurin:25
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]