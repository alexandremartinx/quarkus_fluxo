package com.acme.etl;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PriceRepository implements PanacheMongoRepository<PriceDoc> {}
