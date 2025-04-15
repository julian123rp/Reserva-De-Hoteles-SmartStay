package com.example.SmartStay.user;

public interface UserProjection {

    String getId();

    String getFirstName();

    String getLastName();

    String getEmail();

    boolean getIsAdmin();

    boolean getIsConfirmed();
}