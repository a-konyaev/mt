package ru.mt.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class Account {
    private final String id;
    @Setter
    private BigDecimal balance = BigDecimal.ZERO;
}
