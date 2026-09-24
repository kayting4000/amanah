package com.amanah.banking.controller;

import com.amanah.banking.dto.request.TransferRequest;
import com.amanah.banking.dto.response.ApiResponse;
import com.amanah.banking.dto.response.TransactionResponse;
import com.amanah.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransactionService transactionService;

    public TransferController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> transfer(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody TransferRequest req) {
        List<TransactionResponse> result = transactionService.transfer(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.ok("Transfer completed successfully", result));
    }
}
