package com.acme.ingest;

import com.acme.common.dto.PriceRecord;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import io.delta.standalone.DeltaLog;
import io.delta.standalone.OptimisticTransaction;
import io.delta.standalone.Operation;              // pacote correto
import io.delta.standalone.CommitResult;          // << novo import
import io.delta.standalone.actions.AddFile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DeltaWriter {

    private DeltaLog log;
    private Configuration hconf;
    private String tableUri;

    @PostConstruct
    void init() {
        try {
            tableUri = System.getProperty("delta.tableUri", "s3a://delta/prices");

            hconf = new Configuration();
            hconf.set("fs.s3a.endpoint", "http://localhost:9000");
            hconf.set("fs.s3a.path.style.access", "true");
            hconf.set("fs.s3a.access.key", "minio");
            hconf.set("fs.s3a.secret.key", "minio123");
            hconf.set("fs.s3a.connection.ssl.enabled", "false");

            log = DeltaLog.forTable(hconf, new Path(tableUri));
            Log.infof("DeltaWriter inicializado. Tabela: %s", tableUri);
        } catch (Exception e) {
            Log.error("Erro ao inicializar DeltaLog", e);
        }
    }

    public void append(PriceRecord rec) {
        try {
            var tmp = Files.createTempFile("delta-", ".json");
            String json = "{"
                    + "\"ean\":\"" + rec.ean() + "\","
                    + "\"storeId\":\"" + rec.storeId() + "\","
                    + "\"price\":" + rec.price() + ","
                    + "\"promo\":" + rec.promo() + ","
                    + "\"ts\":\"" + rec.ts() + "\","
                    + "\"category\":\"" + (rec.category() == null ? "" : rec.category()) + "\""
                    + "}";
            Files.writeString(tmp, json);

            String relPath = "part-" + System.nanoTime() + ".json";

            AddFile add = new AddFile(
                    relPath,
                    Map.of("dataChange", "true"),
                    Files.size(tmp),
                    System.currentTimeMillis(),
                    true,
                    null,
                    null
            );

            OptimisticTransaction txn = log.startTransaction();

            // útil para idempotência (ver última versão para um appId)
            long lastAppVersion = txn.txnVersion("file-ingest");

            // Informe o tipo de operação deste commit
            Operation op = new Operation(Operation.Name.WRITE);

            // AGORA: commit retorna CommitResult
            CommitResult cr = txn.commit(List.of(add), op, "quarkus-file-ingest/1.0.0");
            long committedVersion = cr.getVersion();

            Log.infof("Delta commit OK. Tabela=%s, versão=%d (última versão app=%d)",
                    tableUri, committedVersion, lastAppVersion);

        } catch (Exception e) {
            Log.error("Falha ao escrever no Delta Lake", e);
        }
    }
}
