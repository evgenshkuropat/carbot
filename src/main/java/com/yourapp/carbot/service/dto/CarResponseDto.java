package com.yourapp.carbot.service.dto;

import java.time.LocalDateTime;

public class CarResponseDto {

    private Long id;
    private String source;
    private String title;
    private String price;
    private Integer priceValue;
    private String location;
    private String url;
    private String imageUrl;

    private String brand;
    private Integer year;
    private Integer mileage;
    private String fuelType;
    private String transmission;
    private String carType;

    private LocalDateTime createdAt;

    public CarResponseDto() {
    }

    public CarResponseDto(Long id,
                          String source,
                          String title,
                          String price,
                          Integer priceValue,
                          String location,
                          String url,
                          String imageUrl,
                          String brand,
                          Integer year,
                          Integer mileage,
                          String fuelType,
                          String transmission,
                          String carType,
                          LocalDateTime createdAt) {
        this.id = id;
        this.source = source;
        this.title = title;
        this.price = price;
        this.priceValue = priceValue;
        this.location = location;
        this.url = url;
        this.imageUrl = imageUrl;
        this.brand = brand;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.carType = carType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}