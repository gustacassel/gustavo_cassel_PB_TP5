package com.infnet.studentsapi.messaging;

import com.infnet.studentsapi.service.StudentLoanService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LoanEventListener {
    private final StudentLoanService studentLoanService;

    public LoanEventListener(StudentLoanService studentLoanService) {
        this.studentLoanService = studentLoanService;
    }

    @RabbitListener(queues = MessagingConfig.LOANS_QUEUE)
    public void onLoanEvent(LoanEvent event) {
        studentLoanService.apply(event);
    }
}
