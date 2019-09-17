package ru.mt.controller.dto;

public class OKResponse extends MoneyTransferResponse {
    public OKResponse() {
        super(ResponseStatus.OK);
    }

    @Override
    public String toString() {
        return "OK";
    }
}
