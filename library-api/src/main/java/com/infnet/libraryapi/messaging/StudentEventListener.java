package com.infnet.libraryapi.messaging;

import com.infnet.libraryapi.service.StudentReplicaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StudentEventListener {
    private final StudentReplicaService replicaService;

    public StudentEventListener(StudentReplicaService replicaService) {
        this.replicaService = replicaService;
    }

    @RabbitListener(queues = MessagingConfig.STUDENTS_QUEUE)
    public void onStudentEvent(StudentEvent event) {
        replicaService.apply(event);
    }
}
