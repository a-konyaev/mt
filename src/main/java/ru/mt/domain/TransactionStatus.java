package ru.mt.domain;

import lombok.Getter;

/**
 * Статус транзакции по переводу денег
 */
public enum TransactionStatus {
    /**
     * Новая зарегистрированная транзакция
     */
    CREATED(false),
    /**
     * Деньги зарезервированы на счете отправителя
     */
    RESERVED(false),
    /**
     * Деньги добавлены на счет получателя
     */
    ADDED(false),
    /**
     * Транзация отменяется из-за невозможности ее выполнить
     */
    CANCELLING(false),
    /**
     * Перевод денег выполнен
     */
    DONE(true),
    /**
     * В операции отказано (например, из-за недостаточной суммы денег на счете)
     */
    DENIED(true),
    /**
     * Завершено с ошибкой, требуется тех. поддержка
     */
    ERROR(true);

    @Getter
    private final boolean isFinal;

    @Getter
    private String reason;

    public TransactionStatus setReason(String reason) {
        this.reason = reason;
        return this;
    }

    TransactionStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }
}
