package ru.mt.domain;

import lombok.Getter;

/**
 * Статус транзакции по переводу денег
 */
public enum TransactionStatus {
    /**
     * новая зарегистрированная транзакция
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
     * (чтобы потом перейти в статус DENIED)
     */
    CANCELLING(false),
    /**
     * Перевод денег выполнен
     */
    DONE(true),
    /**
     * в операции отказано (например, из-за недостаточной суммы денег на счете)
     */
    DENIED(true),
    /**
     * завершено с ошибкой, требуется тех. поддержка
     */
    ERROR(true);

    public static TransactionStatus INITIAL_STATUS = TransactionStatus.CREATED;

    @Getter
    private final boolean isFinal;

    TransactionStatus(boolean isFinal) {
        this.isFinal = isFinal;
    }
}
