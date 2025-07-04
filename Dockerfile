FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/jira-1.0.jar app.jar
COPY src/main/resources/ /app/resources/
ENTRYPOINT ["java", "-jar", "app.jar"]
