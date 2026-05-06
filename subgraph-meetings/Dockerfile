FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-meetings/pom.xml subgraph-meetings/pom.xml
COPY subgraph-meetings/src subgraph-meetings/src

RUN mvn -pl common,subgraph-meetings -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-meetings/target/subgraph-meetings-*.jar app.jar
EXPOSE 4008
ENTRYPOINT ["java", "-jar", "app.jar"]
