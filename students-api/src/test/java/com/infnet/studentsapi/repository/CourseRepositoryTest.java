package com.infnet.studentsapi.repository;

import com.infnet.studentsapi.config.JpaAuditingConfig;
import com.infnet.studentsapi.model.Course;
import com.infnet.studentsapi.model.DegreeLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CourseRepositoryTest {

    @Autowired
    private CourseRepository repository;

    private Course newCourse(String name, String code, DegreeLevel level, int semesters) {
        var course = new Course();
        course.setName(name);
        course.setCode(code);
        course.setDegreeLevel(level);
        course.setDurationSemesters(semesters);
        course.setDepartment("Tecnologia");
        return course;
    }

    @Test
    void shouldSaveAndFindCourseById() {
        var saved = repository.save(newCourse("Engenharia de Software", "ESW", DegreeLevel.GRADUACAO, 8));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Engenharia de Software");
        assertThat(found.get().getDegreeLevel()).isEqualTo(DegreeLevel.GRADUACAO);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByCode() {
        repository.save(newCourse("Ciencia de Dados", "CDD", DegreeLevel.GRADUACAO, 8));

        assertThat(repository.findByCode("CDD")).isPresent();
        assertThat(repository.findByCode("XXX")).isEmpty();
    }

    @Test
    void shouldFindByDegreeLevel() {
        repository.save(newCourse("Engenharia de Software", "ESW", DegreeLevel.GRADUACAO, 8));
        repository.save(newCourse("Ciencia de Dados", "CDD", DegreeLevel.GRADUACAO, 8));
        repository.save(newCourse("Arquitetura Distribuida", "ASD", DegreeLevel.POS_GRADUACAO, 3));

        assertThat(repository.findByDegreeLevel(DegreeLevel.GRADUACAO)).hasSize(2);
        assertThat(repository.findByDegreeLevel(DegreeLevel.POS_GRADUACAO)).hasSize(1);
        assertThat(repository.findByDegreeLevel(DegreeLevel.MESTRADO)).isEmpty();
    }

    @Test
    void shouldFindAllOrderedByName() {
        repository.save(newCourse("Engenharia de Software", "ESW", DegreeLevel.GRADUACAO, 8));
        repository.save(newCourse("Arquitetura Distribuida", "ASD", DegreeLevel.POS_GRADUACAO, 3));
        repository.save(newCourse("Ciencia de Dados", "CDD", DegreeLevel.GRADUACAO, 8));

        assertThat(repository.findAllByOrderByNameAsc())
                .extracting(Course::getCode)
                .containsExactly("ASD", "CDD", "ESW");
    }

    @Test
    void shouldEnforceUniqueCodeConstraint() {
        repository.saveAndFlush(newCourse("Engenharia de Software", "ESW", DegreeLevel.GRADUACAO, 8));

        assertThatThrownBy(() ->
                repository.saveAndFlush(newCourse("Outro Curso", "ESW", DegreeLevel.TECNICO, 4)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
