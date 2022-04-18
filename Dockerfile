# Build stage
FROM maven:3.6.0-jdk-11-slim AS build
ENV HOME=/home/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD pom.xml $HOME
ARG username
ARG token
RUN echo $username
RUN echo $token
COPY /settings.xml $HOME/settings.xml 
RUN filename=$HOME/settings.xml
RUN sed -i "s/USERNAME/$username/g" $HOME/settings.xml
RUN sed -i "s/TOKEN/$token/g" $HOME/settings.xml
RUN cat $HOME/settings.xml

RUN mvn -s $HOME/settings.xml dependency:go-offline

ADD /src $HOME/src
RUN mvn package -s $HOME/settings.xml -DskipTests=true

# Package stage
FROM openjdk:12-alpine
ENV HOME=/home/app
ENV export $(cat .env | xargs)
WORKDIR $HOME
COPY --from=build $HOME/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
