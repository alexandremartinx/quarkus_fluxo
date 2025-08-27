package com.acme.product;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class RedisCache {

    @Inject RedisDataSource ds;

    public void put(String key, String json, Duration ttl) {
        ValueCommands<String, String> val = ds.value(String.class);
        val.setex(key, (int) ttl.getSeconds(), json);
    }

    public Optional<String> get(String key) {
        var val = ds.value(String.class).get(key);
        return Optional.ofNullable(val);
    }
}
