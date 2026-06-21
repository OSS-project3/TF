# syntax=docker/dockerfile:1
# JAR는 호스트에서 미리 빌드 (./gradlew :backend:bootJar -x test)
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY backend/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
