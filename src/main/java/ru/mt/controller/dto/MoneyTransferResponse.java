package ru.mt.controller.dto;

import lombok.Getter;

@Getter
public abstract class MoneyTransferResponse {
    private final ResponseStatus status;

    MoneyTransferResponse(ResponseStatus status) {
        this.status = status;
    }
}
