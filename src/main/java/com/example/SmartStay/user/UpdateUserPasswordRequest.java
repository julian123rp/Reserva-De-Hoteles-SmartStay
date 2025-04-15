package com.example.SmartStay.user;

import lombok.Getter;

@Getter
public class UpdateUserPasswordRequest {
    private String oldPassword;
    private String newPassword;
}
