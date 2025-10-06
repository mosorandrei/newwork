# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src


COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package


FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /src/target/*.jar /app/app.jar
ENV TZ=${TZ:-UTC}
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
