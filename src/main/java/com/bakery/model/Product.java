package com.bakery.model;

import java.util.List;

public class Product extends BakeryRecord {

    private String productName;
    private String category;
    private String price;
    private String details;
    private String availability;
    private String stockAmount;


    public Product() {

    }


    public Product(String id, String productName, String category, String price, String details, String availability) {

        this(id, productName, category, price, details, availability, "0");
    }


    public Product(String id, String productName, String category, String price, String details, String availability, String stockAmount) {

        super(id);

        this.productName = productName;
        this.category = category;
        this.price = price;
        this.details = details;
        this.availability = availability;
        this.stockAmount = stockAmount;
    }

    @Override
    public String getDisplayName() {
        return productName + " - Rs." + price;
    }

    @Override
    public List<String> toFileFields() {
        return List.of(productName, category, price, details, availability, stockAmount);
    }

    @Override
    public void applyFileFields(List<String> fields) {

        productName = fields.get(0);
        category = fields.get(1);
        price = fields.get(2);
        details = fields.get(3);
        availability = fields.get(4);

        stockAmount = fields.size() > 5 ? fields.get(5) : "10";
    }



    public String getProductName() {return productName;}

    public void setProductName(String productName) {this.productName = productName;}

    public String getCategory() {return category;}

    public void setCategory(String category) {this.category = category;}

    public String getPrice() {return price;}

    public void setPrice(String price) {this.price = price;}

    public String getDetails() {return details;}

    public void setDetails(String details) {this.details = details;}

    public String getAvailability() {return availability;}

    public void setAvailability(String availability) {this.availability = availability;}

    public String getStockAmount() {return stockAmount;}

    public void setStockAmount(String stockAmount) {this.stockAmount = stockAmount;}
}

