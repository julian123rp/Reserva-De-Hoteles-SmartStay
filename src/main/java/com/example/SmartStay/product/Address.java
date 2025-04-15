package com.example.SmartStay.product;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Address {

    private String country;
    private String city;

    public String toString() {
        return "Address(country=" + this.country + ",city=" + this.city + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(country, address.country) &&
                Objects.equals(city, address.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, city);
    }
}