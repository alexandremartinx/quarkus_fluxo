package com.acme.common.events;

import com.acme.common.dto.PriceRecord;

public record PriceParsedEvent(
    String id,
    PriceRecord payload
) {}
