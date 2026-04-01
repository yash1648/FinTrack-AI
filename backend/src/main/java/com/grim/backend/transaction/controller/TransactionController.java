package com.grim.backend.transaction.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.dto.PaginationDto;
import com.grim.backend.auth.security.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(
                userDetails.getId(), request.amount(), request.type(), request.categoryId(), request.description(), request.date());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, mapToResponse(transaction)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID category_id,
            @RequestParam(required = false) BigDecimal min_amount,
            @RequestParam(required = false) BigDecimal max_amount,
            @RequestParam(required = false) String search) {
        
        Page<Transaction> transactionPage = transactionService.getTransactions(
                userDetails.getId(), from, to, type, category_id, min_amount, max_amount, search, page, limit);
        
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        Transaction transaction = transactionService.getTransactionById(userDetails.getId(), id);
        return ResponseEntity.ok(new ApiResponse<>(true, mapToResponse(transaction)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        Transaction transaction = transactionService.updateTransaction(
                userDetails.getId(), id, request.amount(), request.type(), request.categoryId(), request.description(), request.date());
        return ResponseEntity.ok(new ApiResponse<>(true, mapToResponse(transaction)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        transactionService.deleteTransaction(userDetails.getId(), id);
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
