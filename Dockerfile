FROM eclipse-temurin:25 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew gradlew
COPY src src
RUN chmod +x gradlew && ./gradlew build installDist -x test

FROM eclipse-temurin:25 AS jre-builder
RUN $JAVA_HOME/bin/jlink \
      --add-modules java.base,java.xml,java.logging,java.sql,java.naming,java.management,java.desktop \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /javarunti

FROM debian:bookworm-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-builder /javaruntime $JAVA_HOME
WORKDIR /app
COPY --from=builder /app/build/install/event-registration/ /app/
ENTRYPOINT ["/app/bin/event-registration"]