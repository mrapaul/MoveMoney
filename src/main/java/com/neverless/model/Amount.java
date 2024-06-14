package com.neverless.model;

import java.math.BigDecimal;

public class Amount {
    private final BigDecimal value;

    public Amount(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }
}
