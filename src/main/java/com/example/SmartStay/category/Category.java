package com.example.SmartStay.category;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collections;
import java.util.Set;

@Document(collection = "categories")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    private String image;

    @Indexed
    private Set<String> products;

    public Category(String name, String description, String image) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.products = Collections.emptySet();
    }
}