# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Install banking-common
COPY banking-common/pom.xml ./banking-common/pom.xml
COPY banking-common/src ./banking-common/src
RUN mvn install -f banking-common/pom.xml -DskipTests

# Build service — no dependency cache, always fresh
COPY banking-api-gateway/pom.xml ./banking-api-gateway/pom.xml
COPY banking-api-gateway/src ./banking-api-gateway/src
RUN mvn clean package -DskipTests -f banking-api-gateway/pom.xml

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/banking-api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]