package com.example.SmartStay.user;

import lombok.Getter;

@Getter
public class CreateUserRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}