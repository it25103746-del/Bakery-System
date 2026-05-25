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
