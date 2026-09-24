package com.amanah.banking.dto.response;

import com.amanah.banking.model.Customer;

public class CustomerResponse {
    private Long id;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String phone;
    private String address;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id = c.getId();
        r.fullName = c.getFullName();
        r.firstName = c.getFirstName();
        r.middleName = c.getMiddleName();
        r.lastName = c.getLastName();
        r.phone = c.getPhone();
        r.address = c.getAddress();
        return r;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
}
