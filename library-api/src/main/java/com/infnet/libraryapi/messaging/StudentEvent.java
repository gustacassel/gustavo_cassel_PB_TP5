package com.infnet.libraryapi.messaging;

import java.time.Instant;
import java.util.UUID;

public record StudentEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        Long studentId,
        Long version,
        String name,
        String email,
        String enrollmentNumber,
        String status,
        Long courseId,
        String courseName
) {
    public boolean isDeletion() {
        return "DELETED".equals(type);
    }
}
