package com.example.SmartStay.product;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductRatingDTO {
    private String id;
    private String name;
    private String description;
    private List<String> images;
    private List<List<String>> features;
    private Address address;
    private String mapUrl;
    private double rating;
    private int ratingCount;

    // Constructor
    public ProductRatingDTO(Product product, double rating, int ratingCount) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.images = product.getImages();
        this.features = product.getFeatures();
        this.address = product.getAddress();
        this.mapUrl = product.getMapUrl();
        this.rating = rating;
        this.ratingCount = ratingCount;
    }
}