package com.bakery.model;

import java.util.List;

public class CustomerOrder extends BakeryRecord {
    private String customerName;
    private String orderedItems;
    private String totalAmount;
    private String orderStatus;
    private String paymentStatus;

    public CustomerOrder() {
    }

    public CustomerOrder(String id, String customerName, String orderedItems, String totalAmount, String orderStatus, String paymentStatus) {
        super(id);
        this.customerName = customerName;
        this.orderedItems = orderedItems;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String getDisplayName() {
        return customerName + " - " + orderStatus;
    }

    @Override
    public List<String> toFileFields() {
        return List.of(customerName, orderedItems, totalAmount, orderStatus, paymentStatus);
    }

    @Override
    public void applyFileFields(List<String> fields) {
        customerName = fields.get(0);
        orderedItems = fields.get(1);
        totalAmount = fields.get(2);
        orderStatus = fields.get(3);
        paymentStatus = fields.get(4);
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getOrderedItems() { return orderedItems; }
    public void setOrderedItems(String orderedItems) { this.orderedItems = orderedItems; }
    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
