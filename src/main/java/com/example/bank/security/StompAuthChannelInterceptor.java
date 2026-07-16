package com.example.bank.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final AuthenticationManager authenticationManager;

    public StompAuthChannelInterceptor(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String decoded = new String(Base64.getDecoder().decode(authHeader.substring("Basic ".length())));
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) {
                    Authentication authResult = authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(parts[0], parts[1]));
                    accessor.setUser(authResult);
                }
            }
        }
        return message;
    }
}
