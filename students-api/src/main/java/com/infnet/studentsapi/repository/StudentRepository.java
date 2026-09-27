package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.model.Student;
import com.infnet.studentsapi.model.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    List<Student> findByNameContainingIgnoreCase(String name);

    List<Student> findByStatus(StudentStatus status);

    List<Student> findByCourseId(Long courseId);

    boolean existsByCourseId(Long courseId);

    @Query("""
            SELECT s.course.id, COUNT(s)
            FROM Student s
            WHERE s.course IS NOT NULL AND s.status = com.infnet.studentsapi.model.StudentStatus.ATIVO
            GROUP BY s.course.id
            """)
    List<Object[]> countActiveStudentsByCourse();

    @Query("""
            SELECT s FROM Student s
            WHERE s.status = com.infnet.studentsapi.model.StudentStatus.ATIVO
              AND s.currentSemester >= :semester
            """)
    List<Student> findActiveFromSemester(@Param("semester") Integer semester);
}
