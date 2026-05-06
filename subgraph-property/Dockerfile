FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-property/pom.xml subgraph-property/pom.xml
COPY subgraph-property/src subgraph-property/src

RUN mvn -pl common,subgraph-property -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-property/target/subgraph-property-*.jar app.jar
EXPOSE 4001
ENTRYPOINT ["java", "-jar", "app.jar"]
