# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Install banking-common
COPY banking-common/pom.xml ./banking-common/pom.xml
COPY banking-common/src ./banking-common/src
RUN mvn install -f banking-common/pom.xml -DskipTests

# Build service
COPY banking-discovery-server/pom.xml ./banking-discovery-server/pom.xml
RUN mvn dependency:go-offline -B -f banking-discovery-server/pom.xml
COPY banking-discovery-server/src ./banking-discovery-server/src
RUN mvn clean package -DskipTests -f banking-discovery-server/pom.xml

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/banking-discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]