FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-reservations/pom.xml subgraph-reservations/pom.xml
COPY subgraph-reservations/src subgraph-reservations/src

RUN mvn -pl common,subgraph-reservations -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-reservations/target/subgraph-reservations-*.jar app.jar
EXPOSE 4004
ENTRYPOINT ["java", "-jar", "app.jar"]
