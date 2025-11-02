FROM maven:3.9.8-eclipse-temurin-21 AS build
COPY pom.xml /app/pom.xml
WORKDIR /app
RUN mvn dependency:go-offline
COPY src /app/src
RUN mvn clean install -DskipTests

FROM openjdk:21-slim
COPY --from=build /app/target/batch-app-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
