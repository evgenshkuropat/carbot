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
                          LocalDateTime createdAt) {
        this.id = id;
        this.source = source;
        this.title = title;
        this.price = price;
        this.priceValue = priceValue;
        this.location = location;
        this.url = url;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public Integer getPriceValue() {
        return priceValue;
    }

    public String getLocation() {
        return location;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setPriceValue(Integer priceValue) {
        this.priceValue = priceValue;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}