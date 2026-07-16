package com.example.bank.controller;

import com.example.bank.dto.*;
import com.example.bank.service.BankService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Principal principal) {
        UserDto user = bankService.getUserInfo(principal.getName());
        Map<String, String> body = new HashMap<>();
        body.put("username", user.getUsername());
        body.put("role", user.getRole());
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create-user")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(bankService.createUser(request.getUsername(), request.getPassword()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(bankService.listUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/transactions")
    public ResponseEntity<List<TransactionHistoryDto>> allTransactions() {
        return ResponseEntity.ok(bankService.getAllHistory());
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/me")
    public ResponseEntity<UserDto> myProfile(Principal principal) {
        return ResponseEntity.ok(bankService.getUserInfo(principal.getName()));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/deposit")
    public ResponseEntity<UserDto> deposit(Principal principal, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(bankService.deposit(principal.getName(), request.getAmount(), request.getCurrency()));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/withdraw")
    public ResponseEntity<UserDto> withdraw(Principal principal, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(bankService.withdraw(principal.getName(), request.getAmount(), request.getCurrency()));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/transfer")
    public ResponseEntity<UserDto> transfer(Principal principal, @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(bankService.transfer(principal.getName(), request.getTargetUsername(),
                request.getAmount(), request.getCurrency()));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/exchange")
    public ResponseEntity<UserDto> exchange(Principal principal, @Valid @RequestBody ExchangeRequest request) {
        return ResponseEntity.ok(bankService.exchange(principal.getName(), request.getFromCurrency(),
                request.getToCurrency(), request.getAmount()));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/transactions")
    public ResponseEntity<List<TransactionHistoryDto>> myTransactions(Principal principal) {
        return ResponseEntity.ok(bankService.getHistory(principal.getName()));
    }
}
