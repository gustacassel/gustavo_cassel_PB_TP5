package com.infnet.studentsapi.messaging;

import com.infnet.studentsapi.dto.CourseRequest;
import com.infnet.studentsapi.dto.StudentRequest;
import com.infnet.studentsapi.exception.BusinessException;
import com.infnet.studentsapi.model.DegreeLevel;
import com.infnet.studentsapi.model.StudentStatus;
import com.infnet.studentsapi.service.CourseService;
import com.infnet.studentsapi.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
class StudentEventPublishingTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private Long courseId;

    @BeforeEach
    void setUp() {
        courseId = courseService.create(new CourseRequest(
                "Engenharia de Software", "ESW-EVT", DegreeLevel.GRADUACAO, 8, "Tecnologia")).getId();
    }

    private StudentRequest request(String email, String enrollment, StudentStatus status) {
        return new StudentRequest("Maria Silva", email, enrollment,
                LocalDate.of(2000, 5, 10), LocalDate.of(2024, 2, 1), status, 3, courseId);
    }

    private List<StudentEvent> studentEvents() {
        return applicationEvents.stream(StudentEvent.class).toList();
    }

    @Test
    void shouldRegisterCreatedEventCarryingTheStudentState() {
        var saved = studentService.create(request("evt1@email.com", "EVT-001", StudentStatus.ATIVO));

        assertThat(studentEvents()).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(StudentEventType.CREATED);
            assertThat(event.eventId()).isNotNull();
            assertThat(event.studentId()).isEqualTo(saved.getId());
            assertThat(event.version()).isZero();
            assertThat(event.status()).isEqualTo(StudentStatus.ATIVO);
            assertThat(event.courseName()).isEqualTo("Engenharia de Software");
        });
    }

    @Test
    void shouldIncrementTheVersionOnEachUpdateAndUseTheNextOneOnDelete() {
        var saved = studentService.create(request("evt2@email.com", "EVT-002", StudentStatus.ATIVO));

        studentService.update(saved.getId(), request("evt2@email.com", "EVT-002", StudentStatus.TRANCADO));
        studentService.delete(saved.getId());

        assertThat(studentEvents())
                .extracting(StudentEvent::type, StudentEvent::version)
                .containsExactly(
                        tuple(StudentEventType.CREATED, 0L),
                        tuple(StudentEventType.UPDATED, 1L),
                        tuple(StudentEventType.DELETED, 2L));
        assertThat(studentEvents().get(1).status()).isEqualTo(StudentStatus.TRANCADO);
    }

    @Test
    void shouldRepublishStudentsWhenTheirCourseIsRenamed() {
        studentService.create(request("evt3@email.com", "EVT-003", StudentStatus.ATIVO));
        studentService.create(request("evt4@email.com", "EVT-004", StudentStatus.ATIVO));
        applicationEvents.clear();

        courseService.update(courseId, new CourseRequest(
                "Engenharia de Software Moderna", "ESW-EVT", DegreeLevel.GRADUACAO, 8, "Tecnologia"));

        assertThat(studentEvents())
                .hasSize(2)
                .allMatch(event -> event.type() == StudentEventType.UPDATED)
                .allMatch(event -> event.courseName().equals("Engenharia de Software Moderna"));
    }

    @Test
    void shouldNotRegisterAnyEventWhenTheOperationIsRejected() {
        studentService.create(request("evt5@email.com", "EVT-005", StudentStatus.ATIVO));
        applicationEvents.clear();

        assertThatThrownBy(() -> studentService.create(request("evt5@email.com", "EVT-006", StudentStatus.ATIVO)))
                .isInstanceOf(BusinessException.class);

        assertThat(studentEvents()).isEmpty();
    }

    @Test
    void shouldNotSendToTheBrokerBeforeTheTransactionCommits() {
        // a transacao do teste sofre rollback, entao o AFTER_COMMIT nao dispara
        studentService.create(request("evt6@email.com", "EVT-007", StudentStatus.ATIVO));

        assertThat(studentEvents()).hasSize(1);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSendToTheBrokerOnceTheTransactionCommits() {
        var saved = studentService.create(request("evt7@email.com", "EVT-008", StudentStatus.ATIVO));

        try {
            verify(rabbitTemplate).convertAndSend(eq("students.events"), eq("student.created"),
                    argThat((Object payload) -> payload instanceof StudentEvent event
                            && event.studentId().equals(saved.getId())),
                    any(MessagePostProcessor.class));
        } finally {
            studentService.delete(saved.getId());
            courseService.delete(courseId);
        }
    }
}
