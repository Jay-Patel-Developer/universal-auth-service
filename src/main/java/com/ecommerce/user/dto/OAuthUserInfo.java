package com.ecommerce.user.dto;

/**
 * DTO for user info returned by OAuth providers.
 * Contains basic profile fields such as name, email, and picture URL.
 */
public class OAuthUserInfo {
    /** Full name of the user. */
    private String name;
    /** Email address of the user. */
    private String email;
    /** Profile picture URL. */
    private String picture;
    
    /** @return Full name of the user. */
    public String getName() {
        return name;
    }
    
    /** @param name Full name of the user. */
    public void setName(String name) {
        this.name = name;
    }
    
    /** @return Email address of the user. */
    public String getEmail() {
        return email;
    }
    
    /** @param email Email address of the user. */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /** @return Profile picture URL. */
    public String getPicture() {
        return picture;
    }
    
    /** @param picture Profile picture URL. */
    public void setPicture(String picture) {
        this.picture = picture;
    }
}
