package com.infnet.studentsapi.messaging;

public enum StudentEventType {
    CREATED("student.created"),
    UPDATED("student.updated"),
    DELETED("student.deleted");

    private final String routingKey;

    StudentEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
