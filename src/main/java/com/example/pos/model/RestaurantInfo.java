package com.example.pos.model;

public class RestaurantInfo {
    private String name;
    private String address;
    private String city;
    private String state;
    private String pinCode;
    private String contactNumber;
    private String email;
    private String website;
    private String gstin;
    private String fssaiLicense;
    private String logoPath;

    public RestaurantInfo() {
    }

    public RestaurantInfo(String name, String address, String city, String state, String pinCode,
                         String contactNumber, String email, String website, String gstin,
                         String fssaiLicense, String logoPath) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pinCode = pinCode;
        this.contactNumber = contactNumber;
        this.email = email;
        this.website = website;
        this.gstin = gstin;
        this.fssaiLicense = fssaiLicense;
        this.logoPath = logoPath;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getFssaiLicense() { return fssaiLicense; }
    public void setFssaiLicense(String fssaiLicense) { this.fssaiLicense = fssaiLicense; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
}
