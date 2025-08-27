# Quarkus Microservices – Comparador de Preços (RabbitMQ, Redis, MongoDB, Delta Lake)

> Monorepo de referência com **4 microserviços Quarkus**: ingestão de TXT → Delta Lake, ETL para MongoDB, comparação de preços por **produto**, comparação por **categoria**; **RabbitMQ** para assíncrono e **Redis** para baixa latência. Inclui `docker-compose` completo.

---

## Visão geral da arquitetura

```
┌─────────────────────┐      events: prices.parsed       ┌───────────────────────┐
│ file-ingest-service │ ─────────────────────────────────▶│ etl-processor-service │
│  (TXT → Delta + MQ) │                                   │  (Rabbit → MongoDB)   │
└─────────┬───────────┘                                   └───────────┬───────────┘
          │  escreve Parquet + log Delta                               │ escreve docs
          ▼                                                             ▼
     [MinIO/S3]  «Delta Lake»                                     [MongoDB]
          ▲                                                             ▲
          │                                                             │
          └───────────────────────────────┬──────────────────────────────┘
                                          │
                     ┌────────────────────┴────────────────────┐
                     │                                         │
        ┌──────────────────────────────┐          ┌──────────────────────────────┐
        │ product-compare-service      │          │ category-compare-service     │
        │ (REST + Redis cache + Mongo) │          │ (REST + Redis cache + Mongo) │
        └──────────────────────────────┘          └──────────────────────────────┘
```

* **file-ingest-service**: recebe/varre arquivos `.txt`, faz *parse*, **escreve em Delta Lake** (S3/MinIO) e publica evento RabbitMQ `prices.parsed` por linha válida.
* **etl-processor-service**: consome `prices.parsed`, normaliza e **persiste no MongoDB** (coleção `prices`), mantendo histórico.
* **product-compare-service**: endpoint REST para **comparar preços por EAN/GTIN**, faz *lookup* no Mongo com **cache no Redis**.
* **category-compare-service**: endpoint REST para comparação por **categoria**, agregações no MongoDB + cache Redis.

> **Observação** sobre Delta Lake: este blueprint usa **Delta Standalone** (sem Spark) para commitar arquivos Parquet e o *transaction log*. Para ambientes produtivos com volume alto, você pode preferir Spark jobs *batch/streaming* (gatilhados pelo MQ) — a interface do restante do sistema não muda.

---

## Pré-requisitos

* **Java 17+**, **Maven 3.9+**
* **Quarkus CLI** (opcional, facilita): `sdk install quarkus` ou via Homebrew/SDKMAN
* **Docker** + **docker-compose**

---

## Estrutura do repositório

```
price-compare-monorepo/
├─ docker-compose.yml               # RabbitMQ, MongoDB, Redis, MinIO (S3 compat.)
├─ common-dto/                      # DTOs e contratos de eventos
│  ├─ pom.xml
│  └─ src/main/java/com/acme/common/
│     ├─ dto/PriceRecord.java
│     └─ events/PriceParsedEvent.java
├─ file-ingest-service/
│  ├─ pom.xml
│  ├─ src/main/resources/application.properties
│  └─ src/main/java/com/acme/ingest/
│     ├─ IngestResource.java         # Upload/trigger
│     ├─ FolderWatcher.java          # Opcional: watch de pasta
│     ├─ ParserService.java          # Parse do TXT
│     ├─ DeltaWriter.java            # Escrita em Delta Lake (MinIO/S3)
│     └─ MQEmitter.java              # Publica eventos no RabbitMQ
├─ etl-processor-service/
│  ├─ pom.xml
│  ├─ src/main/resources/application.properties
│  └─ src/main/java/com/acme/etl/
│     ├─ PriceDoc.java               # Entidade/Documento Mongo
│     ├─ PriceRepository.java        # Repositório Mongo (Panache)
│     └─ PriceConsumer.java          # Consumer Rabbit → grava no Mongo
├─ product-compare-service/
│  ├─ pom.xml
│  ├─ src/main/resources/application.properties
│  └─ src/main/java/com/acme/product/
│     ├─ CompareResource.java        # /compare/product/{ean}
│     └─ RedisCache.java             # Cache (Quarkus Redis)
└─ category-compare-service/
   ├─ pom.xml
   ├─ src/main/resources/application.properties
   └─ src/main/java/com/acme/category/
      ├─ CategoryCompareResource.java# /compare/category/{name}
      └─ RedisCache.java
```