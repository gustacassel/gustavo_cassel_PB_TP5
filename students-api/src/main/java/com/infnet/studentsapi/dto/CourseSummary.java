package com.infnet.studentsapi.dto;

import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.DegreeLevel;

public record CourseSummary(
        Long id,
        String name,
        String code,
        DegreeLevel degreeLevel,
        Integer durationSemesters,
        String department,
        long activeStudents
) {
    public static CourseSummary of(Course course, long activeStudents) {
        return new CourseSummary(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDegreeLevel(),
                course.getDurationSemesters(),
                course.getDepartment(),
                activeStudents);
    }
}
