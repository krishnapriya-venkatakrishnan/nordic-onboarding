# One container: Angular is built, then served by Spring Boot as static files.

# 1. Frontend
FROM node:24-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npx ng build

# 2. Backend, with the frontend bundled in
FROM maven:3.10-eclipse-temurin-25 AS api
WORKDIR /api
COPY backend/pom.xml .
# Dependencies get their own layer, so a code change doesn't download them all again.
RUN mvn -q -B dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist/frontend/browser src/main/resources/static
# The tests already ran in CI before an image is built.
RUN mvn -q -B package -DskipTests

# 3. Runtime: only a Java runtime and the jar
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=api /api/target/onboarding-0.1.0.jar app.jar
ENV PORT=8080
EXPOSE 8080
# Not root, so a compromised app can't modify the image or anything else it doesn't own.
USER 1000
# Java defaults to a quarter of the container's memory for its heap; small instances need more.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
