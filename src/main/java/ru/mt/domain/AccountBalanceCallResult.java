package ru.mt.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class AccountBalanceCallResult {
    private final LocalDateTime time = LocalDateTime.now();
    private final String callId;
    private final Double amount;
    private final ReservationStatus reservationStatus;
    private final String errorMessage;

    public boolean hasError() {
        return errorMessage != null;
    }
}
