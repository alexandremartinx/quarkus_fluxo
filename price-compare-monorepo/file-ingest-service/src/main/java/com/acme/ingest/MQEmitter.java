package com.acme.ingest;

import com.acme.common.events.PriceParsedEvent;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MQEmitter {

    @Channel("prices-parsed")
    MutinyEmitter<PriceParsedEvent> emitter;

    public void emit(PriceParsedEvent evt) {
        emitter.send(evt);
    }
}
