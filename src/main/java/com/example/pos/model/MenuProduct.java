package com.example.pos.model;

/**
 * Model class for MenuProduct in POS system.
 * Represents an item in the menu with associated category and details.
 */
public class MenuProduct {

    private long id;               // Unique product ID
    private String name;           // Product name
    private double price;          // Product price
    private String category;       // Category name (optional but user-readable)
    private long categoryId;       // Category reference ID
    private int quantity;          // Stock or available quantity
    private String description;    // Optional product description (new feature)
    private String imageUrl;       // Optional image URL (for UI display)
    private double taxRate;        // Tax rate percentage (e.g., 5.0 for 5%)

    // ---------- Constructors ----------

    public MenuProduct() { }

    public MenuProduct(long id, String name, double price, String category,
                       long categoryId, int quantity, String description, String imageUrl, double taxRate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.description = description;
        this.imageUrl = imageUrl;
        this.taxRate = taxRate;
    }

    // ---------- Getters & Setters ----------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    // ---------- Utility ----------

    @Override
    public String toString() {
        return "MenuProduct{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", categoryId=" + categoryId +
                ", quantity=" + quantity +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", taxRate=" + taxRate +
                '}';
    }
}
