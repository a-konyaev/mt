package ru.mt.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.mt.utils.TimeUtils;

/**
 * Запись о резервировании суммы денег на счете
 */
@Getter
@RequiredArgsConstructor
public class Reservation {
    private final long ts = TimeUtils.getTimestamp();
    private final String accountId;
    private final String transactionId;
    private final double amount;
    @Setter
    private ReservationStatus status;
}
