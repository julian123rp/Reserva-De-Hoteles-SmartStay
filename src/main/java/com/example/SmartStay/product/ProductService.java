package com.example.SmartStay.product;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProductService {

    @Autowired
    ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(ObjectId id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(ObjectId id) {
        productRepository.deleteById(id);
    }

    public List<Product> getProductsByCountryAndCityQuery(String query, String query2, String query3) {
        return productRepository.findByMultipleQueriesToAddress(query, query2, query3);
    }

    public List<Product> getProductsByCountryAndCity(String country, String city) {
        return productRepository.findByCountryCity(country, city);
    }

    public HashSet<Address> getAllAddresses() {
        Collection<ProductSummary> products = productRepository.findAllAddresses();
        HashSet<Address> addresses = new HashSet<>();

        for (ProductSummary productSummary : products) {
            Address address = productSummary.getAddress();
            addresses.add(address);
        }

        return addresses;
    }
}