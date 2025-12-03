package com.example.pos.model;

public class MenuItem {

    private String name;
    private double price;
    private String category;
    private String imageUrl;
    private Integer availableQuantity;

    public MenuItem(String name, double price, String category, String imageUrl) {
        this(name, price, category, imageUrl, null);
    }

    public MenuItem(String name, double price, String category, String imageUrl, Integer availableQuantity) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.availableQuantity = availableQuantity;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public boolean hasLimitedInventory() {
        return availableQuantity != null;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
