package com.amanah.banking.controller;

import com.amanah.banking.dto.response.AccountResponse;
import com.amanah.banking.dto.response.ApiResponse;
import com.amanah.banking.dto.response.CustomerResponse;
import com.amanah.banking.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable Long userId) {
        adminService.suspendUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User suspended"));
    }

    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User activated"));
    }

    @PutMapping("/accounts/{accountId}/freeze")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(@PathVariable Long accountId) {
        AccountResponse response = adminService.freezeAccount(accountId);
        return ResponseEntity.ok(ApiResponse.ok("Account frozen", response));
    }

    @PutMapping("/accounts/{accountId}/unfreeze")
    public ResponseEntity<ApiResponse<AccountResponse>> unfreezeAccount(@PathVariable Long accountId) {
        AccountResponse response = adminService.unfreezeAccount(accountId);
        return ResponseEntity.ok(ApiResponse.ok("Account unfrozen", response));
    }

    @PutMapping("/accounts/{accountId}/close")
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(@PathVariable Long accountId) {
        AccountResponse response = adminService.closeAccount(accountId);
        return ResponseEntity.ok(ApiResponse.ok("Account closed", response));
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable Long customerId) {
        CustomerResponse response = adminService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Customer retrieved", response));
    }
}