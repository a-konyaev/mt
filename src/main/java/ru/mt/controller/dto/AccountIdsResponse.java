package ru.mt.controller.dto;

import lombok.Getter;

import java.util.Set;

@Getter
public class AccountIdsResponse extends MoneyTransferResponse {
    private final Set<String> accountIds;

    public AccountIdsResponse(Set<String> accountIds) {
        super(ResponseStatus.OK);

        this.accountIds = accountIds;
    }

    @Override
    public String toString() {
        return "OK: count " + accountIds.size();
    }
}
