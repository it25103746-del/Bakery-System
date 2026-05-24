package com.bakery.model;

import java.util.List;

public class CustomerOrder extends BakeryRecord {
    private String customerName;
    private String orderedItems;
    private String totalAmount;
    private String orderStatus;
    private String paymentStatus;

    public CustomerOrder(){

    }
    public CustomerOrder(String id, String customerName, String orderedItems, String totalAmount, String orderStatus, String paymentStatus){
        super(id);
        this.customerName = customerName;
        this.orderedItems = orderedItems;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
    }


}
