package com.github._255_ping.rpg.api.currency;

import java.math.BigDecimal;

public interface Currency {
    String id();
    String displaySingular();
    String displayPlural();
    String prefix();
    String suffix();
    int decimals();
    BigDecimal maxBalance();
    String format(BigDecimal amount);
}
