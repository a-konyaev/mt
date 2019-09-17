package ru.mt.controller;

class QueryParamsException extends Exception {
    QueryParamsException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
