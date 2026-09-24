package com.amanah.banking.service;

import com.amanah.banking.dto.request.UpdateCustomerRequest;
import com.amanah.banking.dto.response.CustomerResponse;
import com.amanah.banking.exception.ResourceNotFoundException;
import com.amanah.banking.model.Customer;
import com.amanah.banking.model.User;
import com.amanah.banking.repository.CustomerRepository;
import com.amanah.banking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    public CustomerResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse updateProfile(String username, UpdateCustomerRequest req) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        if (req.getFirstName() != null) customer.setFirstName(req.getFirstName());
        if (req.getMiddleName() != null) customer.setMiddleName(req.getMiddleName());
        if (req.getLastName() != null) customer.setLastName(req.getLastName());
        if (req.getPhone() != null) customer.setPhone(req.getPhone());
        if (req.getAddress() != null) customer.setAddress(req.getAddress());

        customerRepository.update(customer);
        return CustomerResponse.from(customer);
    }
}
