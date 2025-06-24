package com.ecommerce.user.dto;

/**
 * DTO for user registration requests.
 * Contains the required fields for creating a new user account.
 */
public class UserRegistrationRequest {
    /** Full name of the user. */
    private String name;
    /** Email address of the user. */
    private String email;
    /** Password for the new user account. */
    private String password;
    /** Optional phone number for the user. */
    private String phoneNumber;

    /**
     * Default constructor.
     */
    public UserRegistrationRequest() {}

    /**
     * Constructs a UserRegistrationRequest with all fields.
     */
    public UserRegistrationRequest(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    /** @return Full name of the user. */
    public String getName() { return name; }
    /** @param name Full name of the user. */
    public void setName(String name) { this.name = name; }
    /** @return Email address of the user. */
    public String getEmail() { return email; }
    /** @param email Email address of the user. */
    public void setEmail(String email) { this.email = email; }
    /** @return Password for the new user account. */
    public String getPassword() { return password; }
    /** @param password Password for the new user account. */
    public void setPassword(String password) { this.password = password; }
    /** @return Phone number of the user. */
    public String getPhoneNumber() { return phoneNumber; }
    /** @param phoneNumber Phone number of the user. */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}