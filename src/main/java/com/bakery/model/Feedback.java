package com.bakery.model;

import java.util.List;

public class Feedback extends BakeryRecord {
    private String customerName;
    private String rating;
    private String comment;
    private String moderationStatus;

    public Feedback() {
    }

    public Feedback(String id, String customerName, String rating, String comment, String moderationStatus) {
        super(id);
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.moderationStatus = moderationStatus;
    }

    @Override
    public String getDisplayName() {
        return customerName + " - " + rating + "/5";
    }

    @Override
    public List<String> toFileFields() {
        return List.of(customerName, rating, comment, moderationStatus);
    }

    @Override
    public void applyFileFields(List<String> fields) {
        customerName = fields.get(0);
        rating = fields.get(1);
        comment = fields.get(2);
        moderationStatus = fields.get(3);
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
}
