package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.model.StudentLoan;
import com.infnet.studentsapi.model.StudentLoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentLoanRepository extends JpaRepository<StudentLoan, Long> {

    long countByStudentIdAndStatus(Long studentId, StudentLoanStatus status);

    List<StudentLoan> findByStudentIdAndStatus(Long studentId, StudentLoanStatus status);
}
