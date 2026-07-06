package com.yourapp.carbot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cars",
        indexes = {
                @Index(name = "idx_car_url", columnList = "url"),
                @Index(name = "idx_car_price", columnList = "priceValue"),
                @Index(name = "idx_car_created", columnList = "createdAt"),
                @Index(name = "idx_car_source", columnList = "source"),
                @Index(name = "idx_car_listing_status", columnList = "listingStatus"),
                @Index(name = "idx_car_owner_chat", columnList = "ownerChatId")
        }
)
public class CarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 100)
    private String price;

    private Integer priceValue;

    @Column(length = 255)
    private String location;

    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    @Column(length = 1500)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String brand;

    private Integer year;

    private Integer mileage;

    @Column(length = 50)
    private String fuelType;

    @Column(length = 50)
    private String transmission;

    @Column(length = 50)
    private String carType;

    private Long ownerChatId;

    @Column(length = 100)
    private String sellerUsername;

    @Column(length = 255)
    private String sellerContact;

    @Column(length = 50)
    private String listingStatus;

    @Column(length = 1000)
    private String description;

    public CarEntity() {
    }

    public CarEntity(String source,
                     String title,
                     String price,
                     Integer priceValue,
                     String location,
                     String url,
                     String imageUrl,
                     LocalDateTime createdAt,
                     String brand,
                     Integer year,
                     Integer mileage,
                     String fuelType,
                     String transmission,
                     String carType) {

        this.source = source;
        this.title = title;
        this.price = price;
        this.priceValue = priceValue;
        this.location = location;
        this.url = url;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.brand = brand;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.carType = carType;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (listingStatus == null || listingStatus.isBlank()) {
            listingStatus = "ACTIVE";
        }
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Integer getPriceValue() {
        return priceValue;
    }

    public void setPriceValue(Integer priceValue) {
        this.priceValue = priceValue;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public Long getOwnerChatId() {
        return ownerChatId;
    }

    public void setOwnerChatId(Long ownerChatId) {
        this.ownerChatId = ownerChatId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public String getSellerContact() {
        return sellerContact;
    }

    public void setSellerContact(String sellerContact) {
        this.sellerContact = sellerContact;
    }

    public String getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(String listingStatus) {
        this.listingStatus = listingStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
