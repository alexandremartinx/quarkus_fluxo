FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl common-dto,etl-processor-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /work
COPY --from=build /workspace/etl-processor-service/target/ /work/
RUN useradd -r -u 1001 quarkus && chown -R quarkus:quarkus /work
USER quarkus
EXPOSE 8082
ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
CMD sh -c '\
  if [ -f /work/quarkus-app/quarkus-run.jar ]; then \
    exec java $JAVA_OPTS -jar /work/quarkus-app/quarkus-run.jar; \
  elif ls /work/*-runner.jar >/dev/null 2>&1; then \
    exec java $JAVA_OPTS -jar /work/*-runner.jar; \
  elif ls /work/*.jar >/dev/null 2>&1; then \
    exec java $JAVA_OPTS -jar $(ls -1 /work/*.jar | head -n1); \
  else \
    echo "Nenhum JAR executável encontrado em /work" >&2; ls -la /work >&2; exit 1; \
  fi'
