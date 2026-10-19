# syntax=docker/dockerfile:1

# Build the executable Spring Boot JAR.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the Maven descriptor first so this layer is cached until dependencies change.
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# Use a small JRE-only image in production.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring \
    && mkdir /app/logs \
    && chown -R spring:spring /app
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
