FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-notifications/pom.xml subgraph-notifications/pom.xml
COPY subgraph-notifications/src subgraph-notifications/src

RUN mvn -pl common,subgraph-notifications -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-notifications/target/subgraph-notifications-*.jar app.jar
EXPOSE 4009
ENTRYPOINT ["java", "-jar", "app.jar"]
