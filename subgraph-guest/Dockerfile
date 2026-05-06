FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-guest/pom.xml subgraph-guest/pom.xml
COPY subgraph-guest/src subgraph-guest/src

RUN mvn -pl common,subgraph-guest -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-guest/target/subgraph-guest-*.jar app.jar
EXPOSE 4002
ENTRYPOINT ["java", "-jar", "app.jar"]
