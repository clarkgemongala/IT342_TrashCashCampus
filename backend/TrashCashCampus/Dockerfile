FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY --from=build /app/target/TrashCashCampus-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
# To use Firebase with environment variables, set FIREBASE_CONFIG with the JSON content as a single line
# Example: docker run -e FIREBASE_CONFIG='{"type":"service_account","project_id":"...",...}' your-image
ENTRYPOINT ["java", "-jar", "/app/app.jar"]