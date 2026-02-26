FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*-all.jar app.jar
RUN mkdir -p /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
