package com.bakery.model;

import java.util.List;

public class CustomCakeBooking extends BakeryRecord {
    private String customerName;
    private String cakeSize;
    private String designDetails;
    private String pickupDate;
    private String bookingStatus;

    public CustomCakeBooking() {
}

    public CustomCakeBooking(String id, String customerName, String cakeSize, String designDetails, String pickupDate, String bookingStatus) {
        super(id);
        this.customerName = customerName;
        this.cakeSize = cakeSize;
        this.designDetails = designDetails;
        this.pickupDate = pickupDate;
        this.bookingStatus = bookingStatus;
    }
    @Override
    public String getDisplayName() {
        return customerName + " - " + cakeSize;
    }

    @Override
    public List<String> toFileFields() {
        return List.of(customerName, cakeSize, designDetails, pickupDate, bookingStatus);
    }

    @Override
    public void applyFileFields(List<String> fields) {
        customerName = fields.get(0);
        cakeSize = fields.get(1);
        designDetails = fields.get(2);
        pickupDate = fields.get(3);
        bookingStatus = fields.get(4);
    }
