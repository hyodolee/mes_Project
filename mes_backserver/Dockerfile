# ── 1단계: 빌드 (gradle로 jar 생성) ───────────────────────────────
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY . .
# 테스트는 외부 DB가 필요해 건너뛰고 실행 jar만 만든다
RUN gradle bootJar -x test --no-daemon

# ── 2단계: 실행 (가벼운 JRE 이미지에 jar만 복사) ──────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul
# 한글 깨짐 방지
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
