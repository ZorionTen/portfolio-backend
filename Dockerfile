FROM maven:3.9.11-eclipse-temurin-17-alpine AS build

WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine

ARG BUILD_TIMESTAMP
ENV BUILD_TIMESTAMP=${BUILD_TIMESTAMP}

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/portfolio-backend-*.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
