FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/DocuMind-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]