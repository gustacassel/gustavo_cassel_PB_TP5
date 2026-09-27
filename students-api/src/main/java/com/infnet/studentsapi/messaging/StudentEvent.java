package com.infnet.studentsapi.messaging;

import com.infnet.studentsapi.model.Student;
import com.infnet.studentsapi.model.StudentStatus;

import java.time.Instant;
import java.util.UUID;

public record StudentEvent(
        UUID eventId,
        StudentEventType type,
        Instant occurredAt,
        Long studentId,
        Long version,
        String name,
        String email,
        String enrollmentNumber,
        StudentStatus status,
        Long courseId,
        String courseName
) {

    public static StudentEvent created(Student student) {
        return of(StudentEventType.CREATED, student, student.getVersion());
    }

    public static StudentEvent updated(Student student) {
        return of(StudentEventType.UPDATED, student, student.getVersion());
    }

    // o delete nao incrementa o @Version, entao o evento usa a proxima versao
    public static StudentEvent deleted(Student student) {
        return of(StudentEventType.DELETED, student, student.getVersion() + 1);
    }

    private static StudentEvent of(StudentEventType type, Student student, Long version) {
        var course = student.getCourse();
        return new StudentEvent(
                UUID.randomUUID(),
                type,
                Instant.now(),
                student.getId(),
                version,
                student.getName(),
                student.getEmail(),
                student.getEnrollmentNumber(),
                student.getStatus(),
                course != null ? course.getId() : null,
                course != null ? course.getName() : null);
    }
}
