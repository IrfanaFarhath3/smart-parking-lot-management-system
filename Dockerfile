# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/parking-lot-management-1.0-SNAPSHOT.jar app.jar
COPY web ./web

EXPOSE 8080

CMD ["sh", "-c", "java -jar app.jar"]