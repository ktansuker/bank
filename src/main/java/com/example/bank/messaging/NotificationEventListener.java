package com.example.bank.messaging;

import com.example.bank.config.RabbitMQConfig;
import com.example.bank.dto.TransactionHistoryDto;
import com.example.bank.dto.UserDto;
import com.example.bank.service.BankService;
import com.example.bank.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final BankService bankService;

    public NotificationEventListener(NotificationService notificationService, BankService bankService) {
        this.notificationService = notificationService;
        this.bankService = bankService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onTransactionEvent(TransactionHistoryDto event) {
        UserDto userDto = bankService.getUserInfo(event.getUsername());
        notificationService.sendBalanceUpdate(event.getUsername(), userDto);
        notificationService.sendAdminTransaction(event);
    }
}
