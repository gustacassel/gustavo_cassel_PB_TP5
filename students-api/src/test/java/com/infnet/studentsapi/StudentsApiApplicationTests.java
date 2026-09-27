package com.infnet.studentsapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@ActiveProfiles("test")
class StudentsApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
