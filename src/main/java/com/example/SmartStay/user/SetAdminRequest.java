package com.example.SmartStay.user;

import lombok.Getter;

public class SetAdminRequest {
    @Getter
    private String id;
    private boolean isAdmin;

    public boolean getIsAdmin() {
        return isAdmin;
    }
}
