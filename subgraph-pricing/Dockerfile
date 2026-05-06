FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-pricing/pom.xml subgraph-pricing/pom.xml
COPY subgraph-pricing/src subgraph-pricing/src

RUN mvn -pl common,subgraph-pricing -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-pricing/target/subgraph-pricing-*.jar app.jar
EXPOSE 4003
ENTRYPOINT ["java", "-jar", "app.jar"]
