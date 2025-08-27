package com.acme.ingest;

import com.acme.common.dto.PriceRecord;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;

@ApplicationScoped
public class ParserService {

    public PriceRecord parseLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("[;,]");
        if (p.length < 5) return null;
        try {
            String ean = p[0].trim();
            String store = p[1].trim();
            var price = new BigDecimal(p[2].trim());
            boolean promo = Boolean.parseBoolean(p[3].trim());
            var ts = Instant.parse(p[4].trim());
            String category = p.length > 5 ? p[5].trim() : null;
            return new PriceRecord(ean, store, price, promo, ts, category);
        } catch (Exception e) {
            return null;
        }
    }
}
