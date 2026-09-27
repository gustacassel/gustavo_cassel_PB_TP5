package com.infnet.libraryapi.messaging;

public enum LoanEventType {
    CREATED("loan.created"),
    RETURNED("loan.returned"),
    DELETED("loan.deleted");

    private final String routingKey;

    LoanEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
