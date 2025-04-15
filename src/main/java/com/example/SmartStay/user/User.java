package com.example.SmartStay.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collections;
import java.util.List;

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private String id;

    private String email;

    private String firstName;

    private String lastName;

    private byte[] password;

    private boolean isAdmin;

    private boolean isConfirmed;

    private List<String> wishlist;

    public User(String email, byte[] password, boolean isAdmin, String firstName, String lastName, boolean isConfirmed) {
        this.email = email;
        this.password = password;
        this.isAdmin = isAdmin;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isConfirmed = isConfirmed;
        this.wishlist = Collections.emptyList();
    }
}