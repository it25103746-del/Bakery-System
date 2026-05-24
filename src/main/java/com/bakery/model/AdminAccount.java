package com.bakery.model;

public class AdminAccount extends BakeryRecord {
    private String adminName;
    private String email;
    private String role;
    private String stockResponsibility;
    private String expiryStatus;

    public AdminAccount(){
    }

    public AdminAccount(String id, String adminName, String email, String role, String stockResponsibility, String expiryStatus) {
        super(id);
        this.adminName = adminName;
        this.email = email;
        this.role = role;
        this.stockResponsibility = stockResponsibility;
        this.expiryStatus = expiryStatus;
    }
}
