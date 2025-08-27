package com.acme.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceRecord(
    String ean,
    String storeId,
    BigDecimal price,
    boolean promo,
    Instant ts,
    String category
) {}
