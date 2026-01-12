FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/bullhorn-mock-service-1.0.0.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
