FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY subgraph-loyalty/pom.xml subgraph-loyalty/pom.xml
COPY subgraph-loyalty/src subgraph-loyalty/src

RUN mvn -pl common,subgraph-loyalty -am -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/subgraph-loyalty/target/subgraph-loyalty-*.jar app.jar
EXPOSE 4005
ENTRYPOINT ["java", "-jar", "app.jar"]
