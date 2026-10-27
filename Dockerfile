# syntax=docker/dockerfile:1

# 1단계: Java 21과 저장소의 Gradle Wrapper로 실행 JAR 생성
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src/main ./src/main

# 테스트는 CI의 H2·MySQL 작업에서 수행
# 여기서는 실행 파일을 빌드
RUN --mount=type=cache,target=/root/.gradle \
    bash ./gradlew --no-daemon bootJar

# 2단계: JRE 기반 실행 이미지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl은 Compose의 HTTP 상태 확인에 사용
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app

COPY --from=build --chown=app:app /workspace/build/libs/app.jar /app/app.jar

# 애플리케이션은 전용 사용자로 실행
USER app:app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]