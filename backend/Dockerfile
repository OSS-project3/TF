# syntax=docker/dockerfile:1

# ---- 1단계: Gradle 빌드 (gradlew 래퍼가 없어 gradle 이미지 사용) ----
FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace

# 의존성 캐시 레이어 (빌드 스크립트만 먼저 복사)
COPY settings.gradle ./
COPY backend/build.gradle ./backend/
RUN gradle :backend:dependencies --no-daemon || true

# 소스 복사 후 실행 가능한 bootJar 생성 (테스트는 이미지 빌드 시 생략)
COPY backend/src ./backend/src
RUN gradle :backend:bootJar --no-daemon -x test

# ---- 2단계: 실행 (경량 JRE) ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""
# bootJar만 매칭됨 (plain jar는 -SNAPSHOT-plain.jar 라 제외)
COPY --from=build /workspace/backend/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
