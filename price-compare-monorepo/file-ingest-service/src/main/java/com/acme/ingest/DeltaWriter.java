package com.acme.ingest;

import com.acme.common.dto.PriceRecord;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import io.delta.standalone.DeltaLog;
import io.delta.standalone.actions.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DeltaWriter {

    DeltaLog log;
    Configuration hconf;
    String tableUri;

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
                    + "\"category\":\"" + (rec.category() == null ? "" : rec.category()) + "\"}";
            Files.writeString(tmp, json);

            AddFile add = new AddFile(
                "part-" + System.nanoTime() + ".json",
                Map.of("dataChange", "true"),
                Files.size(tmp),
                System.currentTimeMillis(),
                true,
                null,
                null
            );

            var txn = log.startTransaction();
            txn.commit(List.of(add), txn.txnVersion("app", 0), "app");
        } catch (Exception e) {
            Log.error("Falha ao escrever no Delta Lake", e);
        }
    }
}
