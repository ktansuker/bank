package com.example.bank.service;

import com.example.bank.dto.TransactionHistoryDto;
import com.example.bank.dto.UserDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendBalanceUpdate(String username, UserDto userDto) {
        messagingTemplate.convertAndSendToUser(username, "/queue/balance", userDto);
    }

    public void sendAdminTransaction(TransactionHistoryDto dto) {
        messagingTemplate.convertAndSend("/topic/admin/transactions", dto);
    }
}
