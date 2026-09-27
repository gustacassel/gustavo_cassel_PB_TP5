package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.DegreeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    List<Course> findByDegreeLevel(DegreeLevel degreeLevel);

    List<Course> findByNameContainingIgnoreCase(String name);

    List<Course> findAllByOrderByNameAsc();
}
