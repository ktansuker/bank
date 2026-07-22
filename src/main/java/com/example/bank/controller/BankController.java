package com.example.bank.controller;

import com.example.bank.dto.*;
import com.example.bank.service.BankService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BankController {

    // Bir istemci yanlışlıkla ya da kötü niyetle size=100000 gibi bir değer
    // gönderip
    // tüm veritabanını tek seferde çekmeye çalışamasın diye üst sınır koyuyoruz.
    private static final int MAX_PAGE_SIZE = 100;

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
        return ResponseEntity
                .ok(bankService.createUser(request.getUsername(), request.getEmail(), request.getPassword()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<PageResponse<UserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("id").ascending());
        return ResponseEntity.ok(PageResponse.from(bankService.listUsers(pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/transactions")
    public ResponseEntity<PageResponse<TransactionHistoryDto>> allTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("timestamp").descending());
        return ResponseEntity.ok(PageResponse.from(bankService.getAllHistory(pageable)));
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
        return ResponseEntity.ok(bankService.transfer(principal.getName(), request.getTargetIdentifier(),
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
    public ResponseEntity<PageResponse<TransactionHistoryDto>> myTransactions(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("timestamp").descending());
        return ResponseEntity.ok(PageResponse.from(bankService.getHistory(principal.getName(), pageable)));
    }

    private int clampSize(int size) {
        if (size < 1)
            return 1;
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
