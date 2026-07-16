package com.example.bank.messaging;

import com.example.bank.config.RabbitMQConfig;
import com.example.bank.dto.TransactionHistoryDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(TransactionHistoryDto event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSACTION_EXCHANGE, "", event);
    }
}
