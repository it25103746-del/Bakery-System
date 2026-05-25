package com.bakery.model;

import java.util.List;

public class Product extends BakeryRecord {
    private String productName;
    private String category;
    private String price;
    private String details;
    private  String availability;
    private String stockAmount;

    public Product(){

    }

    public Product(String id, String productName,String category, String price,String details,String availability){
        this (id, productName,category,price,details,availability"0");
    }
}
