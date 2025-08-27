package com.acme.product;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

@Path("/product")
@Produces(MediaType.APPLICATION_JSON)
public class CompareResource {

    @Inject
    PriceRepo repo;

    // ---------- Melhor preço (menor) para um EAN dentre todas as lojas ----------
    @GET
    @Path("/{ean}/best")
    public Response bestForEan(@PathParam("ean") String ean) {
        List<PriceEntry> rows = repo.findByEanOrdered(ean);
        if (rows.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiMessage("Sem preços para EAN " + ean))
                    .build();
        }
        PriceEntry best = rows.stream()
                .filter(r -> r.price != null)
                .min(Comparator.comparingDouble(r -> r.price))
                .orElse(rows.get(0));
        return Response.ok(best).build();
    }

    // ---------- Histórico de preços (ordenado por ts asc) ----------
    @GET
    @Path("/{ean}/history")
    public List<PriceEntry> history(@PathParam("ean") String ean) {
        return repo.findByEanOrderedByTs(ean);
    }

    // ---------- Estatísticas simples (min/avg/max) por EAN ----------
    @GET
    @Path("/{ean}/stats")
    public Response stats(@PathParam("ean") String ean) {
        List<PriceEntry> rows = repo.findByEanOrdered(ean);
        if (rows.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiMessage("Sem preços para EAN " + ean))
                    .build();
        }
        DoubleSummaryStatistics s = rows.stream()
                .filter(r -> r.price != null)
                .collect(Collectors.summarizingDouble(r -> r.price));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ean", ean);
        out.put("count", s.getCount());
        out.put("min", s.getMin());
        out.put("avg", s.getAverage());
        out.put("max", s.getMax());
        return Response.ok(out).build();
    }

    // ------------- DTO de mensagem simples -------------
    public record ApiMessage(String message) {}

    // ------------- Entidade Mongo ----------------------
    @MongoEntity(collection = "prices")
    public static class PriceEntry {
        public String id;        // _id (string para simplificar)
        public String ean;
        public String storeId;
        public Double price;
        public Boolean promo;
        public String ts;        // timestamp ISO-8601 string
        public String category;
    }

    // ------------- Repositório Panache -----------------
    @ApplicationScoped
    public static class PriceRepo implements PanacheMongoRepository<PriceEntry> {

        public List<PriceEntry> findByEanOrdered(String ean) {
            // Ordena por preço crescente
            return find("ean", Sort.by("price"), ean).list();
        }

        public List<PriceEntry> findByEanOrderedByTs(String ean) {
            // Ordena por timestamp ascendente
            return find("ean", Sort.ascending("ts"), ean).list();
        }
    }
}
