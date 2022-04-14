# Build stage
#FROM maven:3.6.0-jdk-11-slim AS build
#RUN mvn -s /settings.xml dependency:go-offline
#RUN mvn package -s /settings.xml -DskipTests=true

# Package stage
FROM openjdk:12-alpine
ARG JAR_FILE=target/*.jar
RUN echo $JAR_FILE
RUN pwd
COPY ${JAR_FILE} app.jar
#COPY --from=build target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]