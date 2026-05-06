FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-experiences/pom.xml subgraph-experiences/pom.xml
COPY subgraph-experiences/src subgraph-experiences/src

RUN mvn -pl common,subgraph-experiences -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-experiences/target/subgraph-experiences-*.jar app.jar
EXPOSE 4007
ENTRYPOINT ["java", "-jar", "app.jar"]
