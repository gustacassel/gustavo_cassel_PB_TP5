package com.infnet.libraryapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class LibraryapiApplicationTests {

    @Test
    void contextLoads() {
    }

}
