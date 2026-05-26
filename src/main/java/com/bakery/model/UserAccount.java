package com.bakery.model;

import java.util.List;

public class UserAccount extends BakeryRecord {
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String accountStatus;
    private String temporaryPassword;

    public UserAccount() {
    }
    public UserAccount(String id, String fullName, String email, String phone, String address, String accountStatus) {
        this(id, fullName, email, phone, address, accountStatus, "");
    }

    public UserAccount(String id, String fullName, String email, String phone, String address, String accountStatus, String temporaryPassword) {
        super(id);
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.accountStatus = accountStatus;
        this.temporaryPassword = temporaryPassword;
    }
    @Override
    public String getDisplayName() {
        return fullName;
    }

    @Override
    public List<String> toFileFields() {
        return List.of(fullName, email, phone, address, accountStatus, temporaryPassword);
    }

    @Override
    public void applyFileFields(List<String> fields) {
        fullName = fields.get(0);
        email = fields.get(1);
        phone = fields.get(2);
        address = fields.get(3);
        accountStatus = fields.get(4);
        temporaryPassword = fields.size() > 5 ? fields.get(5) : "";
    }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public String getTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }
}
