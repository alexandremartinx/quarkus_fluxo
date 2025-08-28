# -------- Stage 1: Build --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
# Compila apenas o módulo do serviço e dependências do monorepo
RUN mvn -q -DskipTests -pl common-dto,file-ingest-service -am package

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:17-jre
WORKDIR /work
# Copia TUDO de target (independe de fast-jar/uber-jar)
COPY --from=build /workspace/file-ingest-service/target/ /work/
RUN useradd -r -u 1001 quarkus && chown -R quarkus:quarkus /work
USER quarkus
EXPOSE 8081
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
