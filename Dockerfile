# Build stage
FROM maven:3.6.0-jdk-11-slim AS build
ENV HOME=/home/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD pom.xml $HOME

RUN mkdir -p /root/.m2 && mkdir /root/.m2/repository
# Copy maven settings, containing repository configurations
COPY settings.xml /root/.m2

RUN mvn -s /root/.m2/settings.xml dependency:go-offline

ADD /src $HOME/src
RUN mvn package -s /root/.m2/settings.xml -DskipTests=true

# Package stage
FROM openjdk:12-alpine
ENV HOME=/home/app
ENV export $(cat .env | xargs)
WORKDIR $HOME
COPY --from=build $HOME/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]