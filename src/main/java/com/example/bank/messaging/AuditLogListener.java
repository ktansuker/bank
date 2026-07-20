package com.example.bank.messaging;

import com.example.bank.config.RabbitMQConfig;
import com.example.bank.dto.TransactionHistoryDto;
import com.example.bank.entity.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLogListener {

    private static final Logger transactionLog = LoggerFactory.getLogger("com.example.bank.TRANSACTION");

    @RabbitListener(queues = RabbitMQConfig.AUDIT_QUEUE)
    public void onTransactionEvent(TransactionHistoryDto event) {
        transactionLog.info(formatMessage(event));
    }

    private String formatMessage(TransactionHistoryDto event) {
        String actionText = switch (event.getType()) {
            case DEPOSIT -> "PARA YATIRILDI";
            case WITHDRAW -> "PARA ÇEKİLDİ";
            case TRANSFER_OUT -> "PARA GÖNDERİLDİ";
            case TRANSFER_IN -> "PARA ALINDI";
            case EXCHANGE_OUT -> "DÖVİZ BOZDURULDU (SATIŞ)";
            case EXCHANGE_IN -> "DÖVİZ BOZDURULDU (ALIŞ)";
        };

        StringBuilder sb = new StringBuilder();
        sb.append(actionText)
                .append(" | kullanıcı: ").append(event.getUsername())
                .append(" | miktar: ").append(event.getAmount()).append(" ").append(event.getCurrency())
                .append(" | işlem sonrası bakiye: ").append(event.getBalanceAfter()).append(" ")
                .append(event.getCurrency());

        if (event.getCounterparty() != null) {
            String label = event.getType() == TransactionType.TRANSFER_OUT ? "alıcı" : "gönderen";
            sb.append(" | ").append(label).append(": ").append(event.getCounterparty());
        }
        if (event.getNote() != null) {
            sb.append(" | not: ").append(event.getNote());
        }

        return sb.toString();
    }
}
