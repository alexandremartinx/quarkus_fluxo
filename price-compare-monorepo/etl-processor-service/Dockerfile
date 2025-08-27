# -------- Stage 1: Build --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl common-dto,etl-processor-service -am package

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:17-jre
WORKDIR /work
COPY --from=build /workspace/etl-processor-service/target/quarkus-app/ /work/
RUN useradd -r -u 1001 quarkus && chown -R quarkus:quarkus /work
USER quarkus
EXPOSE 8082
ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
CMD ["java","-jar","/work/quarkus-run.jar"]
