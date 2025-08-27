# Quarkus Microservices – Comparador de Preços

Monorepo com **4 microserviços Quarkus** para ingestão, processamento e comparação de preços, com arquitetura **assíncrona** usando **RabbitMQ**, **MongoDB**, **Redis** e **Delta Lake** (via MinIO/S3).

---

## Arquitetura Geral

```s
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

---

## Microserviços

| Serviço                    | Porta | Função                                  |
|---------------------------|-------|---------------------------------------|
| file-ingest-service       | 8081  | Recebe TXT, grava Delta Lake e envia eventos RabbitMQ |
| etl-processor-service     | 8082  | Consome eventos, processa dados e persiste no MongoDB |
| product-compare-service   | 8083  | Compara preços por produto, com cache Redis          |
| category-compare-service  | 8084  | Compara preços por categoria, com cache Redis       |

---

## Pré-requisitos

- **Java 17+**
- **Maven 3.9+**
- **Docker + Docker Compose**

---

## Subindo a infraestrutura local

Na raiz do projeto:

```bash
docker compose up -d
```

### Serviços locais

| Serviço     | URL / Endereço                 |
|------------|--------------------------------|
| RabbitMQ   | `http://localhost:15672` (guest/guest) |
| MongoDB    | `mongodb://localhost:27017`    |
| Redis      | `redis://localhost:6379`       |
| MinIO      | `http://localhost:9001` (minio/minio123) |

---

## Build local com Maven

Antes de rodar, compile os **DTOs compartilhados**:

```bash
mvn -q -DskipTests -pl common-dto -am install
```

Suba cada serviço no modo dev:

```bash
mvn -q -f file-ingest-service/pom.xml quarkus:dev
mvn -q -f etl-processor-service/pom.xml quarkus:dev
mvn -q -f product-compare-service/pom.xml quarkus:dev
mvn -q -f category-compare-service/pom.xml quarkus:dev
```

---

## Build das imagens Docker

Na raiz do monorepo, execute:

```bash
docker build -f file-ingest-service/Dockerfile -t pc-file-ingest:dev .
docker build -f etl-processor-service/Dockerfile -t pc-etl:dev .
docker build -f product-compare-service/Dockerfile -t pc-compare-prod:dev .
docker build -f category-compare-service/Dockerfile -t pc-compare-cat:dev .
```

---

## Executando com Docker

Use a mesma **network** criada pelo Docker Compose:

### **file-ingest-service**
```bash
docker run --rm --network=price-compare-monorepo_default -p 8081:8081   -e MP_MESSAGING_OUTGOING_PRICES_PARSED_HOST=rabbitmq   -e MP_MESSAGING_OUTGOING_PRICES_PARSED_PORT=5672   -e MP_MESSAGING_OUTGOING_PRICES_PARSED_USERNAME=guest   -e MP_MESSAGING_OUTGOING_PRICES_PARSED_PASSWORD=guest   -e S3_ENDPOINT=http://minio:9000   -e S3_ACCESS_KEY=minio   -e S3_SECRET_KEY=minio123   -e S3_BUCKET=delta   -e DELTA_TABLEURI=s3a://delta/prices   pc-file-ingest:dev
```

### **etl-processor-service**
```bash
docker run --rm --network=price-compare-monorepo_default -p 8082:8082   -e MP_MESSAGING_INCOMING_PRICES_PARSED_HOST=rabbitmq   -e MP_MESSAGING_INCOMING_PRICES_PARSED_PORT=5672   -e MP_MESSAGING_INCOMING_PRICES_PARSED_USERNAME=guest   -e MP_MESSAGING_INCOMING_PRICES_PARSED_PASSWORD=guest   -e QUARKUS_MONGODB_CONNECTION_STRING=mongodb://mongodb:27017   -e QUARKUS_MONGODB_DATABASE=pricesdb   pc-etl:dev
```

### **product-compare-service**
```bash
docker run --rm --network=price-compare-monorepo_default -p 8083:8083   -e QUARKUS_MONGODB_CONNECTION_STRING=mongodb://mongodb:27017   -e QUARKUS_MONGODB_DATABASE=pricesdb   -e QUARKUS_REDIS_HOSTS=redis://redis:6379   pc-compare-prod:dev
```

### **category-compare-service**
```bash
docker run --rm --network=price-compare-monorepo_default -p 8084:8084   -e QUARKUS_MONGODB_CONNECTION_STRING=mongodb://mongodb:27017   -e QUARKUS_MONGODB_DATABASE=pricesdb   -e QUARKUS_REDIS_HOSTS=redis://redis:6379   pc-compare-cat:dev
```

---

## Testando a API

### 1. Enviar TXT para ingestão
```bash
curl -F file=@amostra.txt http://localhost:8081/ingest/upload
```
**Formato do TXT**:
```
EAN;STORE;PRICE;PROMO;TS;CATEGORY
7894900011517;STORE_A;9.90;true;2025-08-27T10:00:00Z;limpeza
7894900011517;STORE_B;10.49;false;2025-08-27T10:05:00Z;limpeza
```

### 2. Comparar preços por produto
```bash
curl http://localhost:8083/compare/product/7894900011517 | jq
```

### 3. Comparar preços por categoria
```bash
curl "http://localhost:8084/compare/category/limpeza?limitPerStore=3" | jq
```

---