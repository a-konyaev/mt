package ru.mt.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import ru.mt.utils.TimeUtils;

import java.math.BigDecimal;

@Builder
@Getter
@ToString
public class AccountBalanceCallResult {
    private final long ts = TimeUtils.getTimestamp();

    private final String callId;
    private final BigDecimal amount;
    private final ReservationStatus reservationStatus;
    private final String errorMessage;

    public boolean hasError() {
        return errorMessage != null;
    }
}
