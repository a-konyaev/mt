package ru.mt.controller.dto;

import lombok.Getter;

@Getter
public class ErrorResponse extends MoneyTransferResponse {
    private final String message;

    public ErrorResponse(String message) {
        super(ResponseStatus.ERROR);

        this.message = message;
    }

    @Override
    public String toString() {
        return "ERROR: " + message;
    }
}
