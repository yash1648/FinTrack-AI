package com.grim.backend.transaction.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.dto.PaginationDto;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.category.dto.CategoryResponse;
import com.grim.backend.transaction.dto.CreateTransactionRequest;
import com.grim.backend.transaction.dto.TransactionResponse;
import com.grim.backend.transaction.dto.UpdateTransactionRequest;
import com.grim.backend.transaction.entity.Transaction;
import com.grim.backend.transaction.entity.TransactionType;
import com.grim.backend.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtProvider jwtProvider;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateTransactionRequest request) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Transaction transaction = transactionService.createTransaction(
                userId, request.amount(), request.type(), request.categoryId(), request.description(), request.date());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, mapToResponse(transaction)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID category_id,
            @RequestParam(required = false) BigDecimal min_amount,
            @RequestParam(required = false) BigDecimal max_amount,
            @RequestParam(required = false) String search) {
        
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Page<Transaction> transactionPage = transactionService.getTransactions(
                userId, from, to, type, category_id, min_amount, max_amount, search, page, limit);
        
        List<TransactionResponse> data = transactionPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        PaginationDto pagination = new PaginationDto(
                transactionPage.getNumber() + 1,
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(true, data, pagination));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Transaction transaction = transactionService.getTransactionById(userId, id);
        return ResponseEntity.ok(new ApiResponse<>(true, mapToResponse(transaction)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        Transaction transaction = transactionService.updateTransaction(
                userId, id, request.amount(), request.type(), request.categoryId(), request.description(), request.date());
        return ResponseEntity.ok(new ApiResponse<>(true, mapToResponse(transaction)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getUser().getId(),
                t.getAmount(),
                t.getType(),
                new CategoryResponse(t.getCategory().getId(), t.getCategory().getName(), t.getCategory().isDefault()),
                t.getDescription(),
                t.getDate(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
