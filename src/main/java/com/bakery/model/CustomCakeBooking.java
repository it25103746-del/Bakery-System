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