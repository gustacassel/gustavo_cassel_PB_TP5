package com.infnet.libraryapi.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    public static final String STUDENTS_EXCHANGE = "students.events";
    public static final String STUDENTS_QUEUE = "library.students";
    public static final String LIBRARY_EXCHANGE = "library.events";

    @Bean
    public TopicExchange studentsExchange() {
        return ExchangeBuilder.topicExchange(STUDENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange libraryExchange() {
        return ExchangeBuilder.topicExchange(LIBRARY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue studentsQueue() {
        return QueueBuilder.durable(STUDENTS_QUEUE).build();
    }

    @Bean
    public Binding studentsBinding(Queue studentsQueue, TopicExchange studentsExchange) {
        return BindingBuilder.bind(studentsQueue).to(studentsExchange).with("student.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
