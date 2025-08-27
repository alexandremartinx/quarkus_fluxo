package com.acme.etl;

import com.acme.common.events.PriceParsedEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class PriceConsumer {

    @Inject PriceRepository repo;

    @Incoming("prices-parsed")
    @Blocking
    public void consume(PriceParsedEvent evt) {
        long already = repo.count("sourceEventId", evt.id());
        if (already > 0) return;

        var rec = evt.payload();
        var doc = new PriceDoc();
        doc.ean = rec.ean();
        doc.storeId = rec.storeId();
        doc.price = rec.price();
        doc.promo = rec.promo();
        doc.ts = rec.ts();
        doc.category = rec.category();
        doc.sourceEventId = evt.id();
        repo.persist(doc);
    }
}
