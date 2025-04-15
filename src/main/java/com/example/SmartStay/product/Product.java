package com.example.SmartStay.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndex(name = "address_country_city_idx", def = "{'address.country': 1, 'address.city': 1}")
public class Product {

    @Id
    private String id;

    private String name;

    private String description;

    private List<String> images;

    private List<List<String>> features;

    private Address address;

    private String mapUrl;

    private String mapEmbed;

    private List<Policy> policies;

    public Product(String name, String description, List<String> images, List<List<String>> features, Address address, String mapUrl, List<Policy> policies, String mapEmbed) {
        this.name = name;
        this.description = description;
        this.images = images;
        this.features = features;
        this.address = address;
        this.mapUrl = mapUrl;
        this.mapEmbed = mapEmbed;
        this.policies = policies;
    }
}