package ru.mt.domain;

import lombok.Getter;

/**
 * Статус резервирования денег на счете
 */
public enum ReservationStatus {
    /**
     * Резервирование создано
     */
    OK,
    /**
     * Зарезервированная сумма списаны с баланса счета
     */
    DEBITED,
    /**
     * Резервирование отменено
     */
    CANCELED,
    /**
     * В резервировании отказано
     */
    DENIED;

    @Getter
    private String reason;

    public ReservationStatus setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
