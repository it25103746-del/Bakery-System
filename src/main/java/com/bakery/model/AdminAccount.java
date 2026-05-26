package com.bakery.model;

import java.util.List;

public class AdminAccount extends BakeryRecord {
    private String adminName;
    private String email;
    private String role;
    private String stockResponsibility;
    private String expiryStatus;

    public AdminAccount() {
    }

    public AdminAccount(String id, String adminName, String email, String role, String stockResponsibility, String expiryStatus) {
        super(id);
        this.adminName = adminName;
        this.email = email;
        this.role = role;
        this.stockResponsibility = stockResponsibility;
        this.expiryStatus = expiryStatus;
    }

    @Override
    public String getDisplayName() {
        return adminName + " (" + role + ")";
    }

    @Override
    public List<String> toFileFields() {
        return List.of(adminName, email, role, stockResponsibility, expiryStatus);
    }

    @Override
    public void applyFileFields(List<String> fields) {
        adminName = fields.get(0);
        email = fields.get(1);
        role = fields.get(2);
        stockResponsibility = fields.get(3);
        expiryStatus = fields.get(4);
    }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStockResponsibility() { return stockResponsibility; }
    public void setStockResponsibility(String stockResponsibility) { this.stockResponsibility = stockResponsibility; }
    public String getExpiryStatus() { return expiryStatus; }
    public void setExpiryStatus(String expiryStatus) { this.expiryStatus = expiryStatus; }
}
