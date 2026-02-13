# Build stage
FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

# Run stage
FROM openjdk:17-jdk-slim
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/spring-boot-application.jar"]
