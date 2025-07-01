package com.ecommerce.user.dto;

/**
 * DTO for user registration requests.
 * Contains the required fields for creating a new user account.
 */
public class UserRegistrationRequest {
    private String name;
    private String email;
    private String password;
    private String phoneNumber;

    public UserRegistrationRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}