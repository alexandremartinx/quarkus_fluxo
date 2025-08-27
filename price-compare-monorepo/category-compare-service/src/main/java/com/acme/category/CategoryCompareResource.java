package com.acme.category;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.*;

@Path("/compare/category")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryCompareResource {

    @ApplicationScoped
    static class PriceRepo implements PanacheMongoRepositoryBase<Object, String> {
        public List<Document> bestPerStoreByCategory(String category, int limitPerStore) {
            return getCollection().aggregate(List.of(
                new Document("$match", new Document("category", category)),
                new Document("$sort", new Document("ts", -1)),
                new Document("$group", new Document("_id", new Document("storeId", "$storeId").append("ean", "$ean"))
                    .append("storeId", new Document("$first", "$storeId"))
                    .append("ean", new Document("$first", "$ean"))
                    .append("price", new Document("$first", "$price"))
                    .append("ts", new Document("$first", "$ts"))
                ),
                new Document("$sort", new Document("price", 1)),
                new Document("$group", new Document("_id", "$storeId")
                    .append("top", new Document("$push", new Document("ean", "$ean").append("price", "$price")))),
                new Document("$project", new Document("storeId", "$_id").append("_id", 0)
                    .append("top", new Document("$slice", Arrays.asList("$top", limitPerStore))))
            )).into(new ArrayList<>());
        }
    }

    @Inject PriceRepo repo;
    @Inject RedisCache cache;

    @GET
    @Path("/{category}")
    public Response compare(
            @PathParam("category") String category,
            @QueryParam("limitPerStore") @DefaultValue("5") int limitPerStore) {
        String key = "cmp:cat:" + category + ":" + limitPerStore;
        var cached = cache.get(key);
        if (cached.isPresent()) return Response.ok(cached.get()).build();

        var rows = repo.bestPerStoreByCategory(category, limitPerStore);
        var out = new Document("category", category).append("stores", rows);
        String json = out.toJson();
        cache.put(key, json, Duration.ofMinutes(5));
        return Response.ok(json).build();
    }
}
