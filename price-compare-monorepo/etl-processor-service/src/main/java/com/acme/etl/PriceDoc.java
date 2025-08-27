package com.acme.etl;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;

@MongoEntity(collection = "prices")
public class PriceDoc {
    public ObjectId id;
    public String ean;
    public String storeId;
    public BigDecimal price;
    public boolean promo;
    public Instant ts;
    public String category;
    public String sourceEventId;
}
