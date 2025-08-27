# -------- Stage 1: Build --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
# Copia TODO o monorepo para permitir -pl ... -am
COPY . .
# Compila apenas o que é necessário para este serviço (e dependências)
RUN mvn -q -DskipTests -pl common-dto,file-ingest-service -am package

# -------- Stage 2: Runtime (JRE slim) --------
FROM eclipse-temurin:17-jre
WORKDIR /work
# Copia app Quarkus empacotado
COPY --from=build /workspace/file-ingest-service/target/quarkus-app/ /work/
# Usuário não-root
RUN useradd -r -u 1001 quarkus && chown -R quarkus:quarkus /work
USER quarkus
EXPOSE 8081
ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
# Dica: use variáveis/props para apontar para hostnames do compose (rabbitmq, minio)
CMD ["java","-jar","/work/quarkus-run.jar"]
