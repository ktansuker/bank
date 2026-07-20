package com.example.bank.config;

import com.example.bank.entity.Currency;
import com.example.bank.entity.User;
import com.example.bank.entity.Wallet;
import com.example.bank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@ktbank.com", passwordEncoder.encode("admin123"), "ROLE_ADMIN");
            for (Currency currency : Currency.values()) {
                admin.addWallet(new Wallet(admin, currency));
            }
            userRepository.save(admin);
        }
    }
}
