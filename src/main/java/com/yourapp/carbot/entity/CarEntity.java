package com.yourapp.carbot.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cars")
public class CarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, length = 500)
    private String title;

    private String price;

    private Integer priceValue;

    private String location;

    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    private String imageUrl;

    private LocalDateTime createdAt;

    private String brand;

    private Integer year;

    private Integer mileage;

    private String transmission;

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
                     String transmission) {
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
        this.transmission = transmission;
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

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }
}