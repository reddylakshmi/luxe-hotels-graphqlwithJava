FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-content/pom.xml subgraph-content/pom.xml
COPY subgraph-content/src subgraph-content/src

RUN mvn -pl common,subgraph-content -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-content/target/subgraph-content-*.jar app.jar
EXPOSE 4006
ENTRYPOINT ["java", "-jar", "app.jar"]
