FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-corporate/pom.xml subgraph-corporate/pom.xml
COPY subgraph-corporate/src subgraph-corporate/src

RUN mvn -pl common,subgraph-corporate -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-corporate/target/subgraph-corporate-*.jar app.jar
EXPOSE 4010
ENTRYPOINT ["java", "-jar", "app.jar"]
