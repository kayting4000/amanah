package com.amanah.banking.controller;

import com.amanah.banking.dto.request.UpdateCustomerRequest;
import com.amanah.banking.dto.response.ApiResponse;
import com.amanah.banking.dto.response.CustomerResponse;
import com.amanah.banking.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> getProfile(
        @AuthenticationPrincipal UserDetails userDetails) {
        CustomerResponse response = customerService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateProfile(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody UpdateCustomerRequest req) {
        CustomerResponse response = customerService.updateProfile(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", response));
    }
}
