# syntax=docker/dockerfile:1
FROM maven:3.10.0-eclipse-temurin-21-noble AS build
ARG SERVICE
WORKDIR /build
COPY ${SERVICE}/pom.xml ./pom.xml
COPY ${SERVICE}/src ./src
# Context tests require running infrastructure; execute them with Verify.ps1.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system app && useradd --system --gid app --create-home app \
    && mkdir -p /app/data/tareas && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/*-SNAPSHOT.jar /app/app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/app.jar"]
