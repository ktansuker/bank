package com.example.bank.messaging;

import com.example.bank.config.RabbitMQConfig;
import com.example.bank.dto.TransactionHistoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLogListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);

    @RabbitListener(queues = RabbitMQConfig.AUDIT_QUEUE)
    public void onTransactionEvent(TransactionHistoryDto event) {
        log.info("[AUDIT] {} -> {} | {} {} | işlem sonrası bakiye: {} | not: {}",
                event.getUsername(), event.getType(), event.getAmount(), event.getCurrency(),
                event.getBalanceAfter(), event.getNote());
    }
}
