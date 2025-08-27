package com.acme.product;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;

import java.time.Duration;
import java.util.*;

@Path("/compare/product")
@Produces(MediaType.APPLICATION_JSON)
public class CompareResource {

    @ApplicationScoped
    static class PriceRepo implements PanacheMongoRepositoryBase<PanacheMongoEntityBase, String> {
        public List<Document> findLatestByEAN(String ean) {
            return getCollection().aggregate(List.of(
                new Document("$match", new Document("ean", ean)),
                new Document("$sort", new Document("ts", -1)),
                new Document("$group", new Document("_id", "$storeId")
                    .append("ean", new Document("$first", "$ean"))
                    .append("storeId", new Document("$first", "$storeId"))
                    .append("price", new Document("$first", "$price"))
                    .append("promo", new Document("$first", "$promo"))
                    .append("ts", new Document("$first", "$ts"))
                )
            )).into(new ArrayList<>());
        }
    }

    @Inject PriceRepo repo;
    @Inject RedisCache cache;

    @GET
    @Path("/{ean}")
    public Response compare(@PathParam("ean") String ean) {
        String key = "cmp:prod:" + ean;
        var cached = cache.get(key);
        if (cached.isPresent()) return Response.ok(cached.get()).build();

        List<Document> docs = repo.findLatestByEAN(ean);
        if (docs.isEmpty()) return Response.status(404).entity("{}").build();

        docs.sort(Comparator.comparing(d -> d.get("price", Number.class).doubleValue()));
        var best = docs.get(0);

        var out = new Document("ean", ean)
            .append("best", best)
            .append("offers", docs)
            .append("stores", docs.size());

        String json = out.toJson();
        cache.put(key, json, Duration.ofMinutes(5));
        return Response.ok(json).build();
    }
}
