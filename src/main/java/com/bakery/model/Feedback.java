package com.bakery.model;

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


}
